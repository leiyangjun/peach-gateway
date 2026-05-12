package org.peach.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Swagger 文档门户：总开关 {@code peach.swagger.enabled}。
 * <p>
 * 门户 HTML 是否暴露另受 {@code peach.swagger.html-enabled} 与 profile（prod/product/produce）默认关闭规则约束，
 * 见 {@link org.peach.gateway.config.condition.SwaggerEnabledCondition}。
 * </p>
 *
 * @author leiyangjun
 */
@ConfigurationProperties(prefix = "peach.swagger")
public class SwaggerProperties {

	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
