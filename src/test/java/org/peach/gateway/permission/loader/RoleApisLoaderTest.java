package org.peach.gateway.permission.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.peach.gateway.redis.GatewayRedisAccessor;
import org.peach.gateway.permission.cache.ApisCache;
import org.peach.gateway.permission.model.ApiPermModel;

/**
 * {@link RoleApisLoader} 单测。
 */
@ExtendWith(MockitoExtension.class)
class RoleApisLoaderTest {

	@Mock
	private GatewayRedisAccessor redisAccessor;

	private RoleApisLoader loader;

	@BeforeEach
	void setUp() {
		loader = new RoleApisLoader(redisAccessor, new CommRedisKeyBuilder("dev"));
		ApisCache.setRevision(0L);
		ApisCache.setRoles(Map.of());
	}

	@AfterEach
	void tearDown() {
		ApisCache.setRevision(0L);
		ApisCache.setRoles(Map.of());
	}

	@Test
	void refreshFromRedis_parsesSnapshotAndUpdatesCache() {
		String json = """
				{
				  "revision": 4,
				  "updatedAt": "2026-05-29T12:00:00",
				  "roles": {
				    "ROLE_OPS": [
				      { "method": "POST", "finalPath": "/peach-common-service/admin/user/list" }
				    ]
				  }
				}
				""";
		when(redisAccessor.get("COMM-DEV-ROLE_APIS")).thenReturn(json);

		loader.refreshFromRedis();

		assertEquals(4L, ApisCache.getRevision());
		assertEquals(1, ApisCache.getRoles().size());
		ApiPermModel item = ApisCache.getRoles().get("ROLE_OPS").get(0);
		assertEquals("POST", item.getMethod());
		assertEquals("/peach-common-service/admin/user/list", item.getFinalPath());
	}

	@Test
	void refreshFromRedis_emptyKeyClearsCache() {
		when(redisAccessor.get("COMM-DEV-ROLE_APIS")).thenReturn("");

		loader.refreshFromRedis();

		assertEquals(0L, ApisCache.getRevision());
		assertEquals(0, ApisCache.getRoles().size());
	}

	@Test
	void refreshFromRedis_invalidJsonKeepsOldCache() {
		ApisCache.setRevision(2L);
		ApisCache.setRoles(Map.of("ROLE_ADMIN", List.of(new ApiPermModel("GET", "/x"))));
		when(redisAccessor.get("COMM-DEV-ROLE_APIS")).thenReturn("{invalid");

		loader.refreshFromRedis();

		assertEquals(2L, ApisCache.getRevision());
		assertEquals(1, ApisCache.getRoles().size());
	}
}
