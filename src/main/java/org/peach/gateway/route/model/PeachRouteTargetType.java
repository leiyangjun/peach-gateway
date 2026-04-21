package org.peach.gateway.route.model;

/**
 * 自定义路由的目标类型（写入 Redis 的 JSON 中使用，便于文档与前端枚举对齐）。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
public enum PeachRouteTargetType {

	/**
	 * 使用注册中心实例，网关 URI 形态为 {@code lb://服务名}（需 Nacos + LoadBalancer）。
	 */
	REGISTERED_SERVICE,

	/**
	 * 使用显式地址，如 {@code https://api.example.com} 或 {@code http://10.0.0.1:8080}。
	 */
	EXPLICIT_URI
}
