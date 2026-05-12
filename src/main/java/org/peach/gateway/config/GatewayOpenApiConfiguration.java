package org.peach.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * 网关自身 OpenAPI：固定两条 {@link Server}；默认（列表首项）为带服务前缀，便于经 {@code /{serviceId}/} 访问时「Try it out」正确。
 * <p>
 * 当前依赖的 swagger-core 中 {@link Info} 无 {@code externalDocs} 字段，故仅在 {@link OpenAPI} 根级设置 {@code externalDocs}；
 * 部分 Swagger UI 版本对根级 {@code externalDocs} 展示不完整，因此在 {@code info.description} 中增加 Markdown 链接作为可点击兜底。
 * </p>
 *
 * @author leiyangjun
 */
@Configuration
public class GatewayOpenApiConfiguration {

	/** 注册网关聚合 OpenAPI 定义（含 Servers 与文档门户外链）。 */
	@Bean
	public OpenAPI gatewayOpenApi(@Value("${spring.application.name}") String applicationName) {
		String prefix = "/" + applicationName;
		ExternalDocumentation portal = new ExternalDocumentation()
				.description("回到网关 API 文档门户")
				.url("/index.html");

		Info info = new Info().title("peach-gateway").version("0.0.1-SNAPSHOT")
				.description("Peach API 网关自身 REST 接口说明。"
						+ " 经网关访问其它微服务 Swagger：`/{serviceId}/swagger-ui.html`。"
						+ " Servers 列表首项（默认）为 `" + prefix + "`；根路径直连时请改选 `/`。"
						+ "\n\n[回到网关 API 文档门户](/index.html)");

		return new OpenAPI()
				.info(info)
				.externalDocs(portal)
				.addServersItem(new Server().url(prefix).description("经动态路由 /" + applicationName + "/ 访问本网关（默认）"))
				.addServersItem(new Server().url("/").description("按请求自动解析：直连根路径或经 /{serviceId}/ 前缀"));
	}
}
