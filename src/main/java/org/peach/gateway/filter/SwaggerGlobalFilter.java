package org.peach.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 在请求进入路由前补齐转发相关请求头：当上游未带 {@code X-Forwarded-*} / {@code Forwarded} 时，根据当前请求的
 * Host、scheme、解析出的客户端端口写入，便于下游在 {@code forward-headers-strategy=framework} 下还原对外 URL。
 * <p>
 * 若请求已携带同名头则<strong>不覆盖</strong>，以保留最外层反向代理写入的值。
 * </p>
 *
 * @author leiyangjun
 */
@Component
public class SwaggerGlobalFilter implements GlobalFilter, Ordered {

	/** RFC 7239 */
	private static final String HEADER_FORWARDED = "Forwarded";

	private static final String HEADER_X_FORWARDED_HOST = "X-Forwarded-Host";

	private static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";

	private static final String HEADER_X_FORWARDED_PORT = "X-Forwarded-Port";

	private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

	/** 按序设置转发头后进入过滤器链。 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		HttpHeaders headers = request.getHeaders();
		ServerHttpRequest.Builder mutated = request.mutate();

		String host = headers.getFirst(HttpHeaders.HOST);
		if (StringUtils.hasText(host) && !StringUtils.hasText(headers.getFirst(HEADER_X_FORWARDED_HOST))) {
			mutated.header(HEADER_X_FORWARDED_HOST, host.trim());
		}

		String scheme = request.getURI().getScheme();
		if (!StringUtils.hasText(scheme)) {
			scheme = "http";
		}
		if (!StringUtils.hasText(headers.getFirst(HEADER_X_FORWARDED_PROTO))) {
			mutated.header(HEADER_X_FORWARDED_PROTO, scheme);
		}

		int port = resolveIncomingPort(host, scheme);
		if (port > 0 && !StringUtils.hasText(headers.getFirst(HEADER_X_FORWARDED_PORT))) {
			mutated.header(HEADER_X_FORWARDED_PORT, Integer.toString(port));
		}

		// Spring Boot 在 framework 策略下会解析 RFC 7239 Forwarded，比仅依赖 Host 更完整
		if (StringUtils.hasText(host) && !StringUtils.hasText(headers.getFirst(HEADER_FORWARDED))) {
			mutated.header(HEADER_FORWARDED, buildRfc7239Forwarded(scheme, host.trim()));
		}

		return chain.filter(exchange.mutate().request(mutated.build()).build());
	}

	/** RFC 7239 {@code Forwarded}，供下游 ForwardedHeaderFilter 还原 scheme/host。 */
	private static String buildRfc7239Forwarded(String scheme, String hostHeader) {
		String hostParam = hostHeader.contains(":") ? '"' + hostHeader + '"' : hostHeader;
		return "proto=" + scheme + ";host=" + hostParam;
	}

	/**
	 * 从发往网关的 {@code Host} 解析端口；未写端口时按 scheme 使用默认 80/443。
	 */
	private static int resolveIncomingPort(String hostHeader, String scheme) {
		if (!StringUtils.hasText(hostHeader)) {
			return -1;
		}
		int comma = hostHeader.indexOf(',');
		String host = (comma < 0 ? hostHeader : hostHeader.substring(0, comma)).trim();
		if (host.startsWith("[")) {
			int endBracket = host.indexOf(']');
			if (endBracket > 0 && endBracket + 1 < host.length() && host.charAt(endBracket + 1) == ':') {
				try {
					return Integer.parseInt(host.substring(endBracket + 2));
				}
				catch (NumberFormatException ex) {
					return -1;
				}
			}
			return "https".equalsIgnoreCase(scheme) ? 443 : 80;
		}
		int colon = host.lastIndexOf(':');
		if (colon > 0 && colon < host.length() - 1) {
			String tail = host.substring(colon + 1);
			if (tail.chars().allMatch(Character::isDigit)) {
				try {
					return Integer.parseInt(tail);
				}
				catch (NumberFormatException ex) {
					return -1;
				}
			}
		}
		return "https".equalsIgnoreCase(scheme) ? 443 : 80;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}
}
