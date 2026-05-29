package org.peach.gateway.common.bootstrap.config;

import java.util.Objects;
import org.peach.gateway.common.bootstrap.cache.ModuleCodeCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 
 * @Title: ModuleCodeConfiguration.java
 * @Description: 启动时初始化缓存模块信息，用于统一消息处理等
 * @author: 雷阳军
 * @date: 2026年5月29日 17:15:14
 */
@Configuration(proxyBeanMethods = false)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ModuleCodeConfiguration {

	/**
	 * 读取并校验 {@code spring.application.module-code}（必须为四位），通过后写入 {@link ModuleCodeCache}。
	 */
	public ModuleCodeConfiguration(@Value("${spring.application.module-code}") String moduleCode) {
		String mc = Objects.requireNonNull(moduleCode, "spring.application.module-code").trim();
		if (mc.length() != 4) {
			throw new IllegalArgumentException("spring.application.module-code 必须为四位字符串，当前长度=" + mc.length());
		}
		ModuleCodeCache.setCachedModule(moduleCode);
	}
}
