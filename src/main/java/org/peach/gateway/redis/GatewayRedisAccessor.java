package org.peach.gateway.redis;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * 网关 Redis 字符串存取：调用方传入完整键，不经前缀序列化。
 * <p>
 * 由 {@link org.peach.gateway.redis.config.GatewayRedisAutoConfiguration} 注册；未配置 Redis 时 Bean 不存在。
 * </p>
 *
 * @author leiyangjun
 */
public class GatewayRedisAccessor {

	private final RedisTemplate<String, String> redisTemplate;

	public GatewayRedisAccessor(RedisTemplate<String, String> redisTemplate) {
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
