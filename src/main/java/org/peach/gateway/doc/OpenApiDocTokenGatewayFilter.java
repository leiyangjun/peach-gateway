package org.peach.gateway.doc;

import org.peach.gateway.config.PeachDocTokenProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 将 Token 传播到下游 OpenAPI 文档请求（如 /v3/api-docs），便于受保护的微服务暴露聚合文档。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun — 后续可结合 Vue 管理端登录态、网关统一鉴权等扩展取值逻辑。
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "peach.gateway.doc.token-propagation", name = "enabled", havingValue = "true")
public class OpenApiDocTokenGatewayFilter implements GlobalFilter, Ordered {

	private final PeachDocTokenProperties props;

	public OpenApiDocTokenGatewayFilter(PeachDocTokenProperties props) {
		this.props = props;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (!props.isEnabled() || !StringUtils.hasText(props.getStaticValue())) {
			return chain.filter(exchange);
		}
		String path = exchange.getRequest().getURI().getPath();
		if (!path.contains("/v3/api-docs") && !path.contains("/v2/api-docs")) {
			return chain.filter(exchange);
		}
		ServerHttpRequest request = exchange.getRequest().mutate().header(props.getHeaderName(), props.getStaticValue()).build();
		return chain.filter(exchange.mutate().request(request).build());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 20;
	}
}
