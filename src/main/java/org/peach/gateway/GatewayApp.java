package org.peach.gateway;

import org.peach.gateway.config.SwaggerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
	// 未引入 peach-common-start 时使用原注解；与业务侧 @PeachCloud 均为启用服务发现
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties({ SwaggerProperties.class })
public class GatewayApp {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApp.class, args);
	}
}
