package org.peach.gateway;

import org.peach.gateway.config.PeachDocTokenProperties;
import org.peach.gateway.config.PeachGatewayAdminProperties;
import org.peach.gateway.config.PeachRedisRouteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Peach 网关启动类。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun — 启用服务发现客户端，向 Nacos 注册并由网关拉取实例。
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties({ PeachRedisRouteProperties.class, PeachDocTokenProperties.class,
		PeachGatewayAdminProperties.class })
public class PeachGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(PeachGatewayApplication.class, args);
	}
}
