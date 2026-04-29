package org.peach.gateway.route.discovery;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 * 含 X-Forwarded-Prefix 与 RewritePath（不做任何服务 id 排除）。
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
		ReactiveDiscoveryClient reactive = reactiveDiscoveryClient.getIfAvailable();
		if (reactive != null) {
			return reactive.getServices().map(this::normalizeServiceId).filter(StringUtils::hasText).map(this::buildRoute);
		}
		DiscoveryClient blocking = blockingDiscoveryClient.getIfAvailable();
		if (blocking == null) {
			return Flux.empty();
		}
		return Mono.fromCallable(() -> buildRoutesFromBlocking(blocking)).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable);
	}

	private List<RouteDefinition> buildRoutesFromBlocking(DiscoveryClient blocking) {
		List<RouteDefinition> out = new ArrayList<>();
		for (String rawId : blocking.getServices()) {
			String serviceId = normalizeServiceId(rawId);
			if (!StringUtils.hasText(serviceId)) {
				continue;
			}
			out.add(buildRoute(serviceId));
		}
		return out;
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

		FilterDefinition rewrite = new FilterDefinition();
		rewrite.setName(NameUtils.normalizeFilterFactoryName(RewritePathGatewayFilterFactory.class));
		rewrite.addArg("regexp", "/" + serviceId + "/?(?<remaining>.*)");
		rewrite.addArg("replacement", "/${remaining}");
		def.getFilters().add(rewrite);

		return def;
	}
}
