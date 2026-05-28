package org.peach.gateway.jwt.anonymous;

/**
 * 免鉴权 Redis 业务 Key（与 common-service {@code UnauthApiRedisConstants} 保持一致）。
 */
public final class UnauthApiRedisBusinessKeys {

	public static final String BIZ_SNAPSHOT = "UNAUTHAPI";

	public static final String BIZ_REVISION = "UNAUTHAPIREV";

	private UnauthApiRedisBusinessKeys() {
	}
}
