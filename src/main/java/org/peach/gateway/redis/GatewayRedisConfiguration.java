package org.peach.gateway.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 网关 Redis 基础设施：仅注册字符串 {@link RedisTemplate}，完整键由 {@link CommRedisKeyBuilder} 拼装。
 *
 * @author leiyangjun
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class GatewayRedisConfiguration {

	/**
	 * 字符串 RedisTemplate：Key/Value 均为明文，不经 MODULE-PROFILE 前缀序列化器。
	 */
	@Bean
	RedisTemplate<String, String> gatewayRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setValueSerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);
		template.setHashValueSerializer(stringSerializer);
		template.setEnableDefaultSerializer(false);
		template.afterPropertiesSet();
		return template;
	}

}
