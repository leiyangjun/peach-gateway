package org.peach.gateway.redis.config;

import org.peach.gateway.redis.GatewayRedisAccessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 网关 Redis 自动配置：在 Boot Data Redis 之后注册 {@code gatewayRedisTemplate} 与 {@link GatewayRedisAccessor}。
 * <p>
 * 须在 Boot 4 {@code DataRedisAutoConfiguration} 之后评估，否则 {@code @ConditionalOnBean} 过早判定为无 Bean 而整类被跳过。
 * </p>
 *
 * @author leiyangjun
 */
@AutoConfiguration(afterName = {
		"org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
		"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnBean(RedisConnectionFactory.class)
public class GatewayRedisAutoConfiguration {

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

	@Bean
	GatewayRedisAccessor gatewayRedisAccessor(
			@Qualifier("gatewayRedisTemplate") RedisTemplate<String, String> redisTemplate) {
		return new GatewayRedisAccessor(redisTemplate);
	}

}
