package org.peach.gateway.permission.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.peach.gateway.permission.cache.ApisCache;
import org.peach.gateway.permission.cache.RoleUsersCache;
import org.peach.gateway.permission.model.ApiPermModel;
import org.springframework.http.HttpMethod;

/**
 * {@link PermEvaluator} 单测。
 */
class RolePermEvaluatorTest {

	private final PermEvaluator evaluator = new PermEvaluator();

	@BeforeEach
	void setUp() {
		RoleUsersCache.setRevision(1L);
		RoleUsersCache.setRoles(Map.of("ROLE_OPS", List.of(100L)));
		RoleUsersCache.rebuildUserRoleIndex(Map.of("ROLE_OPS", List.of(100L)));

		ApisCache.setRevision(1L);
		ApisCache.setRoles(Map.of("ROLE_OPS",
				List.of(new ApiPermModel("POST", "/peach-common-service/admin/user/list"))));
	}

	@AfterEach
	void tearDown() {
		RoleUsersCache.setRevision(0L);
		RoleUsersCache.setRoles(Map.of());
		RoleUsersCache.rebuildUserRoleIndex(Map.of());
		ApisCache.setRevision(0L);
		ApisCache.setRoles(Map.of());
	}

	@Test
	void isPermitted_trueWhenRoleHasMatchingApi() {
		assertTrue(evaluator.isPermitted(100L, HttpMethod.POST, "/peach-common-service/admin/user/list"));
	}

	@Test
	void isPermitted_methodCaseInsensitive() {
		ApisCache.setRoles(Map.of("ROLE_OPS",
				List.of(new ApiPermModel("post", "/peach-common-service/admin/user/list"))));
		assertTrue(evaluator.isPermitted(100L, HttpMethod.POST, "/peach-common-service/admin/user/list"));
	}

	@Test
	void isPermitted_antPatternWildcard() {
		ApisCache.setRoles(Map.of("ROLE_OPS",
				List.of(new ApiPermModel("GET", "/*/admin/v3/api-docs/**"))));
		assertTrue(evaluator.isPermitted(100L, HttpMethod.GET, "/peach-common-service/admin/v3/api-docs/default"));
	}

	@Test
	void isPermitted_falseWhenPathNotMatched() {
		assertFalse(evaluator.isPermitted(100L, HttpMethod.GET, "/peach-common-service/admin/user/list"));
	}

	@Test
	void isPermitted_falseWhenUserHasNoRole() {
		assertFalse(evaluator.isPermitted(999L, HttpMethod.POST, "/peach-common-service/admin/user/list"));
	}

	@Test
	void isPermitted_falseWhenPathEmpty() {
		assertFalse(evaluator.isPermitted(100L, HttpMethod.POST, ""));
	}

	@Test
	void isPermitted_unionAcrossMultipleRoles() {
		RoleUsersCache.setRoles(Map.of("ROLE_A", List.of(100L), "ROLE_B", List.of(100L)));
		RoleUsersCache.rebuildUserRoleIndex(Map.of("ROLE_A", List.of(100L), "ROLE_B", List.of(100L)));
		ApisCache.setRoles(Map.of(
				"ROLE_A", List.of(new ApiPermModel("GET", "/a")),
				"ROLE_B", List.of(new ApiPermModel("POST", "/peach-common-service/admin/user/list"))));

		assertTrue(evaluator.isPermitted(100L, HttpMethod.POST, "/peach-common-service/admin/user/list"));
	}
}
