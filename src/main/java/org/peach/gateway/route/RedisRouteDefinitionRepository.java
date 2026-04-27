package org.peach.gateway.route;

import org.peach.gateway.route.convert.PeachRedisRouteConverter;
import org.peach.gateway.route.store.PeachRedisRouteStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 从 Redis 读取/维护自定义路由：Redis 中仅存放 {@link org.peach.gateway.route.model.PeachRedisRoute} JSON 数组，运行时转换为 {@link RouteDefinition}。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun — 持久化逻辑委托 {@link PeachRedisRouteStore}，与 HTTP CRUD 共用数据源。
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "peach.gateway.redis-routes", name = "enabled", havingValue = "true")
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {

	private final PeachRedisRouteStore store;

	public RedisRouteDefinitionRepository(PeachRedisRouteStore store) {
		this.store = store;
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return store.loadAll()
			.flatMapMany(Flux::fromIterable)
			.map(PeachRedisRouteConverter::toRouteDefinition)
			.onErrorResume(e -> Flux.empty());
	}

	@Override
	public Mono<Void> save(Mono<RouteDefinition> routeMono) {
		return routeMono.map(PeachRedisRouteConverter::fromRouteDefinition).flatMap(store::upsert);
	}

	@Override
	public Mono<Void> delete(Mono<String> routeIdMono) {
		return routeIdMono.flatMap(store::deleteLenient);
	}
}
