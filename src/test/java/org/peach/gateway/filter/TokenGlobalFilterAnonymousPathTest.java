package org.peach.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 校验 JWT 匿名路径在 {@code peach.api.context}（如 {@code /admin}）下仍能匹配。
 */
class TokenGlobalFilterAnonymousPathTest {

	private final TokenGlobalFilter filter = new TokenGlobalFilter();

	@Test
	void legacyServicePrefixedPathsStillAnonymous() {
		assertAnonymous("/peach-auth-service/auth/login");
		assertAnonymous("/peach-auth-service/auth/login/slider/challenge");
		assertAnonymous("/peach-auth-service/v3/api-docs");
	}

	@Test
	void adminContextServicePrefixedPathsAnonymous() {
		assertAnonymous("/peach-auth-service/admin/auth/login");
		assertAnonymous("/peach-auth-service/admin/auth/login/slider/x");
		assertAnonymous("/peach-auth-service/admin/v3/api-docs");
		assertAnonymous("/peach-auth-service/admin/swagger-ui/index.html");
	}

	@Test
	void adminContextRootPrefixedPathsAnonymous() {
		assertAnonymous("/admin/auth/login");
		assertAnonymous("/admin/auth/refresh");
		assertAnonymous("/admin/v3/api-docs");
	}

	@Test
	void adminContextRefreshPathAnonymous() {
		assertAnonymous("/peach-auth-service/admin/auth/refresh");
	}

	@Test
	void protectedBusinessPathStillRequiresJwt() {
		assertNotAnonymous("/peach-auth-service/admin/users");
		assertNotAnonymous("/peach-auth-service/users");
	}

	private void assertAnonymous(String path) {
		assertThat(invokeIsAnonymousPath(path)).as("path=%s", path).isTrue();
	}

	private void assertNotAnonymous(String path) {
		assertThat(invokeIsAnonymousPath(path)).as("path=%s", path).isFalse();
	}

	private boolean invokeIsAnonymousPath(String path) {
		return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(filter, "isAnonymousPath", path));
	}
}
