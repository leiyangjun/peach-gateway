package org.peach.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 自定义路由相关配置。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
@ConfigurationProperties(prefix = "peach.gateway.redis-routes")
public class PeachRedisRouteProperties {

	/**
	 * 是否从 Redis 加载自定义 {@link org.springframework.cloud.gateway.route.RouteDefinition} 列表（JSON 数组）。
	 */
	private boolean enabled;

	/**
	 * Redis 中存储自定义路由 JSON 的键。
	 */
	private String redisKey = "peach:gateway:routes:custom";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getRedisKey() {
		return redisKey;
	}

	public void setRedisKey(String redisKey) {
		this.redisKey = redisKey;
	}
}
