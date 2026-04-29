package org.peach.gateway.route.discovery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 注册中心心跳时仅在服务 id 集合变化时刷新网关路由，减少无意义 RefreshRoutes。
 */
@Component
public class DiscoveryRouteHeartbeatListener implements ApplicationListener<HeartbeatEvent> {

	private static final Logger log = LoggerFactory.getLogger(DiscoveryRouteHeartbeatListener.class);

	private static final Duration FINGERPRINT_BLOCK = Duration.ofSeconds(5);

	private final ApplicationEventPublisher publisher;

	private final ObjectProvider<DiscoveryClient> blockingDiscoveryClient;

	private final ObjectProvider<ReactiveDiscoveryClient> reactiveDiscoveryClient;

	private volatile String lastServicesFingerprint = "";

	public DiscoveryRouteHeartbeatListener(ApplicationEventPublisher publisher, ObjectProvider<DiscoveryClient> blockingDiscoveryClient,
			ObjectProvider<ReactiveDiscoveryClient> reactiveDiscoveryClient) {
		this.publisher = publisher;
		this.blockingDiscoveryClient = blockingDiscoveryClient;
		this.reactiveDiscoveryClient = reactiveDiscoveryClient;
	}

	@Override
	public void onApplicationEvent(HeartbeatEvent event) {
		String fp = resolveFingerprint();
		if (fp.equals(lastServicesFingerprint)) {
			if (log.isTraceEnabled()) {
				log.trace("Discovery 心跳：服务列表未变，跳过路由刷新");
			}
			return;
		}
		lastServicesFingerprint = fp;
		if (log.isDebugEnabled()) {
			log.debug("Discovery 心跳：服务列表已变化，发布 RefreshRoutesEvent");
		}
		publisher.publishEvent(new RefreshRoutesEvent(this));
	}

	private String resolveFingerprint() {
		ReactiveDiscoveryClient reactive = reactiveDiscoveryClient.getIfAvailable();
		if (reactive != null) {
			return reactive.getServices().collectList().map(list -> {
				var sorted = new ArrayList<>(list);
				sorted.sort(String.CASE_INSENSITIVE_ORDER);
				return sorted.stream().collect(Collectors.joining("\0"));
			}).defaultIfEmpty("").block(FINGERPRINT_BLOCK);
		}
		DiscoveryClient blocking = blockingDiscoveryClient.getIfAvailable();
		if (blocking == null) {
			return "";
		}
		return blocking.getServices().stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining("\0"));
	}
}
