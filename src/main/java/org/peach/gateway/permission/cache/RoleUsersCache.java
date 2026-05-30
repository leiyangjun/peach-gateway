package org.peach.gateway.permission.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色-用户本地缓存：正向 roleCode→userIds 与反向 userId→roleCodes 索引。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
public final class RoleUsersCache {

	/**
	 * Key为角色编码，List<Long>为用户合集
	 */
	private static volatile Map<String, List<Long>> roles = Map.of();

	/**
	 * key为用户，Set为角色编码
	 */
	private static volatile Map<Long, Set<String>> userRoleIndex = Map.of();

	private static volatile long revision;

	private RoleUsersCache() {}

	public static long getRevision() {
		return revision;
	}

	public static void setRevision(long revision) {
		RoleUsersCache.revision = revision;
	}

	public static Map<String, List<Long>> getRoles() {
		return roles;
	}

	public static void setRoles(Map<String, List<Long>> roles) {
		RoleUsersCache.roles = roles == null ? Map.of() : roles;
	}

	public static Set<String> getRoleCodesByUserId(long userId) {
		Set<String> codes = userRoleIndex.get(userId);
		return codes == null ? Set.of() : codes;
	}

	/**
	 * 写入正向映射并重建 userId→roleCodes 反向索引。
	 */
	public static void rebuildUserRoleIndex(Map<String, List<Long>> roleUsers) {
		if (roleUsers == null || roleUsers.isEmpty()) {
			userRoleIndex = Map.of();
			return;
		}
		Map<Long, Set<String>> index = new HashMap<>();
		for (Map.Entry<String, List<Long>> entry : roleUsers.entrySet()) {
			String roleCode = entry.getKey();
			if (roleCode == null) {
				continue;
			}
			List<Long> userIds = entry.getValue();
			if (userIds == null) {
				continue;
			}
			for (Long userId : userIds) {
				if (userId == null) {
					continue;
				}
				index.computeIfAbsent(userId, k -> new HashSet<>()).add(roleCode);
			}
		}
		Map<Long, Set<String>> frozen = new HashMap<>();
		for (Map.Entry<Long, Set<String>> entry : index.entrySet()) {
			frozen.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
		}
		userRoleIndex = Map.copyOf(frozen);
	}
}
