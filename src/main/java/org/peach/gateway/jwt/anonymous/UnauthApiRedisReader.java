package org.peach.gateway.jwt.anonymous;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.stereotype.Component;

/**
 * 网关侧读取 common-service 写入的免鉴权快照（完整 Redis 键，不经 Template 前缀）。
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class UnauthApiRedisReader {

	private final RedisConnectionFactory connectionFactory;

	private final UnauthApiRedisKeyResolver keyResolver;

	public UnauthApiRedisReader(RedisConnectionFactory connectionFactory, UnauthApiRedisKeyResolver keyResolver) {
		this.connectionFactory = connectionFactory;
		this.keyResolver = keyResolver;
	}

	public String getSnapshotJson() {
		return getString(keyResolver.snapshotAndChannelKey());
	}

	public String refreshChannel() {
		return keyResolver.snapshotAndChannelKey();
	}

	private String getString(String fullKey) {
		return execute(conn -> {
			byte[] raw = conn.stringCommands().get(fullKey.getBytes(StandardCharsets.UTF_8));
			return raw == null ? null : new String(raw, StandardCharsets.UTF_8);
		});
	}

	private <T> T execute(RedisCallback<T> callback) {
		RedisConnection conn = RedisConnectionUtils.getConnection(connectionFactory);
		try {
			return callback.doInRedis(conn);
		}
		finally {
			RedisConnectionUtils.releaseConnection(conn, connectionFactory);
		}
	}

	@FunctionalInterface
	private interface RedisCallback<T> {

		T doInRedis(RedisConnection connection) throws DataAccessException;
	}
}
