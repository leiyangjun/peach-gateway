package org.peach.gateway.route.discovery;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
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
 * 基于服务发现为<strong>全部</strong>服务生成路由：Path=/{serviceId}/** → lb://{serviceId}，
 * 含 X-Forwarded-Prefix、X-Peach-Gateway-Prefix（供下游 {@code SwaggerOpenApiCustomizer} 识别网关访问）
 * 与 RewritePath（不做任何服务 id 排除）。
 * <p>
 * 若 Reactive 发现客户端 {@link ReactiveDiscoveryClient#getServices()} 在短时间内为空（与 Nacos 缓存/时序有关），
 * 则回退到阻塞式 {@link DiscoveryClient#getServices()}，避免出现「注册中心有服务但网关无任何 discovery 路由 → 404」。
 * </p>
 */
public class DynamicDiscoveryRouteDefinitionLocator implements RouteDefinitionLocator {

	private static final String ROUTE_ID_PREFIX = "discovery";

	private static final boolean LOWER_CASE_SERVICE_ID = true;

	private final ObjectProvider<ReactiveDiscoveryClient> reactiveDiscoveryClient;

	private final ObjectProvider<DiscoveryClient> blockingDiscoveryClient;

	public DynamicDiscoveryRouteDefinitionLocator(ObjectProvider<ReactiveDiscoveryClient> reactiveDiscoveryClient,
			ObjectProvider<DiscoveryClient> blockingDiscoveryClient) {
		this.reactiveDiscoveryClient = reactiveDiscoveryClient;
		this.blockingDiscoveryClient = blockingDiscoveryClient;
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return discoverServiceNames().map(this::normalizeServiceId).filter(StringUtils::hasText).map(this::buildRoute);
	}

	private Flux<String> discoverServiceNames() {
		ReactiveDiscoveryClient reactive = reactiveDiscoveryClient.getIfAvailable();
		DiscoveryClient blocking = blockingDiscoveryClient.getIfAvailable();
		if (reactive != null) {
			return reactive.getServices()
				.collectList()
				.flatMapMany(reactiveList -> {
					/*
					 * Reactive 与阻塞式 Discovery 在短时间内可能不一致（Nacos 缓存/刷新时序）。
					 * 仅信任 Reactive 非空列表会漏路由，而门户 /peach-doc-portal/services 走阻塞式 Client，
					 * 表现为「列表里有服务，点 Swagger 却 404」。合并二者并集再建路由。
					 */
					Set<String> merged = new LinkedHashSet<>();
					if (reactiveList != null) {
						for (String s : reactiveList) {
							if (StringUtils.hasText(s)) {
								merged.add(s);
							}
						}
					}
					if (blocking != null) {
						for (String s : blocking.getServices()) {
							if (StringUtils.hasText(s)) {
								merged.add(s);
							}
						}
					}
					if (merged.isEmpty()) {
						return fromBlocking(blocking);
					}
					return Flux.fromIterable(merged);
				})
				.onErrorResume(ex -> fromBlocking(blocking));
		}
		return fromBlocking(blocking);
	}

	private static Flux<String> fromBlocking(DiscoveryClient blocking) {
		if (blocking == null) {
			return Flux.empty();
		}
		return Mono.fromCallable(blocking::getServices).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable);
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
		/* 备用：个别链路易丢标准 Prefix，与 SwaggerOpenApiCustomizer 配合修正 Swagger Try it out */
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
