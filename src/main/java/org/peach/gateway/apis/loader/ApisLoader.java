package org.peach.gateway.apis.loader;

import java.util.List;
import org.peach.gateway.apis.cache.UnauthApiCache;
import org.peach.gateway.apis.model.ApiModel;
import org.peach.gateway.apis.model.ApiSnapshot;
import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.peach.gateway.redis.GatewayRedisAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * 免鉴权快照加载器：仅从 Redis 读取 JSON、解析并刷新 {@link UnauthApiCache}，不含启动与订阅逻辑。
 *
 * @author leiyangjun
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class ApisLoader {

	private static final Logger LOG = LoggerFactory.getLogger(ApisLoader.class);

	private static final String BIZ_SNAPSHOT = "UNAUTHAPI";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GatewayRedisAccessor redisAccessor;

	private final CommRedisKeyBuilder redisKeyBuilder;

	public ApisLoader(GatewayRedisAccessor redisAccessor, CommRedisKeyBuilder redisKeyBuilder) {
		this.redisAccessor = redisAccessor;
		this.redisKeyBuilder = redisKeyBuilder;
	}

	/** 从 Redis 读取快照 JSON，解析后写入本地缓存。 */
	public void refreshFromRedis() {
		String key = snapshotKey();
		try {
			String json = redisAccessor.get(key);
			if (!StringUtils.hasText(json)) {
				LOG.warn("Redis 中无 JWT 匿名路径快照 key={}，清空本地缓存", key);
				UnauthApiCache.setRevision(0L);
				UnauthApiCache.setApis(List.of());
				return;
			}
			ApiSnapshot snapshot = JSON.readValue(json, ApiSnapshot.class);
			List<ApiModel> items = snapshot.getItems() == null ? List.of() : List.copyOf(snapshot.getItems());
			UnauthApiCache.setRevision(snapshot.getRevision());
			UnauthApiCache.setApis(items);
			LOG.info("JWT 匿名路径缓存已刷新 revision={} items={} key={}", snapshot.getRevision(), items.size(), key);
		} catch (Exception ex) {
			LOG.error("JWT 匿名路径快照加载失败，保留当前缓存 key={}", key, ex);
		}
	}

	/** 快照存储键，与 Pub/Sub 频道名相同（COMM-PROFILE-UNAUTHAPI）。 */
	public String snapshotKey() {
		return redisKeyBuilder.commKey(BIZ_SNAPSHOT);
	}

}
