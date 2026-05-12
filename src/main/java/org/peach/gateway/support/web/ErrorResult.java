package org.peach.gateway.support.web;

import org.peach.gateway.support.ModuleCodeCache;
import org.peach.gateway.support.code.GatewayApiResultCodeComposer;
import org.springframework.http.HttpStatus;

/**
 * 网关隔离层与过滤器错误体：仅 {@code code}、{@code msg}，无 {@code data}。
 * <p>
 * {@code code} 为 11 位网关码，规则见 {@link GatewayApiResultCodeComposer}（模块四位 + HTTP 三位 + 末四位提示）。
 * </p>
 *
 * @author leiyangjun
 */
public class ErrorResult {

	private final String code;

	private final String msg;

	public ErrorResult(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	/**
	 * 按当前模块码与 HTTP 状态拼装 {@code code}，与 {@link GatewayApiResultCodeComposer} 一致。
	 */
	public static ErrorResult forHttpStatus(HttpStatus status, String message) {
		String module = ModuleCodeCache.requireModuleCode();
		String full = GatewayApiResultCodeComposer.fullCodeForHttpStatus(module, status);
		return new ErrorResult(full, message);
	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}
}
