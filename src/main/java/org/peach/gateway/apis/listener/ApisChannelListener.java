package org.peach.gateway.apis.listener;

import org.peach.gateway.apis.cache.UnauthApiCache;
import org.peach.gateway.apis.loader.ApisLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * 免鉴权 API 快照 Pub/Sub 监听器：比对 revision 后委托加载器拉取 Redis 全量快照。
 *
 * @author leiyangjun
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class ApisChannelListener {

	private static final Logger LOG = LoggerFactory.getLogger(ApisChannelListener.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ApisLoader snapshotLoader;

	public ApisChannelListener(ApisLoader snapshotLoader) {
		this.snapshotLoader = snapshotLoader;
	}

	/**
	 * Pub/Sub 回调：消息体为 common-service 发布的 revision（Jackson JSON Long，如 {@code 2}）。
	 */
	public void onMessage(String message) {
		Long incoming = parseRevision(message);
		if (incoming == null) {
			LOG.warn("免鉴权快照频道消息无法解析 revision，忽略 body={}", message);
			return;
		}
		Long local = UnauthApiCache.getRevision();
		long localRev = local == null ? 0L : local;
		if (incoming <= localRev) {
			LOG.debug("免鉴权快照 revision 未递增，跳过拉取 incoming={} local={}", incoming, localRev);
			return;
		}
		LOG.info("收到免鉴权快照变更通知 revision={} local={}，拉取快照", incoming, localRev);
		snapshotLoader.refreshFromRedis();
	}

	private static Long parseRevision(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return JSON.readValue(raw.trim(), Long.class);
		}
		catch (Exception ex) {
			return null;
		}
	}

}
