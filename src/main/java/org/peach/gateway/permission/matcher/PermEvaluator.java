package org.peach.gateway.permission.matcher;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.peach.gateway.permission.cache.ApisCache;
import org.peach.gateway.permission.cache.RoleUsersCache;
import org.peach.gateway.permission.model.ApiPermModel;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * 用户 ID + 双缓存 → 是否拥有当前 method+path 权限。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
@Component
public class PermEvaluator {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 判断用户是否拥有访问指定 method+path 的权限（多角色 API 并集）。
	 */
	public boolean isPermitted(long userId, HttpMethod method, String path) {
		if (method == null || !StringUtils.hasText(path)) {
			return false;
		}
		Set<String> roleCodes = RoleUsersCache.getRoleCodesByUserId(userId);
		if (roleCodes.isEmpty()) {
			return false;
		}
		Map<String, List<ApiPermModel>> roleApis = ApisCache.getRoles();
		if (roleApis.isEmpty()) {
			return false;
		}
		String requestMethod = method.name();
		for (String roleCode : roleCodes) {
			List<ApiPermModel> apis = roleApis.get(roleCode);
			if (apis == null || apis.isEmpty()) {
				continue;
			}
			for (ApiPermModel api : apis) {
				if (matches(requestMethod, path, api)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean matches(String requestMethod, String path, ApiPermModel api) {
		if (api == null || !StringUtils.hasText(api.getFinalPath())) {
			return false;
		}
		String ruleMethod = api.getMethod();
		if (!StringUtils.hasText(ruleMethod)) {
			return false;
		}
		return ruleMethod.trim().equalsIgnoreCase(requestMethod)
				&& pathMatcher.match(api.getFinalPath().trim(), path);
	}
}
