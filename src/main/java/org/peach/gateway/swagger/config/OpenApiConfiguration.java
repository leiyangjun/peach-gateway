package org.peach.gateway.swagger.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * 
 * @Title: OpenApiConfiguration.java
 * @Description: 网关swagger配置类
 * @author: 雷阳军
 * @date: 2026年5月29日 16:53:02
 */
@Configuration
public class OpenApiConfiguration {

	/** 注册网关聚合 OpenAPI 定义（含 Servers 与文档门户外链）。 */
	@Bean
	public OpenAPI gatewayOpenApi(@Value("${spring.application.name}") String applicationName) {
		String prefix = "/" + applicationName;
		ExternalDocumentation portal = new ExternalDocumentation().description("回到网关 API 文档门户").url("/index.html");

		Info info = new Info().title("peach-gateway").version("0.0.1-SNAPSHOT")
			.description("Peach API 网关自身 REST 接口说明。" + " 经网关访问其它微服务 Swagger：`/{serviceId}/swagger-ui.html`。"
				+ " Servers 列表首项（默认）为 `" + prefix + "`；根路径直连时请改选 `/`。" + "\n\n[回到网关 API 文档门户](/index.html)");

		return new OpenAPI().info(info).externalDocs(portal)
			.addServersItem(new Server().url("/").description("按请求自动解析：直连根路径或经 /{serviceId}/ 前缀"));
	}
}
