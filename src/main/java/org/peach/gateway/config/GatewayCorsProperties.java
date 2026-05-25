package org.peach.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 浏览器跨域（CORS）配置：Vite 开发源直连网关 {@code http://127.0.0.1:8090} 时使用。
 * <p>
 * 生产环境请通过配置中心或环境 profile 将 {@link #allowedOriginPatterns} 改为具体域名或受控模式，
 * 勿长期使用 {@code *} 与凭证同时开启。
 * </p>
 *
 * @author leiyangjun
 */
@ConfigurationProperties(prefix = "peach.gateway.cors")
public class GatewayCorsProperties {

	/**
	 * 允许的 Origin 模式（与 {@code Access-Control-Allow-Origin} 反射一致）。
	 * 开发默认放行 Vite 常见端口；生产在 yml / Nacos 中覆盖。
	 */
	private List<String> allowedOriginPatterns = defaultDevOriginPatterns();

	private List<String> allowedMethods = List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

	/**
	 * 预检与实际请求允许携带的请求头（含 Bearer {@code Authorization}）。
	 */
	private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With",
			"Cache-Control", "Pragma");

	private List<String> exposedHeaders = List.of("Authorization", "Content-Type");

	/** 前端 {@code axios.withCredentials} 或 Cookie 会话时为 true；须配合非 {@code *} 的 Origin 列表。 */
	private boolean allowCredentials = true;

	private long maxAge = 3600L;

	private static List<String> defaultDevOriginPatterns() {
		List<String> origins = new ArrayList<>(2);
		origins.add("http://localhost:5173");
		origins.add("http://127.0.0.1:5173");
		return origins;
	}

	public List<String> getAllowedOriginPatterns() {
		return allowedOriginPatterns;
	}

	public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns;
	}

	public List<String> getAllowedMethods() {
		return allowedMethods;
	}

	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}

	public List<String> getAllowedHeaders() {
		return allowedHeaders;
	}

	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = allowedHeaders;
	}

	public List<String> getExposedHeaders() {
		return exposedHeaders;
	}

	public void setExposedHeaders(List<String> exposedHeaders) {
		this.exposedHeaders = exposedHeaders;
	}

	public boolean isAllowCredentials() {
		return allowCredentials;
	}

	public void setAllowCredentials(boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}
}
