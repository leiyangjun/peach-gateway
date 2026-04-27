package org.peach.gateway.doc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

import org.springframework.boot.context.event.ApplicationReadyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自注册中心发现微服务并填充 Swagger UI 下拉列表（替代原 Knife4j discover）。
 * <p>
 * 约定：下游暴露 {@code /v3/api-docs}，经网关路径为 {@code /<serviceId>/v3/api-docs}（与 discovery locator + StripPrefix 一致）。
 * </p>
 */
@Configuration
@ConditionalOnClass(DiscoveryClient.class)
@ConditionalOnProperty(prefix = "springdoc.gateway.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringdocGatewayDiscoveryConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SpringdocGatewayDiscoveryConfiguration.class);

	private final DiscoveryClient discoveryClient;

	private final SwaggerUiConfigProperties swaggerUiConfigProperties;

	private final String applicationName;

	private final List<String> excludedServiceIds;

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	public SpringdocGatewayDiscoveryConfiguration(DiscoveryClient discoveryClient,
			SwaggerUiConfigProperties swaggerUiConfigProperties,
			@Value("${spring.application.name}") String applicationName,
			@Value("${springdoc.gateway.discovery.excluded-services:peach-gateway}") String excludedRaw) {
		this.discoveryClient = discoveryClient;
		this.swaggerUiConfigProperties = swaggerUiConfigProperties;
		this.applicationName = applicationName;
		this.excludedServiceIds = parseExcluded(excludedRaw);
	}

	private static List<String> parseExcluded(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of("peach-gateway");
		}
		return List.of(raw.split(","));
	}

	@EventListener(ApplicationReadyEvent.class)
	public void registerSwaggerUiUrls() {
		if (!initialized.compareAndSet(false, true)) {
			return;
		}
		Set<SwaggerUrl> urls = new LinkedHashSet<>();
		urls.add(new SwaggerUrl("peach-gateway", "/v3/api-docs", "API 网关"));
		for (String serviceId : discoveryClient.getServices()) {
			if (!StringUtils.hasText(serviceId)) {
				continue;
			}
			if (isExcluded(serviceId)) {
				continue;
			}
			if (serviceId.equalsIgnoreCase(applicationName)) {
				continue;
			}
			String docPath = "/" + serviceId + "/v3/api-docs";
			urls.add(new SwaggerUrl(serviceId, docPath, serviceId));
		}
		swaggerUiConfigProperties.setUrls(urls);
		log.info("SpringDoc 聚合：已注册 {} 个 OpenAPI 文档入口（含网关自身）", urls.size());
	}

	private boolean isExcluded(String serviceId) {
		for (String ex : excludedServiceIds) {
			if (StringUtils.hasText(ex) && ex.trim().equalsIgnoreCase(serviceId)) {
				return true;
			}
		}
		return false;
	}
}
