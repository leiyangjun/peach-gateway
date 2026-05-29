package org.peach.gateway.apis.init;

import org.peach.gateway.apis.loader.ApisLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * 
 * @Title: StartupLoaderEvent.java
 * @Description: springboot启动事件，启动时加载redis缓存
 * @author: Administrator
 * @date: 2026年5月29日 18:24:42
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class StartupLoaderEvent {

	private final ApisLoader snapshotLoader;

	public StartupLoaderEvent(ApisLoader snapshotLoader) {
		this.snapshotLoader = snapshotLoader;
	}

	/** 启动时全量拉取 Redis 快照。 */
	@EventListener(ApplicationReadyEvent.class)
	public void loadOnStartup() {
		snapshotLoader.refreshFromRedis();
		//
	}

}
