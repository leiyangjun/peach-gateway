package org.peach.gateway.permission.listener;

import org.peach.gateway.permission.cache.ApisCache;
import org.peach.gateway.permission.loader.RoleApisLoader;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * 角色-API 快照 Pub/Sub 监听器：比对 revision 后委托加载器拉取 Redis 全量快照。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
@Slf4j
public class ApisPersChannelListener {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final RoleApisLoader snapshotLoader;

	public ApisPersChannelListener(RoleApisLoader snapshotLoader) {
		this.snapshotLoader = snapshotLoader;
	}

	/**
	 * Pub/Sub 回调：消息体为 common-service 发布的 revision（纯文本或 Jackson Long）。
	 */
	public void onMessage(String message) {
		log.info("RoleApisChannelListener 收到消息 body={}", message);
		Long incoming = parseRevision(message);
		if (incoming == null) {
			log.debug("角色-API 快照频道消息无法解析 revision，忽略 body={}", message);
			return;
		}
		long localRev = ApisCache.getRevision();
		if (incoming <= localRev) {
			log.debug("角色-API 快照 revision 未递增，跳过拉取 incoming={} local={}", incoming, localRev);
			return;
		}
		log.info("收到角色-API 快照变更通知 revision={} local={}，拉取快照", incoming, localRev);
		snapshotLoader.refreshFromRedis();
	}

	private static Long parseRevision(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String trimmed = raw.trim();
		if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
				|| (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
			trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
		}
		try {
			return Long.parseLong(trimmed);
		}
		catch (NumberFormatException ex) {
			try {
				return JSON.readValue(trimmed, Long.class);
			}
			catch (Exception jsonEx) {
				return null;
			}
		}
	}
}
