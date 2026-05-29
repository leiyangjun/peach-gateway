package org.peach.gateway.apis.filter;

import java.util.List;
import org.peach.gateway.apis.cache.UnauthApiCache;
import org.peach.gateway.apis.model.ApiModel;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 白名单过滤器：按 {@link UnauthApiCache} 中的 method + finalPath（Ant 模式）匹配，命中则标记 skipAuth。
 *
 * @author leiyangjun
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WhitelistFilter implements GlobalFilter {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (matchesWhitelist(exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath())) {
			exchange.getAttributes().put("skipAuth", true);
		}
		return chain.filter(exchange);
	}

	private boolean matchesWhitelist(HttpMethod httpMethod, String path) {
		if (!StringUtils.hasText(path)) {
			return false;
		}
		List<ApiModel> apis = UnauthApiCache.getApis();
		if (apis.isEmpty()) {
			return false;
		}
		for (ApiModel item : apis) {
			if (item == null || !StringUtils.hasText(item.getFinalPath())) {
				continue;
			}
			String ruleMethod = item.getMethod() == null ? "" : item.getMethod().trim();
			if (!StringUtils.hasText(ruleMethod) || "ALL".equalsIgnoreCase(ruleMethod)) {
				continue;
			}
			if (methodMatches(ruleMethod, httpMethod)
					&& pathMatcher.match(item.getFinalPath().trim(), path)) {
				return true;
			}
		}
		return false;
	}

	private static boolean methodMatches(String ruleMethod, HttpMethod requestMethod) {
		if (requestMethod == null || !StringUtils.hasText(ruleMethod)) {
			return false;
		}
		return ruleMethod.equalsIgnoreCase(requestMethod.name());
	}

}
