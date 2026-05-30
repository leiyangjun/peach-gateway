package org.peach.gateway.route.discovery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 每 30 秒比对注册中心服务名集合；与上次快照不一致时发布 {@link RefreshRoutesEvent} 触发网关重新加载路由。
 *
 * @author leiyangjun
 */
@Component
@Slf4j
public class RouteListWatcher {

	private final DiscoveryClient discoveryClient;

	private final ApplicationEventPublisher eventPublisher;

	/** 上次快照，用于判断服务列表是否变化 */
	private volatile Set<String> lastSnapshot = new HashSet<>();

	public RouteListWatcher(DiscoveryClient discoveryClient, ApplicationEventPublisher eventPublisher) {
		this.discoveryClient = discoveryClient;
		this.eventPublisher = eventPublisher;
	}

	/** 定时任务入口：由 Spring 调度器调用。 */
	@Scheduled(fixedRate = 30000)
	public void checkServiceChange() {
		List<String> services = discoveryClient.getServices();
		Set<String> current = new HashSet<>(services);

		if (!current.equals(lastSnapshot)) {
			Set<String> previous = lastSnapshot;
			lastSnapshot = new HashSet<>(current);

			log.info("[RouteListWatcher] 检测到注册中心服务列表变更：此前 {} 个 → 当前 {} 个；此前={}；当前={}", previous.size(), current.size(),
				previous, current);
			log.info("[RouteListWatcher] 发布 RefreshRoutesEvent");
			eventPublisher.publishEvent(new RefreshRoutesEvent(this));
		}

	}
}
