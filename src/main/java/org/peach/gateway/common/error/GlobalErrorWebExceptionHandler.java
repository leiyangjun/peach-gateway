package org.peach.gateway.common.error;

import java.nio.charset.StandardCharsets;
import org.peach.gateway.common.result.message.MessageError;
import org.peach.gateway.common.result.web.ErrorResult;
import org.peach.gateway.common.util.JSONUtil;
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
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 实现 Spring WebFlux {@link ErrorWebExceptionHandler}：在响应未提交时，将异常转为 JSON {@link ErrorResult}（仅
 * {@code code}、{@code msg}），并设置与异常类型对应的 HTTP 状态。
 * <p>
 * 典型场景包括网关框架层异常（如未匹配路由、超时 {@link TimeoutException}、 {@link ResponseStatusException} 等）；已匹配路由并由下游返回的响应体不在此处理。
 * </p>
 * <p>
 * {@code code} 由 {@link org.peach.gateway.common.bootstrap.cache.ModuleCodeCache#get()} 四位模块前缀、HTTP 状态三位数字与
 * {@link MessageError} 末四位拼接而成（见 {@link ErrorResult#httpStatus(MessageError, String)}）。
 * </p>
 *
 * @author leiyangjun
 */
@Component
@Order(-2)
@Slf4j
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

	/**
	 * 若响应已提交则原样传递异常；否则设置状态码与 {@code application/json}，写入 {@link ErrorResult#httpStatus(MessageError, String)}
	 * 序列化后的字节并结束响应。
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) {
			return Mono.error(ex);
		}
		int statusValue = resolveHttpStatusValue(ex);
		String path = exchange.getRequest().getURI().getRawPath();
		response.setStatusCode(HttpStatusCode.valueOf(statusValue));
		response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
		ErrorResult body = ErrorResult.httpStatus(MessageError.formStatus(statusValue), null);
		log(exchange, HttpStatusCode.valueOf(statusValue), path, ex);
		byte[] bytes = JSONUtil.toJsonBytes(body);
		DataBuffer buffer = response.bufferFactory().wrap(bytes);
		return response.writeWith(Mono.just(buffer));
	}

	/**
	 * 自异常链解析 HTTP 状态：优先 {@link ResponseStatusException#getStatusCode()} 的数值；遇 {@link TimeoutException} 为 504；否则为 500。供
	 * {@link MessageError#formStatus(int)} 选择文案与末四位。
	 */
	private static int resolveHttpStatusValue(Throwable ex) {
		Throwable t = ex;
		while (t != null) {
			if (t instanceof ResponseStatusException rse) {
				HttpStatusCode code = rse.getStatusCode();
				return code.value();
			}
			if (t instanceof TimeoutException) {
				return HttpStatus.GATEWAY_TIMEOUT.value();
			}
			t = t.getCause();
		}
		return HttpStatus.INTERNAL_SERVER_ERROR.value();
	}

	private static void log(ServerWebExchange exchange, HttpStatusCode status, String path, Throwable ex) {
		String method = exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "?";
		if (status.is5xxServerError()) {
			log.error("[网关错误] {} {} http={} msg={}", method, path, status.value(), ex.getMessage());
		} else if (status.value() == HttpStatus.NOT_FOUND.value()) {
			log.debug("[网关404] {} {} msg={}", method, path, ex.getMessage());
		} else {
			log.warn("[网关{}] {} {} msg={}", status.value(), method, path, ex.getMessage());
		}
	}
}
