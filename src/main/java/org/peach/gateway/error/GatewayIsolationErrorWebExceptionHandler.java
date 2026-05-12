package org.peach.gateway.error;

import java.nio.charset.StandardCharsets;

import org.peach.gateway.support.message.Message400;
import org.peach.gateway.support.message.Message500;
import org.peach.gateway.support.web.ErrorResult;
import org.peach.gateway.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 网关「隔离层」错误：仅处理<strong>未命中下游路由</strong>且<strong>非网关自身 Controller 正常命中</strong>时的框架异常，
 * 以及<strong>网关节点自身</strong>抛出的错误（如超时、连接失败、编码异常等）。
 * <p>
 * 不介入「已匹配动态路由并成功与下游建立转发」时的 HTTP 响应体；该场景由下游原样返回（含下游 4xx/5xx 的 body）。
 * </p>
 * <p>
 * 响应体仅含 {@code code}、{@code msg}：{@code code} 为 11 位网关码（见 {@link org.peach.gateway.support.code.GatewayApiResultCodeComposer}）；404 文案与业务侧「资源不存在」一致。
 * </p>
 */
@Component
@Order(-2)
public class GatewayIsolationErrorWebExceptionHandler implements ErrorWebExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayIsolationErrorWebExceptionHandler.class);

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) {
			return Mono.error(ex);
		}
		HttpStatus status = resolveHttpStatus(ex);
		String path = exchange.getRequest().getURI().getRawPath();
		String msg = defaultMsg(status, ex);
		log(exchange, status, path, msg, ex);

		response.setStatusCode(status);
		response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
		ErrorResult body = ErrorResult.forHttpStatus(status, msg);
		byte[] bytes = JSONUtil.toJsonBytes(body);
		DataBuffer buffer = response.bufferFactory().wrap(bytes);
		return response.writeWith(Mono.just(buffer));
	}

	private static HttpStatus resolveHttpStatus(Throwable ex) {
		Throwable t = ex;
		while (t != null) {
			if (t instanceof ResponseStatusException rse) {
				HttpStatusCode code = rse.getStatusCode();
				if (code instanceof HttpStatus hs) {
					return hs;
				}
				int v = code.value();
				HttpStatus resolved = HttpStatus.resolve(v);
				return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
			}
			if (t instanceof TimeoutException) {
				return HttpStatus.GATEWAY_TIMEOUT;
			}
			t = t.getCause();
		}
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	private static String defaultMsg(HttpStatus status, Throwable ex) {
		if (status == HttpStatus.NOT_FOUND) {
			return Message400.GATEWAY_NOT_FOUND.msg();
		}
		if (status == HttpStatus.GATEWAY_TIMEOUT) {
			return Message500.GATEWAY_TIMEOUT.msg();
		}
		if (status == HttpStatus.BAD_GATEWAY) {
			return Message500.GATEWAY_BAD_GATEWAY.msg();
		}
		if (status == HttpStatus.SERVICE_UNAVAILABLE) {
			return Message500.GATEWAY_SERVICE_UNAVAILABLE.msg();
		}
		if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
			return Message500.GATEWAY_INTERNAL.msg();
		}
		if (status.is4xxClientError()) {
			return ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : Message400.GATEWAY_BAD_REQUEST.msg();
		}
		return Message500.GATEWAY_INTERNAL.msg();
	}

	private static void log(ServerWebExchange exchange, HttpStatus status, String path, String msg, Throwable ex) {
		String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "?";
		if (status.is5xxServerError()) {
			log.error("[网关错误] {} {} http={} msg={}", method, path, status.value(), msg, ex);
		}
		else if (status == HttpStatus.NOT_FOUND) {
			log.warn("[网关404] {} {} msg={}", method, path, msg);
		}
		else {
			log.warn("[网关{}] {} {} msg={}", status.value(), method, path, msg);
		}
	}
}
