package org.peach.gateway.permission.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.peach.gateway.redis.GatewayRedisAccessor;
import org.peach.gateway.permission.cache.RoleUsersCache;

/**
 * {@link RoleUsersLoader} 单测。
 */
@ExtendWith(MockitoExtension.class)
class RoleUsersLoaderTest {

	@Mock
	private GatewayRedisAccessor redisAccessor;

	private RoleUsersLoader loader;

	@BeforeEach
	void setUp() {
		loader = new RoleUsersLoader(redisAccessor, new CommRedisKeyBuilder("dev"));
		RoleUsersCache.setRevision(0L);
		RoleUsersCache.setRoles(Map.of());
		RoleUsersCache.rebuildUserRoleIndex(Map.of());
	}

	@AfterEach
	void tearDown() {
		RoleUsersCache.setRevision(0L);
		RoleUsersCache.setRoles(Map.of());
		RoleUsersCache.rebuildUserRoleIndex(Map.of());
	}

	@Test
	void refreshFromRedis_buildsReverseIndex() {
		String json = """
				{
				  "revision": 3,
				  "updatedAt": "2026-05-29T12:00:00",
				  "roles": {
				    "ROLE_ADMIN": [100000000001],
				    "ROLE_OPS": [100000000001, 200]
				  }
				}
				""";
		when(redisAccessor.get("COMM-DEV-ROLE_USERS")).thenReturn(json);

		loader.refreshFromRedis();

		assertEquals(3L, RoleUsersCache.getRevision());
		Set<String> roles = RoleUsersCache.getRoleCodesByUserId(100000000001L);
		assertEquals(2, roles.size());
		assertTrue(roles.contains("ROLE_ADMIN"));
		assertTrue(roles.contains("ROLE_OPS"));
		assertEquals(Set.of("ROLE_OPS"), RoleUsersCache.getRoleCodesByUserId(200L));
	}
}
