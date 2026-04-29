package org.peach.gateway.route.discovery;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 {@link DynamicDiscoveryRouteDefinitionLocator}（优先 Reactive 发现客户端）。
 * <p>
 * 不在本类上使用 {@code @ConditionalOnBean(DiscoveryClient.class)}：配置类解析阶段 Bean 可能尚未注册，会导致整类被跳过。
 * </p>
 */
@Configuration
public class DiscoveryRouteConfiguration {

	@Bean
	public DynamicDiscoveryRouteDefinitionLocator dynamicDiscoveryRouteDefinitionLocator(
			ObjectProvider<ReactiveDiscoveryClient> reactiveDiscoveryClient, ObjectProvider<DiscoveryClient> discoveryClient) {
		return new DynamicDiscoveryRouteDefinitionLocator(reactiveDiscoveryClient, discoveryClient);
	}
}
