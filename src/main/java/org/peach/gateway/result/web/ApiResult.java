package org.peach.gateway.result.web;

import org.peach.gateway.config.ModuleCodeCache;
import org.peach.gateway.result.message.Message200;
import org.springframework.http.HttpStatus;

/**
 * 网关自身 REST 成功返回体：{@code code}、{@code msg}、{@code data}。
 * <p>
 * 成功时 {@code code} 为 {@link org.peach.gateway.config.ModuleCodeCache#get()} + {@code 200} +
 * {@link Message200#OK} 的 {@code code()} 四位末段拼接结果。
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

	/** 使用 {@link Message200#OK} 拼装 {@code code}/{@code msg} 并携带 {@code data}。 */
	public static <T> ApiResult<T> ok(T data) {
		Message200 ok = Message200.OK;
		return new ApiResult<>(ModuleCodeCache.get() + HttpStatus.OK.value() + ok.code(), ok.msg(), data);
	}

	/** {@code data} 为 {@code null} 的成功响应。 */
	public static ApiResult<Void> ok() {
		return ok(null);
	}
}
