package org.peach.gateway.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 网关 Redis 字符串存取：调用方传入完整键，不经前缀序列化。
 *
 * @author leiyangjun
 */
@Component
@ConditionalOnBean(name = "gatewayRedisTemplate")
public class GatewayRedisAccessor {

	private final RedisTemplate<String, String> redisTemplate;

	public GatewayRedisAccessor(@Qualifier("gatewayRedisTemplate") RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * 按完整键读取字符串值。
	 */
	public String get(String fullKey) {
		return redisTemplate.opsForValue().get(fullKey);
	}

	/**
	 * 按完整键写入字符串值。
	 */
	public void set(String fullKey, String value) {
		redisTemplate.opsForValue().set(fullKey, value);
	}

}
