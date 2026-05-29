package org.peach.gateway.apis.config;

import org.peach.gateway.apis.listener.ApisChannelListener;
import org.peach.gateway.apis.loader.ApisLoader;
import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 免鉴权快照 Pub/Sub 监听：须在 Boot Data Redis 自动配置之后注册，否则 {@code @ConditionalOnBean} 过早跳过。
 *
 * @author leiyangjun
 */
@AutoConfiguration(afterName = {"org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
	"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
@ConditionalOnBean(RedisConnectionFactory.class)
@Slf4j
public class RedisListenerConfig {

	private static final String BIZ_SNAPSHOT = "UNAUTHAPI_VERSION";

	@Bean
	ApisChannelListener apisChannelListener(ApisLoader apisLoader) {
		return new ApisChannelListener(apisLoader);
	}

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
		log.debug("注册 redis监听事件，事件监听信道：{}", channel);
		container.addMessageListener(apisChannelListenerAdapter, new ChannelTopic(channel));
		return container;
	}

}
