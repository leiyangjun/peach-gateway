package org.peach.gateway.permission.init;

import org.peach.gateway.permission.loader.RoleApisLoader;
import org.peach.gateway.permission.loader.RoleUsersLoader;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动时加载角色权限 Redis 快照至本地缓存。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
@Slf4j
@Component
public class RolePermStartupLoaderEvent {

	private final RoleApisLoader roleApisLoader;

	private final RoleUsersLoader roleUsersLoader;

	public RolePermStartupLoaderEvent(RoleApisLoader roleApisLoader, RoleUsersLoader roleUsersLoader) {
		this.roleApisLoader = roleApisLoader;
		this.roleUsersLoader = roleUsersLoader;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void loadOnStartup(ApplicationReadyEvent event) {
		log.info("应用已就绪，开始加载API权限 Redis 快照...");
		roleUsersLoader.refreshFromRedis();
		roleApisLoader.refreshFromRedis();
		log.info("应用已就绪，开始加载API权限 Redis 快照完成");
	}
}
