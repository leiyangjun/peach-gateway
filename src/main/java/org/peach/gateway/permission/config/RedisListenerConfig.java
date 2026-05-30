package org.peach.gateway.permission.config;

import org.peach.gateway.redis.CommRedisKeyBuilder;
import org.peach.gateway.permission.listener.ApisPersChannelListener;
import org.peach.gateway.permission.listener.RoleUsersChannelListener;
import org.peach.gateway.permission.loader.RoleApisLoader;
import org.peach.gateway.permission.loader.RoleUsersLoader;
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
 * 角色权限快照 Pub/Sub 监听：频道为 ROLE_APIS / ROLE_USERS（非 _VERSION 后缀）。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
@AutoConfiguration(afterName = { "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
		"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration" })
@ConditionalOnBean(RedisConnectionFactory.class)
@Slf4j
public class RedisListenerConfig {

	private static final String BIZ_ROLE_APIS = "ROLE_APIS";

	private static final String BIZ_ROLE_USERS = "ROLE_USERS";

	@Bean
	ApisPersChannelListener roleApisChannelListener(RoleApisLoader roleApisLoader) {
		return new ApisPersChannelListener(roleApisLoader);
	}

	@Bean
	RoleUsersChannelListener roleUsersChannelListener(RoleUsersLoader roleUsersLoader) {
		return new RoleUsersChannelListener(roleUsersLoader);
	}

	@Bean
	MessageListenerAdapter roleApisChannelListenerAdapter(ApisPersChannelListener roleApisChannelListener) {
		MessageListenerAdapter adapter = new MessageListenerAdapter(roleApisChannelListener, "onMessage");
		adapter.setStringSerializer(StringRedisSerializer.UTF_8);
		return adapter;
	}

	@Bean
	MessageListenerAdapter roleUsersChannelListenerAdapter(RoleUsersChannelListener roleUsersChannelListener) {
		MessageListenerAdapter adapter = new MessageListenerAdapter(roleUsersChannelListener, "onMessage");
		adapter.setStringSerializer(StringRedisSerializer.UTF_8);
		return adapter;
	}

	@Bean
	RedisMessageListenerContainer rolePermRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
			MessageListenerAdapter roleApisChannelListenerAdapter,
			MessageListenerAdapter roleUsersChannelListenerAdapter, CommRedisKeyBuilder redisKeyBuilder) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		String apisChannel = redisKeyBuilder.commKey(BIZ_ROLE_APIS);
		String usersChannel = redisKeyBuilder.commKey(BIZ_ROLE_USERS);
		log.debug("注册 redis监听事件，事件监听信道：{}", apisChannel);
		container.addMessageListener(roleApisChannelListenerAdapter, new ChannelTopic(apisChannel));
		log.debug("注册 redis监听事件，事件监听信道：{}", usersChannel);
		container.addMessageListener(roleUsersChannelListenerAdapter, new ChannelTopic(usersChannel));
		return container;
	}
}
