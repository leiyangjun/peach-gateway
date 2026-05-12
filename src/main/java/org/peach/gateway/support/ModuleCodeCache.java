package org.peach.gateway.support;

/**
 * 缓存网关 {@code spring.application.module-code}，供过滤器与异常处理等无 Spring 注入处读取。
 *
 * @author leiyangjun
 */
public final class ModuleCodeCache {

	private static volatile String cachedModule;

	private ModuleCodeCache() {
	}

	public static void setCachedModule(String moduleCode) {
		cachedModule = moduleCode;
	}

	public static String get() {
		return cachedModule;
	}

	/**
	 * 在模块码已由 {@link org.peach.gateway.config.GatewayModuleCodeConfiguration} 写入后使用。
	 */
	public static String requireModuleCode() {
		String c = cachedModule;
		if (c == null || c.isBlank()) {
			throw new IllegalStateException("spring.application.module-code 尚未写入 ModuleCodeCache，请确认 GatewayModuleCodeConfiguration 已加载");
		}
		return c;
	}
}
