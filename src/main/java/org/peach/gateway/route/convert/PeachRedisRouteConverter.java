package org.peach.gateway.route.convert;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.peach.gateway.route.model.PeachRedisRoute;
import org.peach.gateway.route.model.PeachRouteTargetType;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Peach 自定义路由模型与 Spring Cloud Gateway {@link RouteDefinition} 互转（Redis 只存前者）。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
public final class PeachRedisRouteConverter {

	private PeachRedisRouteConverter() {
	}

	/**
	 * 将 Peach 模型转为网关运行时定义。
	 */
	public static RouteDefinition toRouteDefinition(PeachRedisRoute route) {
		if (route == null || !StringUtils.hasText(route.getRouteId())) {
			throw new IllegalArgumentException("PeachRedisRoute.routeId 不能为空");
		}
		if (route.getUriType() == null) {
			throw new IllegalArgumentException("PeachRedisRoute.uriType 不能为空: " + route.getRouteId());
		}
		if (!StringUtils.hasText(route.getTarget())) {
			throw new IllegalArgumentException("PeachRedisRoute.target 不能为空: " + route.getRouteId());
		}
		if (!StringUtils.hasText(route.getPathPattern())) {
			throw new IllegalArgumentException("PeachRedisRoute.pathPattern 不能为空: " + route.getRouteId());
		}

		RouteDefinition def = new RouteDefinition();
		def.setId(route.getRouteId());
		def.setOrder(route.getOrder() != null ? route.getOrder() : 0);
		def.setUri(buildUri(route));

		List<PredicateDefinition> predicates = new ArrayList<>();
		predicates.add(pathPredicate(route.getPathPattern()));
		if (!CollectionUtils.isEmpty(route.getHttpMethods())) {
			String methods = route.getHttpMethods().stream().map(String::trim).filter(StringUtils::hasText)
				.collect(Collectors.joining(","));
			if (StringUtils.hasText(methods)) {
				predicates.add(methodPredicate(methods));
			}
		}
		def.setPredicates(predicates);

		List<FilterDefinition> filters = new ArrayList<>();
		if (route.getStripPrefixParts() != null && route.getStripPrefixParts() > 0) {
			filters.add(stripPrefixFilter(route.getStripPrefixParts()));
		}
		def.setFilters(filters);
		return def;
	}

	private static URI buildUri(PeachRedisRoute route) {
		return switch (route.getUriType()) {
			case REGISTERED_SERVICE -> URI.create("lb://" + route.getTarget().trim());
			case EXPLICIT_URI -> URI.create(route.getTarget().trim());
		};
	}

	private static PredicateDefinition pathPredicate(String pattern) {
		Map<String, String> args = new LinkedHashMap<>();
		args.put("pattern", pattern);
		PredicateDefinition p = new PredicateDefinition();
		p.setName("Path");
		p.setArgs(args);
		return p;
	}

	/**
	 * 与 {@code MethodRoutePredicateFactory} 约定一致：参数名 {@code methods}，逗号分隔。
	 */
	private static PredicateDefinition methodPredicate(String methodsCsv) {
		Map<String, String> args = new LinkedHashMap<>();
		args.put("methods", methodsCsv);
		PredicateDefinition p = new PredicateDefinition();
		p.setName("Method");
		p.setArgs(args);
		return p;
	}

	private static FilterDefinition stripPrefixFilter(int parts) {
		Map<String, String> args = new LinkedHashMap<>();
		args.put("parts", String.valueOf(parts));
		FilterDefinition f = new FilterDefinition();
		f.setName("StripPrefix");
		f.setArgs(args);
		return f;
	}

	/**
	 * 将 Spring {@link RouteDefinition} 尽量还原为 Peach 模型（供 {@code RouteDefinitionRepository#save} 等场景；复杂谓词/过滤器可能无法完整还原）。
	 */
	public static PeachRedisRoute fromRouteDefinition(RouteDefinition def) {
		if (def == null || !StringUtils.hasText(def.getId())) {
			throw new IllegalArgumentException("RouteDefinition.id 不能为空");
		}
		PeachRedisRoute r = new PeachRedisRoute();
		r.setRouteId(def.getId());
		r.setOrder(def.getOrder());
		URI uri = def.getUri();
		if (uri == null) {
			throw new IllegalArgumentException("RouteDefinition.uri 不能为空: " + def.getId());
		}
		String scheme = uri.getScheme();
		if ("lb".equalsIgnoreCase(scheme)) {
			r.setUriType(PeachRouteTargetType.REGISTERED_SERVICE);
			r.setTarget(uri.getHost() != null ? uri.getHost() : uri.getAuthority());
		}
		else {
			r.setUriType(PeachRouteTargetType.EXPLICIT_URI);
			r.setTarget(uri.toString());
		}
		for (PredicateDefinition p : def.getPredicates()) {
			if ("Path".equalsIgnoreCase(p.getName())) {
				r.setPathPattern(p.getArgs().getOrDefault("pattern", p.getArgs().values().stream().findFirst().orElse(null)));
			}
			if ("Method".equalsIgnoreCase(p.getName())) {
				String raw = p.getArgs().getOrDefault("methods",
						p.getArgs().values().stream().findFirst().orElse(""));
				if (StringUtils.hasText(raw)) {
					r.setHttpMethods(List.of(raw.split(",")));
				}
			}
		}
		for (FilterDefinition f : def.getFilters()) {
			if ("StripPrefix".equalsIgnoreCase(f.getName())) {
				String parts = f.getArgs().getOrDefault("parts", f.getArgs().values().stream().findFirst().orElse("0"));
				try {
					r.setStripPrefixParts(Integer.parseInt(parts));
				}
				catch (NumberFormatException ignored) {
					r.setStripPrefixParts(0);
				}
			}
		}
		if (!StringUtils.hasText(r.getPathPattern())) {
			throw new IllegalArgumentException("无法从 RouteDefinition 解析 Path 断言: " + def.getId());
		}
		return r;
	}
}
