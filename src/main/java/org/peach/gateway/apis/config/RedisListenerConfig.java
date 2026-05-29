package org.peach.gateway.apis.config;

import org.peach.gateway.apis.listener.ApisChannelListener;
import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisListenerConfig {

	private static final String BIZ_SNAPSHOT = "UNAUTHAPI";

	@Bean
	MessageListenerAdapter apisChannelListenerAdapter(ApisChannelListener apisChannelListener) {
		MessageListenerAdapter adapter = new MessageListenerAdapter(apisChannelListener, "onMessage");
		adapter.setStringSerializer(StringRedisSerializer.UTF_8);
		return adapter;
	}

	@Bean
	RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
			MessageListenerAdapter apisChannelListenerAdapter, CommRedisKeyBuilder redisKeyBuilder) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		String channel = redisKeyBuilder.commKey(BIZ_SNAPSHOT);
		container.addMessageListener(apisChannelListenerAdapter, new ChannelTopic(channel));
		return container;
	}

}
