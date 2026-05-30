package org.peach.gateway.route.discovery;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 基于 {@link DiscoveryClient#getServices()} 为每个服务 id 生成路由：
 * <ul>
 * <li>{@code /peach-gateway/{serviceId}/**}（推荐统一入口）</li>
 * <li>{@code /{serviceId}/**}（兼容旧路径）</li>
 * </ul>
 * 转发至 {@code lb://{serviceId}}，{@code RewritePath} 去掉 URL 中的前缀段。
 * <p>
 * 不向本进程注册 {@code spring.application.name} 对应的发现路由：网关已在 Nacos 注册为 {@code peach-gateway} 时，若再生成
 * {@code lb://peach-gateway} 会与 {@link org.peach.gateway.route.PeachGatewayShellRouteDefinitionLocator} 的
 * {@code /peach-gateway/** → forward:/} 冲突，并产生 {@code /peach-gateway/peach-gateway/**} 等错误回环路径。
 * </p>
 */
@Slf4j
public class DynamicDiscoveryRouteDefinitionLocator implements RouteDefinitionLocator {

	private final DiscoveryClient discoveryClient;

	/** 与 {@code spring.application.name} 对齐、经规范化后的本地服务名，用于排除自路由 */
	private final String gatewayName;

	/**
	 * 
	 * @Title: DynamicDiscoveryRouteDefinitionLocator
	 * @Description:
	 * @param: @param discoveryClient
	 * @param: @param applicationName 网关的服务名称
	 * @throws
	 */
	public DynamicDiscoveryRouteDefinitionLocator(DiscoveryClient discoveryClient, String gatewayName) {
		this.discoveryClient = discoveryClient;
		this.gatewayName = gatewayName;
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		log.info("Refreshing dynamic routes from DiscoveryClient (blocking) ...");
		// 使用 Mono.fromCallable 将阻塞调用隔离到弹性线程池，避免阻塞 Netty 事件循环
		return Mono.fromCallable(() -> {
			List<String> services = discoveryClient.getServices();
			log.debug("Discovered services: {}", services);
			List<RouteDefinition> definitions = new ArrayList<>();
			for (String serviceId : services) {
				if (serviceId.equalsIgnoreCase(gatewayName)) {
					definitions.add(createGatewayRoute(serviceId));
				} else {
					definitions.add(createServiceRoute(serviceId));
				}
			}
			return definitions;
		}).subscribeOn(Schedulers.boundedElastic()) // 将阻塞操作调度到弹性线程池
			.flatMapMany(Flux::fromIterable) // 将 List<RouteDefinition> 转为 Flux<RouteDefinition>
			.doOnComplete(() -> log.info("Dynamic routes refresh completed"));
	}

	/**
	 * 根据 ServiceInstance 创建 RouteDefinition。 规则： - id = serviceId - uri = lb://serviceId - predicates =
	 * Path=/serviceId/** - filters = StripPrefix=1 (去掉第一级路径)
	 */
	private RouteDefinition createServiceRoute(String serviceId) {
		RouteDefinition definition = new RouteDefinition();
		definition.setId(serviceId);
		definition.setUri(URI.create("lb://" + serviceId));
		definition.setOrder(0);

		// 1. 断言：Path 匹配
		PredicateDefinition predicateDef = new PredicateDefinition();
		predicateDef.setName("Path");
		Map<String, String> predicateArgs = new HashMap<>();
		predicateArgs.put("pattern", "/" + serviceId + "/**");
		predicateDef.setArgs(predicateArgs);
		definition.setPredicates(Collections.singletonList(predicateDef));

		// 2. 过滤器列表
		List<FilterDefinition> filters = new ArrayList<>();

		// 2.1 添加 X-Forwarded-Prefix 头（关键！）
		FilterDefinition prefixFilter = new FilterDefinition();
		prefixFilter.setName("AddRequestHeader");
		Map<String, String> prefixArgs = new HashMap<>();
		prefixArgs.put("name", "X-Forwarded-Prefix");
		prefixArgs.put("value", "/" + serviceId);
		prefixFilter.setArgs(prefixArgs);
		filters.add(prefixFilter);

		// 2.2 剥离路径前缀（StripPrefix=1）
		FilterDefinition stripFilter = new FilterDefinition();
		stripFilter.setName("StripPrefix");
		Map<String, String> stripArgs = new HashMap<>();
		stripArgs.put("parts", "1");
		stripFilter.setArgs(stripArgs);
		filters.add(stripFilter);

		definition.setFilters(filters);
		// definition.setMetadata(Map.of("discovered", "true", "serviceId", serviceId));
		return definition;
	}

	public RouteDefinition createGatewayRoute(String gatewayName) {
		String prefix = "/peach-gateway";

		RouteDefinition def = new RouteDefinition();
		def.setId("peach-gateway-shell");
		def.setUri(URI.create("forward:/"));
		def.setOrder(10_000);

		PredicateDefinition pathPredicate = new PredicateDefinition();
		pathPredicate.setName(NameUtils.normalizeRoutePredicateName(PathRoutePredicateFactory.class));
		pathPredicate.addArg("pattern", prefix + "/**");
		def.getPredicates().add(pathPredicate);

		return def;
	}
}
