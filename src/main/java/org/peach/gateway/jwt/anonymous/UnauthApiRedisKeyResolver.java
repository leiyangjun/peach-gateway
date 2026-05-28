package org.peach.gateway.jwt.anonymous;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 按约定拼出 common-service 写入的完整 Redis 键（COMM-ACTIVE-业务Key）；网关不引 peach-common-start。
 */
@Component
public class UnauthApiRedisKeyResolver {

	/** 免鉴权数据归属模块（与 common-service module-code 一致） */
	public static final String DATA_MODULE = "COMM";

	private final String active;

	public UnauthApiRedisKeyResolver(@Value("${spring.profiles.active:dev}") String active) {
		this.active = active;
	}

	/**
	 * 快照存储键，与 Pub/Sub 频道名相同。
	 */
	public String snapshotAndChannelKey() {
		return buildFullKey(UnauthApiRedisBusinessKeys.BIZ_SNAPSHOT);
	}

	public String revisionKey() {
		return buildFullKey(UnauthApiRedisBusinessKeys.BIZ_REVISION);
	}

	private String buildFullKey(String businessKey) {
		String act = StringUtils.hasText(active) ? active.trim().toUpperCase() : "DEFAULT";
		String tail = businessKey.trim();
		if (tail.indexOf('-') >= 0) {
			throw new IllegalArgumentException("businessKey 不得包含 '-'");
		}
		return DATA_MODULE + "-" + act + "-" + tail;
	}
}
