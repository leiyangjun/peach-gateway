package org.peach.gateway.filter;

import java.net.URI;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

/**
 * 在转发下游前将当前用户写入查询参数（与业务 Starter 中 {@code CurrentLoginUserUtil} 的 {@code peach_*} 名称一致）。
 * {@code subject_type} 与访问令牌 Claim 一致，取值域与业务侧 {@code UserType}（INTERNAL / CUSTOMER）对齐。
 */
@Component
public class PeachGatewayForwardedUserGatewayFilter implements GlobalFilter, Ordered {

	private static final String CLAIM_SUBJECT_TYPE = "subject_type";

	/** 与 {@code peach-common-start} {@code CurrentLoginUserUtil#QUERY_USER_ID} 保持一致 */
	public static final String QUERY_USER_ID = "peach_user_id";

	/** 与 {@code CurrentLoginUserUtil#QUERY_USERNAME} 保持一致 */
	public static final String QUERY_USERNAME = "peach_username";

	/** 与 {@code CurrentLoginUserUtil#QUERY_SUBJECT_TYPE} 保持一致 */
	public static final String QUERY_SUBJECT_TYPE = "peach_subject_type";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return ReactiveSecurityContextHolder.getContext().flatMap(ctx -> {
			if (ctx.getAuthentication() == null || !ctx.getAuthentication().isAuthenticated()) {
				return chain.filter(exchange);
			}
			Object p = ctx.getAuthentication().getPrincipal();
			if (!(p instanceof Jwt jwt)) {
				return chain.filter(exchange);
			}
			String sub = jwt.getSubject();
			String username = jwt.getClaimAsString("preferred_username");
			if (username == null) {
				username = jwt.getClaimAsString("user_name");
			}
			String subjectType = jwt.getClaimAsString(CLAIM_SUBJECT_TYPE);

			ServerHttpRequest request = exchange.getRequest();
			URI uri = request.getURI();
			UriComponentsBuilder b = UriComponentsBuilder.fromUri(uri);
			if (sub != null) {
				b.replaceQueryParam(QUERY_USER_ID, sub);
			}
			if (username != null) {
				b.replaceQueryParam(QUERY_USERNAME, username);
			}
			if (subjectType != null) {
				b.replaceQueryParam(QUERY_SUBJECT_TYPE, subjectType);
			}
			URI newUri = b.build(true).toUri();
			ServerHttpRequest downstream = request.mutate().uri(newUri).build();
			return chain.filter(exchange.mutate().request(downstream).build());
		}).switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}
}
