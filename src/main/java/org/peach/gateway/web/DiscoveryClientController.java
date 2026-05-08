package org.peach.gateway.web;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 返回注册中心中的全部服务 id 与说明（Swagger URL 由前端拼装），不做任何服务排除（用于网关API文档入口门户HTML页面）。
 */
@RestController
@ConditionalOnProperty(prefix = "peach.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DiscoveryClientController {

	/** 与本地配置、Nacos 元数据键一致：{@code server.description} */
	private static final String META_DESCRIPTION = "server.description";

	/** 兼容中间版本 starter 写入的 {@code peach.description} */
	private static final String META_DESCRIPTION_COMPAT_PEACH = "peach.description";

	/** 兼容更早的 {@code peach.service-description} */
	private static final String META_DESCRIPTION_LEGACY = "peach.service-description";

	private static final boolean LOWER_CASE_SERVICE_ID = true;

	private final DiscoveryClient discoveryClient;

	public DiscoveryClientController(DiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	@GetMapping(path = "/peach-doc-portal/services", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<SwaggerPortalServiceRow> listServices() {
		return discoveryClient.getServices()
			.stream()
			.filter(StringUtils::hasText)
			.map(raw -> {
				String id = normId(raw);
				return new SwaggerPortalServiceRow(id, resolveDescription(raw, id));
			})
			.sorted(Comparator.comparing(SwaggerPortalServiceRow::serviceId, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	private String normId(String id) {
		return LOWER_CASE_SERVICE_ID ? id.toLowerCase(Locale.ROOT) : id;
	}

	private String resolveDescription(String rawServiceId, String normalizedId) {
		for (String key : List.of(rawServiceId, normalizedId)) {
			if (!StringUtils.hasText(key)) {
				continue;
			}
			String d = firstDescription(discoveryClient.getInstances(key.trim()));
			if (StringUtils.hasText(d)) {
				return d;
			}
		}
		return "";
	}

	private static String firstDescription(List<ServiceInstance> instances) {
		if (instances == null || instances.isEmpty()) {
			return "";
		}
		for (ServiceInstance si : instances) {
			if (si.getMetadata() == null) {
				continue;
			}
			Map<String, String> meta = si.getMetadata();
			String v = meta.get(META_DESCRIPTION);
			if (StringUtils.hasText(v)) {
				return v.trim();
			}
			v = meta.get(META_DESCRIPTION_COMPAT_PEACH);
			if (StringUtils.hasText(v)) {
				return v.trim();
			}
			v = meta.get(META_DESCRIPTION_LEGACY);
			if (StringUtils.hasText(v)) {
				return v.trim();
			}
		}
		return "";
	}

	public record SwaggerPortalServiceRow(String serviceId, String description) {
	}
}
