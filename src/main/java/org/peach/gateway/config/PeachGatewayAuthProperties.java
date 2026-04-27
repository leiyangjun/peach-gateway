package org.peach.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关统一鉴权配置（SecurityWebFilterChain）。
 * <p>
 * 创作日期：2026-04-21，作者：leiyangjun
 * </p>
 */
@ConfigurationProperties(prefix = "peach.gateway.auth")
public class PeachGatewayAuthProperties {

	/**
	 * 是否启用统一鉴权过滤器。
	 */
	private boolean enabled = true;

	/**
	 * 鉴权请求头名称，默认 Authorization。
	 */
	private String headerName = "Authorization";

	/**
	 * 是否要求 Bearer 前缀。
	 */
	private boolean requireBearerPrefix = true;

	/**
	 * 白名单路径（支持 Ant 风格），默认放行登录接口与健康检查。
	 */
	private List<String> whitelistPaths = new ArrayList<>(
			List.of("/actuator/**", "/*/auth/login/**"));

	/**
	 * 与认证服务共享的 HS256 密钥，建议通过环境变量注入。
	 */
	private String jwtSecret = "please-change-this-to-32-plus-bytes-secret";

	/**
	 * JWT 期望发行方（iss）；为空时不校验 issuer。
	 */
	private String issuer;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		this.headerName = headerName;
	}

	public boolean isRequireBearerPrefix() {
		return requireBearerPrefix;
	}

	public void setRequireBearerPrefix(boolean requireBearerPrefix) {
		this.requireBearerPrefix = requireBearerPrefix;
	}

	public List<String> getWhitelistPaths() {
		return whitelistPaths;
	}

	public void setWhitelistPaths(List<String> whitelistPaths) {
		this.whitelistPaths = whitelistPaths;
	}

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
}
