package org.peach.gateway.support.web;

import org.peach.gateway.support.ModuleCodeCache;
import org.peach.gateway.support.code.GatewayApiResultCodeComposer;
import org.peach.gateway.support.message.Message200;
import org.springframework.http.HttpStatus;

/**
 * 网关自身 REST API 统一成功返回体：{@code code}、{@code msg}、{@code data}。
 * <p>
 * 成功时 {@code code} 由 {@link GatewayApiResultCodeComposer#compose(String, int, int)} 拼装（模块四位 + HTTP200 三位 + 消息码四位）。
 * </p>
 *
 * @param <T> 数据体类型
 * @author leiyangjun
 */
public class ApiResult<T> {

	private final String code;

	private String msg;

	private final T data;

	private ApiResult(String code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public static <T> ApiResult<T> ok(T data) {
		Message200 ok = Message200.OK;
		String c = GatewayApiResultCodeComposer.compose(ModuleCodeCache.requireModuleCode(), HttpStatus.OK.value(), ok.code());
		return new ApiResult<>(c, ok.msg(), data);
	}

	public static ApiResult<Void> ok() {
		return ok(null);
	}
}
