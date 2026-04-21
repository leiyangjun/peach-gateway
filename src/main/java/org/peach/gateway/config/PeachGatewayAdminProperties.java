package org.peach.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关管理端（如 Redis 路由 CRUD）相关配置，与「经网关转发的业务 API」权限解耦。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun — 业务侧统一鉴权请使用独立 GlobalFilter 或 {@code SecurityWebFilterChain}，仅匹配业务路径，勿与本处管理端逻辑混写。
 * </p>
 */
@ConfigurationProperties(prefix = "peach.gateway.admin")
public class PeachGatewayAdminProperties {

	/**
	 * Redis 路由 CRUD 的 URL 前缀（须与 Controller 上 {@code @RequestMapping} 一致）。
	 */
	private String redisRoutesPath = "/admin/gateway/redis-routes";

	private final Security security = new Security();

	public String getRedisRoutesPath() {
		return redisRoutesPath;
	}

	public void setRedisRoutesPath(String redisRoutesPath) {
		this.redisRoutesPath = redisRoutesPath;
	}

	public Security getSecurity() {
		return security;
	}

	/**
	 * 仅作用于 {@link #redisRoutesPath} 下请求；不开启则不做 API Key 校验。
	 */
	public static class Security {

		/**
		 * 是否对 Redis 路由 CRUD 启用 API Key 校验（与下游业务 API 无关）。
		 */
		private boolean apiKeyEnabled = false;

		/**
		 * 客户端携带密钥的请求头名称。
		 */
		private String apiKeyHeader = "X-Admin-Api-Key";

		/**
		 * 服务端期望的密钥；务必使用环境变量注入，勿写入仓库。
		 */
		private String apiKeyValue = "";

		public boolean isApiKeyEnabled() {
			return apiKeyEnabled;
		}

		public void setApiKeyEnabled(boolean apiKeyEnabled) {
			this.apiKeyEnabled = apiKeyEnabled;
		}

		public String getApiKeyHeader() {
			return apiKeyHeader;
		}

		public void setApiKeyHeader(String apiKeyHeader) {
			this.apiKeyHeader = apiKeyHeader;
		}

		public String getApiKeyValue() {
			return apiKeyValue;
		}

		public void setApiKeyValue(String apiKeyValue) {
			this.apiKeyValue = apiKeyValue;
		}
	}
}
