package org.peach.gateway.jwt.anonymous;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * 订阅免鉴权快照刷新频道（与快照 Redis 键同名：COMM-ACTIVE-UNAUTHAPI）。
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class JwtAnonymousRedisConfiguration {

	@Bean
	RedisMessageListenerContainer jwtAnonymousRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
			JwtAnonymousSnapshotRefresher refresher, UnauthApiRedisReader redisReader) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		MessageListenerAdapter adapter = new MessageListenerAdapter(refresher, "reload");
		container.addMessageListener(adapter, new ChannelTopic(redisReader.refreshChannel()));
		return container;
	}
}
