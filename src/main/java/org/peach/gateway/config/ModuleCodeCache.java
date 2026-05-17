package org.peach.gateway.config;

/**
 * 缓存网关 {@code spring.application.module-code}，供过滤器与异常处理等无 Spring 注入处读取。
 */
public final class ModuleCodeCache {

	private static volatile String moduleCode;

	private ModuleCodeCache() {
	}

	/** @return 已缓存的模块编码前缀；写入前可能为 {@code null} */
	public static String get() {
		return moduleCode;
	}

	/**
	 * 由 {@link GatewayModuleCodeConfiguration} 构造阶段独占调用；入参已由该校验保证合法，此处不做重复校验。
	 */
	static void setCachedModule(String moduleCode) {
		ModuleCodeCache.moduleCode = moduleCode;
	}
}
