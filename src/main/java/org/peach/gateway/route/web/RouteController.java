package org.peach.gateway.route.web;

import java.util.List;
import java.util.stream.Collectors;
import org.peach.gateway.common.result.web.ApiResult;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

/**
 * 排查用：返回当前网关中已合并的 {@link Route} 列表，以及 {@link DiscoveryClient} 中的服务名。
 * <p>
 * 与业务微服务解耦，不放在 peach-common-service。
 * <p>
 * 路径固定为 {@code GET /routes}（与 {@code application.yml}、网关 JWT 匿名白名单一致）。
 * </p>
 *
 * @author leiyangjun
 */
@RestController
@RequestMapping("/routes")
@Tag(name = "路由诊断", description = "当前网关已加载路由与注册中心服务名")
public class RouteController {

	private final RouteLocator routeLocator;

	private final DiscoveryClient discoveryClient;

	public RouteController(RouteLocator routeLocator, DiscoveryClient discoveryClient) {
		this.routeLocator = routeLocator;
		this.discoveryClient = discoveryClient;
	}

	/** 收集 {@link RouteLocator} 路由快照与注册中心服务 id，封装为 {@link ApiResult}。 */
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "当前网关路由快照与注册中心服务名")
	public Mono<ResponseEntity<ApiResult<GatewayRoutesSnapshot>>> routes() {
		return routeLocator.getRoutes().collectList().map(list -> {
			List<RouteSummary> summaries = list.stream().map(this::toSummary).collect(Collectors.toList());
			List<String> names = discoveryClient.getServices();
			GatewayRoutesSnapshot body = new GatewayRoutesSnapshot(System.currentTimeMillis(), names, summaries);
			return ResponseEntity.ok(ApiResult.ok(body));
		});
	}

	private RouteSummary toSummary(Route r) {
		String pred = r.getPredicate() != null ? r.getPredicate().toString() : "";
		List<String> filterDesc = r.getFilters().stream().map(Object::toString).collect(Collectors.toList());
		String uri = r.getUri() != null ? r.getUri().toString() : null;
		return new RouteSummary(r.getId(), uri, r.getOrder(), pred, filterDesc);
	}

	/** 路由诊断响应体（JSON）。 */
	public record GatewayRoutesSnapshot(long generatedAtEpochMs, List<String> discoveryServiceNames,
		List<RouteSummary> routes) {
	}

	/** 单条路由摘要。 */
	public record RouteSummary(String id, String uri, int order, String predicate, List<String> filters) {
	}
}
