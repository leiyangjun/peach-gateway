package org.peach.gateway.route.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 {@link DynamicDiscoveryRouteDefinitionLocator}。
 * <p>
 * 不在本类上使用 {@code @ConditionalOnBean(DiscoveryClient.class)}：配置类解析阶段 Bean 可能尚未注册，会导致整类被跳过。
 * </p>
 *
 * @author leiyangjun
 */
@Configuration
public class DiscoveryRouteConfiguration {

	/** 声明基于服务发现的 {@link DynamicDiscoveryRouteDefinitionLocator} Bean。 */
	@Bean
	public DynamicDiscoveryRouteDefinitionLocator dynamicDiscoveryRouteDefinitionLocator(
		DiscoveryClient discoveryClient, @Value("${spring.application.name}") String gatewayName) {
		return new DynamicDiscoveryRouteDefinitionLocator(discoveryClient, gatewayName);
	}

}
