package org.peach.gateway.permission.loader;

import java.util.List;
import java.util.Map;
import org.peach.gateway.permission.cache.ApisCache;
import org.peach.gateway.permission.model.ApiPermModel;
import org.peach.gateway.permission.model.RoleApisSnapshot;
import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.peach.gateway.redis.GatewayRedisAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * 角色-API 快照加载器：从 Redis 读取 JSON 并刷新 {@link ApisCache}。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
@Component
@Slf4j
public class RoleApisLoader {

	private static final String BIZ_SNAPSHOT = "ROLE_APIS";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GatewayRedisAccessor redisAccessor;

	private final CommRedisKeyBuilder redisKeyBuilder;

	public RoleApisLoader(GatewayRedisAccessor redisAccessor, CommRedisKeyBuilder redisKeyBuilder) {
		this.redisAccessor = redisAccessor;
		this.redisKeyBuilder = redisKeyBuilder;
	}

	/** 从 Redis 读取快照 JSON，解析后写入本地缓存。 */
	public void refreshFromRedis() {
		String key = snapshotKey();
		try {
			String json = redisAccessor.get(key);
			if (!StringUtils.hasText(json)) {
				log.warn("Redis 中无角色-API 快照 key={}，清空本地缓存", key);
				ApisCache.setRevision(0L);
				ApisCache.setRoles(Map.of());
				return;
			}
			RoleApisSnapshot snapshot = JSON.readValue(json, RoleApisSnapshot.class);
			Map<String, List<ApiPermModel>> roles = snapshot.getRoles() == null ? Map.of()
					: Map.copyOf(snapshot.getRoles());
			ApisCache.setRevision(snapshot.getRevision());
			ApisCache.setRoles(roles);
			log.info("角色-API 缓存已刷新 revision={} roles={} key={}", snapshot.getRevision(), roles.size(), key);
		}
		catch (Exception ex) {
			log.error("角色-API 快照加载失败，保留当前缓存 key={}", key, ex);
		}
	}

	/** 快照存储键，与 Pub/Sub 频道名相同（COMM-PROFILE-ROLE_APIS）。 */
	public String snapshotKey() {
		return redisKeyBuilder.commKey(BIZ_SNAPSHOT);
	}
}
