package org.peach.gateway.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.peach.gateway.route.store.PeachRedisRouteStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * 管理端接口异常响应（JSON）。
 * <p>
 * 创作日期：2026-04-20，作者：leiyangjun
 * </p>
 */
@RestControllerAdvice
@ConditionalOnBean(PeachRedisRouteStore.class)
public class PeachGatewayWebExceptionAdvice {

	@ExceptionHandler(ResponseStatusException.class)
	public Mono<ResponseEntity<Map<String, Object>>> handleStatus(ResponseStatusException ex) {
		Map<String, Object> body = new HashMap<>(2);
		body.put("message", ex.getReason());
		body.put("status", ex.getStatusCode().value());
		return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(body));
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public Mono<ResponseEntity<Map<String, Object>>> handleBind(WebExchangeBindException ex) {
		String msg = ex.getFieldErrors().stream()
			.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
			.collect(Collectors.joining("; "));
		Map<String, Object> body = new HashMap<>(2);
		body.put("message", msg);
		body.put("status", HttpStatus.BAD_REQUEST.value());
		return Mono.just(ResponseEntity.badRequest().body(body));
	}
}
