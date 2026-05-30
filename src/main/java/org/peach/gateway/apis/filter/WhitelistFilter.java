package org.peach.gateway.apis.filter;

import org.peach.gateway.apis.cache.UnauthApiCache;
import org.peach.gateway.apis.model.ApiModel;
import org.peach.gateway.common.GatewayExchangeAttributes;
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
 * 白名单过滤器：按 {@link UnauthApiCache} 中的 method + finalPath（Ant 模式）匹配，
 * 按 {@code accessType} 设置 {@link GatewayExchangeAttributes#SKIP_AUTH} 或 {@link GatewayExchangeAttributes#SKIP_PERMISSION}。
 *
 * @author leiyangjun
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WhitelistFilter implements GlobalFilter {

	/** 免登录白名单 */
	private static final short ACCESS_TYPE_SKIP_AUTH = 1;

	/** 需登录但跳过权限校验 */
	private static final short ACCESS_TYPE_SKIP_PERMISSION = 2;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		HttpMethod method = exchange.getRequest().getMethod();
		String path = exchange.getRequest().getURI().getPath();
		applyMatchedAccessType(exchange, method, path, UnauthApiCache.getApis());
		return chain.filter(exchange);
	}

	/**
	 * 多条规则同时命中时：任一 accessType=1 则免登录；否则任一 accessType=2 则仅跳过权限。
	 */
	private void applyMatchedAccessType(ServerWebExchange exchange, HttpMethod httpMethod, String path,
			java.util.List<ApiModel> rules) {
		if (httpMethod == null || !StringUtils.hasText(path) || rules == null || rules.isEmpty()) {
			return;
		}
		boolean anySkipPermission = false;
		for (ApiModel item : rules) {
			if (!matchesRule(httpMethod, path, item)) {
				continue;
			}
			Short accessType = item.getAccessType();
			if (isSkipAuth(accessType)) {
				exchange.getAttributes().put(GatewayExchangeAttributes.SKIP_AUTH, true);
				return;
			}
			if (isSkipPermission(accessType)) {
				anySkipPermission = true;
			}
		}
		if (anySkipPermission) {
			exchange.getAttributes().put(GatewayExchangeAttributes.SKIP_PERMISSION, true);
		}
	}

	private boolean matchesRule(HttpMethod httpMethod, String path, ApiModel item) {
		if (item == null || !StringUtils.hasText(item.getFinalPath())) {
			return false;
		}
		String ruleMethod = item.getMethod();
		if (!StringUtils.hasText(ruleMethod)) {
			return false;
		}
		return ruleMethod.trim().equalsIgnoreCase(httpMethod.name())
				&& pathMatcher.match(item.getFinalPath().trim(), path);
	}

	private static boolean isSkipAuth(Short accessType) {
		return accessType != null && accessType.shortValue() == ACCESS_TYPE_SKIP_AUTH;
	}

	private static boolean isSkipPermission(Short accessType) {
		return accessType != null && accessType.shortValue() == ACCESS_TYPE_SKIP_PERMISSION;
	}
}
