package org.peach.gateway.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 网关侧 Redis 键前缀构建：{@code {MODULE}-{PROFILE}-{业务Key}}，与 common-service
 * {@link org.peach.common.mvc.redis.RedisKeyBuilder} 规则一致。
 *
 * @author leiyangjun
 */
@Component
public class CommRedisKeyBuilder {

	/** 数据归属模块（与 common-service module-code 一致） */
	public static final String DATA_MODULE = "COMM";

	private final String active;

	public CommRedisKeyBuilder(@Value("${spring.profiles.active:dev}") String active) {
		this.active = active;
	}

	/**
	 * 便捷方法：按 COMM 模块与当前 profile 拼出完整键。
	 */
	public String commKey(String businessKey) {
		return buildKey(DATA_MODULE, active, businessKey);
	}

	/**
	 * 指定模块、环境与业务尾段拼出完整 Redis 键（不经 RedisTemplate 前缀）。
	 *
	 * @param module      四位模块码，如 COMM
	 * @param profile     环境标识，如 dev、test
	 * @param businessKey 业务尾段
	 * @return {@code MODULE-PROFILE-businessKey}
	 */
	public String buildKey(String module, String profile, String businessKey) {
		String tail = validateBusinessKey(businessKey);
		String moduleSeg = normalizeSegment(module, "module");
		String activeSeg = normalizeSegment(profile, "active");
		return moduleSeg + "-" + activeSeg + "-" + tail;
	}

	private static String validateBusinessKey(String businessKey) {
		if (businessKey == null) {
			throw new IllegalArgumentException("businessKey 不能为空");
		}
		String tail = businessKey.trim();
		if (tail.isEmpty()) {
			throw new IllegalArgumentException("businessKey 不能为空");
		}
		if (tail.indexOf(':') >= 0 || tail.indexOf('-') >= 0) {
			throw new IllegalArgumentException("businessKey 不得包含 ':' 或 '-'");
		}
		return tail;
	}

	private static String normalizeSegment(String raw, String segmentName) {
		if (raw == null) {
			throw new IllegalArgumentException(segmentName + " 不能为空");
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			throw new IllegalArgumentException(segmentName + " 不能为空");
		}
		return t.toUpperCase();
	}
}
