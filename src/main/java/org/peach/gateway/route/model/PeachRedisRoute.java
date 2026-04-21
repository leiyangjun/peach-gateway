package org.peach.gateway.route.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Redis 中存储的「Peach 自定义路由」契约模型（与 Spring {@code RouteDefinition} 解耦，便于 OpenAPI / 管理端文档描述）。
 * <p>
 * Redis 值为 <strong>本类型的 JSON 数组</strong>，例如：
 * </p>
 * <pre>{@code
 * [
 *   {
 *     "routeId": "demo-order",
 *     "order": 10,
 *     "uriType": "REGISTERED_SERVICE",
 *     "target": "order-service",
 *     "pathPattern": "/api/order/**",
 *     "httpMethods": ["GET", "POST"],
 *     "stripPrefixParts": 2,
 *     "description": "订单服务聚合前缀示例"
 *   },
 *   {
 *     "routeId": "demo-static",
 *     "order": 20,
 *     "uriType": "EXPLICIT_URI",
 *     "target": "https://httpbin.org",
 *     "pathPattern": "/echo/**",
 *     "stripPrefixParts": 1
 *   }
 * ]
 * }</pre>
 * <p>
 * HTTP CRUD（需 {@code peach.gateway.redis-routes.enabled=true}）：默认前缀见配置项 {@code peach.gateway.admin.redis-routes-path}。
 * </p>
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeachRedisRoute {

	/**
	 * 路由唯一标识（对应 Spring {@code RouteDefinition#id}）。
	 */
	@NotBlank
	private String routeId;

	/**
	 * 匹配优先级，数值越小越优先（对应 Spring {@code RouteDefinition#order}）。
	 */
	private Integer order = 0;

	/**
	 * 目标类型：注册中心服务名或完整 URI。
	 */
	@NotNull
	private PeachRouteTargetType uriType;

	/**
	 * 当 {@link #uriType} 为 {@link PeachRouteTargetType#REGISTERED_SERVICE} 时填写 Nacos 等服务上的 <strong>服务名</strong>（不要写 {@code lb://} 前缀）。
	 * 当为 {@link PeachRouteTargetType#EXPLICIT_URI} 时填写完整 {@code http(s)://...}。
	 */
	@NotBlank
	private String target;

	/**
	 * 路径匹配表达式（Ant 风格），将映射为 Gateway {@code Path} 断言的 {@code pattern}。
	 */
	@NotBlank
	private String pathPattern;

	/**
	 * 可选；限制 HTTP 方法（大写），映射为 {@code Method} 断言；为空或省略表示不限制方法。
	 */
	private List<String> httpMethods;

	/**
	 * 可选；转发前剥离路径前若干段，映射为 {@code StripPrefix}；为 {@code null} 或 0 表示不剥离。
	 */
	private Integer stripPrefixParts;

	/**
	 * 可选；仅用于管理端/文档展示，网关逻辑忽略。
	 */
	private String description;

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public PeachRouteTargetType getUriType() {
		return uriType;
	}

	public void setUriType(PeachRouteTargetType uriType) {
		this.uriType = uriType;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getPathPattern() {
		return pathPattern;
	}

	public void setPathPattern(String pathPattern) {
		this.pathPattern = pathPattern;
	}

	public List<String> getHttpMethods() {
		return httpMethods;
	}

	public void setHttpMethods(List<String> httpMethods) {
		this.httpMethods = httpMethods;
	}

	public Integer getStripPrefixParts() {
		return stripPrefixParts;
	}

	public void setStripPrefixParts(Integer stripPrefixParts) {
		this.stripPrefixParts = stripPrefixParts;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
