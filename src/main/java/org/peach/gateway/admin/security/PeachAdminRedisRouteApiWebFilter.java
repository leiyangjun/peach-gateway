package org.peach.gateway.admin.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.peach.gateway.config.PeachGatewayAdminProperties;
import org.peach.gateway.route.store.PeachRedisRouteStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 仅校验「Redis 自定义路由 CRUD」路径的 API Key，不参与网关对下游业务的转发链路。
 * <p>
 * 设计要点：通过路径前缀 {@link PeachGatewayAdminProperties#getRedisRoutesPath()} 限定作用域；
 * 非该前缀的请求直接放行，便于今后为其他服务 API 单独编写 {@code GlobalFilter} / Spring Security 规则。
 * </p>
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@ConditionalOnBean(PeachRedisRouteStore.class)
@ConditionalOnProperty(prefix = "peach.gateway.admin.security", name = "api-key-enabled", havingValue = "true")
public class PeachAdminRedisRouteApiWebFilter implements WebFilter {

	private final PeachGatewayAdminProperties adminProps;

	public PeachAdminRedisRouteApiWebFilter(PeachGatewayAdminProperties adminProps) {
		this.adminProps = adminProps;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (request.getMethod() == HttpMethod.OPTIONS) {
			return chain.filter(exchange);
		}
		String path = request.getPath().pathWithinApplication().value();
		String prefix = adminProps.getRedisRoutesPath();
		if (!StringUtils.hasText(prefix) || !pathStartsWithPrefix(path, prefix)) {
			return chain.filter(exchange);
		}
		String expected = adminProps.getSecurity().getApiKeyValue();
		if (!StringUtils.hasText(expected)) {
			return jsonError(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE,
					"已开启管理端 API Key 校验但未配置 peach.gateway.admin.security.api-key-value");
		}
		String headerName = adminProps.getSecurity().getApiKeyHeader();
		String presented = request.getHeaders().getFirst(headerName);
		if (!StringUtils.hasText(presented) || !constantTimeEquals(presented, expected)) {
			return jsonError(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "管理端 API Key 无效或缺失");
		}
		return chain.filter(exchange);
	}

	private static boolean pathStartsWithPrefix(String path, String prefix) {
		String p = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
		if (!path.startsWith(p)) {
			return false;
		}
		return path.length() == p.length() || path.charAt(p.length()) == '/';
	}

	private static boolean constantTimeEquals(String a, String b) {
		byte[] aa = a.getBytes(StandardCharsets.UTF_8);
		byte[] bb = b.getBytes(StandardCharsets.UTF_8);
		return aa.length == bb.length && MessageDigest.isEqual(aa, bb);
	}

	private static Mono<Void> jsonError(ServerHttpResponse response, HttpStatus status, String message) {
		response.setStatusCode(status);
		response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		String body = "{\"message\":\"" + escapeJson(message) + "\",\"status\":" + status.value() + "}";
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}

	private static String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
