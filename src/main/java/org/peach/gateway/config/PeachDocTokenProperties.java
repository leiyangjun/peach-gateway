package org.peach.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 经网关转发 OpenAPI 文档时的 Token 传播配置（可扩展对接 Vue 管理端 / OIDC 等）。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun — 建议通过环境变量注入 {@link #staticValue}，勿提交密钥。
 * </p>
 */
@ConfigurationProperties(prefix = "peach.gateway.doc.token-propagation")
public class PeachDocTokenProperties {

	private boolean enabled;

	/**
	 * 追加到下游文档请求上的请求头名称，例如 Authorization、X-API-Key。
	 */
	private String headerName = "Authorization";

	/**
	 * 静态 Token；后续可改为从 Redis、Session 或自定义 {@code GlobalFilter} 解析。
	 */
	private String staticValue = "";

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

	public String getStaticValue() {
		return staticValue;
	}

	public void setStaticValue(String staticValue) {
		this.staticValue = staticValue;
	}
}
