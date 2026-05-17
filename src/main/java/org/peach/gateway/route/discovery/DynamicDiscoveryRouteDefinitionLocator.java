package org.peach.gateway.route.discovery;

import java.net.URI;
import java.util.Locale;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 基于 {@link DiscoveryClient#getServices()} 为每个服务 id 生成一条路由定义：
 * 匹配 {@code /{serviceId}/**}，转发至 {@code lb://{serviceId}}，并追加 {@code X-Forwarded-Prefix}、
 * {@code X-Peach-Gateway-Prefix} 与 {@code RewritePath} 去掉 URL 中的服务前缀段。
 *
 * @author leiyangjun
 */
public class DynamicDiscoveryRouteDefinitionLocator implements RouteDefinitionLocator {

	private static final String ROUTE_ID_PREFIX = "discovery";

	private static final boolean LOWER_CASE_SERVICE_ID = true;

	private final DiscoveryClient discoveryClient;

	public DynamicDiscoveryRouteDefinitionLocator(DiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	/** 在弹性线程上拉取服务名并映射为路由定义。 */
	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return serviceIds().map(this::normalizeServiceId).filter(StringUtils::hasText).map(this::buildRoute);
	}

	/** 阻塞式 DiscoveryClient 调用，调度至 boundedElastic 线程池。 */
	private Flux<String> serviceIds() {
		return Mono.fromCallable(() -> discoveryClient.getServices())
				.subscribeOn(Schedulers.boundedElastic())
				.flatMapMany(Flux::fromIterable);
	}

	private String normalizeServiceId(String rawId) {
		if (!StringUtils.hasText(rawId)) {
			return "";
		}
		return LOWER_CASE_SERVICE_ID ? rawId.toLowerCase(Locale.ROOT) : rawId;
	}

	private RouteDefinition buildRoute(String serviceId) {
		RouteDefinition def = new RouteDefinition();
		def.setId(ROUTE_ID_PREFIX + "-" + serviceId);
		def.setUri(URI.create("lb://" + serviceId));

		PredicateDefinition path = new PredicateDefinition();
		path.setName(NameUtils.normalizeRoutePredicateName(PathRoutePredicateFactory.class));
		path.addArg("pattern", "/" + serviceId + "/**");
		def.getPredicates().add(path);

		FilterDefinition fwdPrefix = new FilterDefinition();
		fwdPrefix.setName(NameUtils.normalizeFilterFactoryName(AddRequestHeaderGatewayFilterFactory.class));
		fwdPrefix.addArg("name", "X-Forwarded-Prefix");
		fwdPrefix.addArg("value", "/" + serviceId);
		def.getFilters().add(fwdPrefix);

		FilterDefinition peachPrefix = new FilterDefinition();
		peachPrefix.setName(NameUtils.normalizeFilterFactoryName(AddRequestHeaderGatewayFilterFactory.class));
		peachPrefix.addArg("name", "X-Peach-Gateway-Prefix");
		peachPrefix.addArg("value", "/" + serviceId);
		def.getFilters().add(peachPrefix);

		FilterDefinition rewrite = new FilterDefinition();
		rewrite.setName(NameUtils.normalizeFilterFactoryName(RewritePathGatewayFilterFactory.class));
		rewrite.addArg("regexp", "/" + serviceId + "/?(?<remaining>.*)");
		rewrite.addArg("replacement", "/${remaining}");
		def.getFilters().add(rewrite);

		return def;
	}
}
