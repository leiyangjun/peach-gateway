package org.peach.gateway.jwt.anonymous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.json.JsonMapper;

/**
 * 从 Redis 拉取匿名路径快照并刷新 {@link JwtAnonymousRuleCache}。
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class JwtAnonymousSnapshotRefresher {

	private static final Logger LOG = LoggerFactory.getLogger(JwtAnonymousSnapshotRefresher.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final UnauthApiRedisReader redisReader;

	private final JwtAnonymousRuleCache ruleCache;

	public JwtAnonymousSnapshotRefresher(UnauthApiRedisReader redisReader, JwtAnonymousRuleCache ruleCache) {
		this.redisReader = redisReader;
		this.ruleCache = ruleCache;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void loadOnStartup() {
		reload();
	}

	public void reload() {
		try {
			String json = redisReader.getSnapshotJson();
			String key = redisReader.refreshChannel();
			if (!StringUtils.hasText(json)) {
				LOG.warn("Redis 中无 JWT 匿名路径快照 key={}，使用内置兜底规则", key);
				ruleCache.replaceFromSnapshot(null);
				return;
			}
			JwtAnonymousSnapshot snapshot = JSON.readValue(json, JwtAnonymousSnapshot.class);
			ruleCache.replaceFromSnapshot(snapshot);
			LOG.info("JWT 匿名路径缓存已刷新 revision={} items={} key={}", snapshot.getRevision(),
				snapshot.getItems() == null ? 0 : snapshot.getItems().size(), key);
		}
		catch (Exception ex) {
			LOG.error("JWT 匿名路径快照加载失败，保留当前缓存或兜底规则", ex);
		}
	}
}
