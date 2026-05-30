package org.peach.gateway.permission.cache;

import java.util.List;
import java.util.Map;
import org.peach.gateway.permission.model.ApiPermModel;

/**
 * 角色-API 权限本地缓存及 revision。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
public final class ApisCache {

	private static volatile Map<String, List<ApiPermModel>> roles = Map.of();

	private static volatile long revision;

	private ApisCache() {
	}

	public static long getRevision() {
		return revision;
	}

	public static void setRevision(long revision) {
		ApisCache.revision = revision;
	}

	public static Map<String, List<ApiPermModel>> getRoles() {
		return roles;
	}

	public static void setRoles(Map<String, List<ApiPermModel>> roles) {
		ApisCache.roles = roles == null ? Map.of() : roles;
	}
}
