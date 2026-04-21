package org.peach.gateway.route.store;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.peach.gateway.config.PeachRedisRouteProperties;
import org.peach.gateway.route.model.PeachRedisRoute;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Redis 自定义路由的持久化与刷新（单 Key 存 JSON 数组）；供 {@link org.peach.gateway.route.RedisRouteDefinitionRepository} 与管理端 CRUD 共用。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "peach.gateway.redis-routes", name = "enabled", havingValue = "true")
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
public class PeachRedisRouteStore {

	private static final TypeReference<List<PeachRedisRoute>> LIST_TYPE = new TypeReference<>() {
	};

	private final ReactiveStringRedisTemplate redis;
	private final PeachRedisRouteProperties props;
	private final ObjectMapper mapper;
	private final ApplicationEventPublisher publisher;

	public PeachRedisRouteStore(ReactiveStringRedisTemplate redis, PeachRedisRouteProperties props, ObjectMapper mapper,
			ApplicationEventPublisher publisher) {
		this.redis = redis;
		this.props = props;
		this.mapper = mapper;
		this.publisher = publisher;
	}

	private ReactiveValueOperations<String, String> ops() {
		return redis.opsForValue();
	}

	private String key() {
		return props.getRedisKey();
	}

	/**
	 * 加载全部自定义路由。
	 */
	public Mono<List<PeachRedisRoute>> loadAll() {
		return ops().get(key()).defaultIfEmpty("[]").map(this::parseList);
	}

	/**
	 * 按 routeId 查询；不存在则返回 empty Mono。
	 */
	public Mono<PeachRedisRoute> findByRouteId(String routeId) {
		return loadAll().flatMap(list -> Mono.justOrEmpty(list.stream().filter(r -> routeId.equals(r.getRouteId())).findFirst()));
	}

	/**
	 * 新增：routeId 已存在则 409。
	 */
	public Mono<PeachRedisRoute> create(PeachRedisRoute route) {
		return loadAll().flatMap(list -> {
			boolean exists = list.stream().anyMatch(r -> route.getRouteId().equals(r.getRouteId()));
			if (exists) {
				return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "routeId 已存在: " + route.getRouteId()));
			}
			List<PeachRedisRoute> next = new ArrayList<>(list);
			next.add(route);
			return persist(next).thenReturn(route);
		});
	}

	/**
	 * 全量更新：routeId 不存在则 404。
	 */
	public Mono<PeachRedisRoute> update(PeachRedisRoute route) {
		return loadAll().flatMap(list -> {
			boolean exists = list.stream().anyMatch(r -> route.getRouteId().equals(r.getRouteId()));
			if (!exists) {
				return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "routeId 不存在: " + route.getRouteId()));
			}
			List<PeachRedisRoute> next = new ArrayList<>(list.stream().filter(r -> !r.getRouteId().equals(route.getRouteId())).toList());
			next.add(route);
			return persist(next).thenReturn(route);
		});
	}

	/**
	 * 删除：不存在则 404。
	 */
	public Mono<Void> deleteStrict(String routeId) {
		return loadAll().flatMap(list -> {
			List<PeachRedisRoute> next = list.stream().filter(r -> !r.getRouteId().equals(routeId)).toList();
			if (next.size() == list.size()) {
				return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "routeId 不存在: " + routeId));
			}
			return persist(next);
		});
	}

	/**
	 * 与 Spring {@code RouteDefinitionRepository#save} 语义一致：按 routeId 覆盖或追加。
	 */
	public Mono<Void> upsert(PeachRedisRoute peach) {
		return loadAll().flatMap(list -> {
			List<PeachRedisRoute> next = new ArrayList<>(list.stream().filter(r -> !r.getRouteId().equals(peach.getRouteId())).toList());
			next.add(peach);
			return persist(next);
		});
	}

	/**
	 * 与 Spring {@code RouteDefinitionRepository#delete} 语义一致：不存在则静默成功。
	 */
	public Mono<Void> deleteLenient(String routeId) {
		return loadAll().flatMap(list -> {
			List<PeachRedisRoute> next = list.stream().filter(r -> !r.getRouteId().equals(routeId)).toList();
			if (next.size() == list.size()) {
				return Mono.<Void>empty();
			}
			return persist(next);
		});
	}

	private List<PeachRedisRoute> parseList(String json) {
		try {
			List<PeachRedisRoute> list = mapper.readValue(json, LIST_TYPE);
			return list != null ? list : new ArrayList<>();
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException("Redis 自定义路由 JSON 解析失败: " + e.getOriginalMessage(), e);
		}
	}

	private Mono<Void> persist(List<PeachRedisRoute> routes) {
		try {
			String out = mapper.writeValueAsString(routes);
			return ops().set(key(), out).doOnSuccess(v -> publisher.publishEvent(new RefreshRoutesEvent(this))).then();
		}
		catch (JsonProcessingException e) {
			return Mono.error(e);
		}
	}
}
