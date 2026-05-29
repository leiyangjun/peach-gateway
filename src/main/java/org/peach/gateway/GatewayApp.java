package org.peach.gateway;

import org.peach.gateway.cors.properties.CorsProperties;
import org.peach.gateway.swagger.properties.SwaggerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网关应用入口：启用 Spring Cloud 服务发现、定时任务、{@link SwaggerProperties} 配置绑定。
 *
 * @author leiyangjun
 */
@SpringBootApplication
	// 未引入 peach-common-start 时使用原注解；与业务侧 @PeachCloud 均为启用服务发现
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties({ SwaggerProperties.class, CorsProperties.class })
public class GatewayApp {

	/** 启动 Spring Boot / Cloud Gateway 应用上下文。
 *
 * @author leiyangjun
 */
	public static void main(String[] args) {
		SpringApplication.run(GatewayApp.class, args);
	}
}
