package org.peach.gateway.config;

import java.util.Objects;

import org.peach.gateway.support.ModuleCodeCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 启动时校验并缓存 {@code spring.application.module-code}，供 11 位对外 {@code code} 前缀拼装使用。
 * <p>
 * 仅校验长度为四位；<strong>不</strong>做业务错误码区间类校验（网关无下游业务域）。
 * </p>
 *
 * @author leiyangjun
 */
@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayModuleCodeConfiguration {

	public GatewayModuleCodeConfiguration(@Value("${spring.application.module-code}") String moduleCode) {
		String mc = Objects.requireNonNull(moduleCode, "spring.application.module-code").trim();
		if (mc.length() != 4) {
			throw new IllegalArgumentException("spring.application.module-code 必须为四位字符串，当前长度=" + mc.length());
		}
		ModuleCodeCache.setCachedModule(mc);
	}
}
