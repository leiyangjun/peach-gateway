package org.peach.gateway.apis.cache;

import java.util.List;
import org.peach.gateway.apis.model.ApiModel;
import org.peach.gateway.common.bootstrap.config.ModuleCodeConfiguration;

/**
 * 
 * @Title: UnauthApiCache.java
 * @Description: 缓存API白名单以及当前版本号
 * @author: leiyangjun
 * @date: 2026年5月29日 17:35:22
 */
public class UnauthApiCache {

	private static volatile List<ApiModel> apis = List.of();

	private static volatile long revision;

	private UnauthApiCache() {}

	/** @return 已缓存的模块编码前缀；写入前可能为 {@code null} */
	public static Long getRevision() {
		return revision;
	}

	/**
	 * 由 {@link ModuleCodeConfiguration} 构造阶段独占调用；入参已由该校验保证合法，此处不做重复校验。
	 */
	public static void setRevision(long revision) {
		UnauthApiCache.revision = revision;
	}

	public static void setApis(List<ApiModel> apis) {
		UnauthApiCache.apis = apis;
	}

	public static List<ApiModel> getApis() {
		return apis;
	}

}
