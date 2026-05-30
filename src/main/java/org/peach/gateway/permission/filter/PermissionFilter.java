package org.peach.gateway.permission.filter;

import java.nio.charset.StandardCharsets;
import org.peach.gateway.common.GatewayExchangeAttributes;
import org.peach.gateway.common.result.message.Message400;
import org.peach.gateway.common.result.web.ErrorResult;
import org.peach.gateway.common.util.JSONUtil;
import org.peach.gateway.permission.matcher.PermEvaluator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * API 权限过滤器：JWT 校验通过后，按 ROLE_USERS + ROLE_APIS 快照判定 403。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class PermissionFilter implements GlobalFilter {

	private static final String QUERY_USER_ID = "peach_user_id";

	private static final String QUERY_USERNAME = "peach_username";

	/** 超级管理员用户名，与 RoleServiceImpl.ADMIN_USER 一致，跳过权限校验 */
	private static final String SUPER_ADMIN_USERNAME = "admin";

	private final PermEvaluator rolePermEvaluator;

	public PermissionFilter(PermEvaluator rolePermEvaluator) {
		this.rolePermEvaluator = rolePermEvaluator;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		HttpMethod method = request.getMethod();
		String path = request.getURI().getPath();
		if (method == HttpMethod.OPTIONS) {
			//log.debug("[权限] 放行 OPTIONS {}", path);
			return chain.filter(exchange);
		}
		// 白名单 accessType=1：免登录，一并跳过权限
		Boolean skipAuth = exchange.getAttribute(GatewayExchangeAttributes.SKIP_AUTH);
		if (skipAuth != null && skipAuth) {
			return chain.filter(exchange);
		}
		// 白名单 accessType=2：已登录，仅跳过权限校验
		Boolean skipPermission = exchange.getAttribute(GatewayExchangeAttributes.SKIP_PERMISSION);
		if (skipPermission != null && skipPermission) {
			return chain.filter(exchange);
		}
		// 超级管理员具备所有权限
		String username = request.getQueryParams().getFirst(QUERY_USERNAME);
		if (SUPER_ADMIN_USERNAME.equals(username)) {
			return chain.filter(exchange);
		}
		Long userId = parseUserId(request.getQueryParams().getFirst(QUERY_USER_ID));
		if (userId == null) {
			// LOG.warn("[权限] 拒绝 403 {} {} reason=缺少或非法 peach_user_id", method, path);
			return forbidden(exchange);
		}
		if (rolePermEvaluator.isPermitted(userId, method, path)) {
			return chain.filter(exchange);
		} else {
			return forbidden(exchange);
		}
	}

	/**
	 * 
	 * @Title: parseUserId
	 * @Description: 获取当前用户登陆ID
	 * @param: @param raw
	 * @param: @return
	 * @return: Long 当前用户ID
	 * @throws
	 */
	private static Long parseUserId(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * 
	 * @Title: forbidden
	 * @Description: 返回403错误
	 * @param: @param exchange
	 * @param: @return
	 * @return: Mono<Void>
	 * @throws
	 */
	private Mono<Void> forbidden(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.FORBIDDEN);
		response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
		ErrorResult err = ErrorResult.forbidden(Message400.GATEWAY_FORBIDDEN);
		byte[] body = JSONUtil.toJsonBytes(err);
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
	}
}
