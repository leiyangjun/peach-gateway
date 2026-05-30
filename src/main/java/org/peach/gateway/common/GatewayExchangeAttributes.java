package org.peach.gateway.common;

/**
 * 网关 {@link org.springframework.web.server.ServerWebExchange} 属性键。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
public final class GatewayExchangeAttributes {

	/** 白名单 accessType=1：跳过 JWT 校验 */
	public static final String SKIP_AUTH = "skipAuth";

	/** 白名单 accessType=2：需登录但跳过 API 权限校验 */
	public static final String SKIP_PERMISSION = "skipPermission";

	private GatewayExchangeAttributes() {
	}
}
