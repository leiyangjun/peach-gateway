package org.peach.gateway.admin.web;

import jakarta.validation.Valid;
import org.peach.gateway.route.model.PeachRedisRoute;
import org.peach.gateway.route.store.PeachRedisRouteStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Redis 自定义路由的 HTTP CRUD（可选开启 {@code peach.gateway.admin.security.api-key-enabled}，仅作用于本路径前缀）。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
@RestController
@RequestMapping("${peach.gateway.admin.redis-routes-path:/admin/gateway/redis-routes}")
@ConditionalOnBean(PeachRedisRouteStore.class)
public class PeachRedisRouteAdminController {

	private final PeachRedisRouteStore store;

	public PeachRedisRouteAdminController(PeachRedisRouteStore store) {
		this.store = store;
	}

	/**
	 * 列出全部自定义路由。
	 */
	@GetMapping
	public Mono<java.util.List<PeachRedisRoute>> list() {
		return store.loadAll();
	}

	/**
	 * 查询单条。
	 */
	@GetMapping("/{routeId}")
	public Mono<ResponseEntity<?>> get(@PathVariable String routeId) {
		return store.findByRouteId(routeId)
			.<ResponseEntity<?>>map(ResponseEntity::ok)
			.switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
	}

	/**
	 * 新增；routeId 冲突返回 409。
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<PeachRedisRoute> create(@Valid @RequestBody PeachRedisRoute body) {
		return store.create(body);
	}

	/**
	 * 全量更新指定 routeId；路径与 body.routeId 须一致。
	 */
	@PutMapping("/{routeId}")
	public Mono<PeachRedisRoute> update(@PathVariable String routeId, @Valid @RequestBody PeachRedisRoute body) {
		if (!routeId.equals(body.getRouteId())) {
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "路径中的 routeId 与请求体 routeId 不一致"));
		}
		return store.update(body);
	}

	/**
	 * 删除。
	 */
	@DeleteMapping("/{routeId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public Mono<Void> delete(@PathVariable String routeId) {
		return store.deleteStrict(routeId);
	}
}
