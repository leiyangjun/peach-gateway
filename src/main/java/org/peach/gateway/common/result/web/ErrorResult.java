package org.peach.gateway.common.result.web;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.peach.gateway.common.bootstrap.cache.ModuleCodeCache;
import org.peach.gateway.common.result.message.MessageCode;
import org.peach.gateway.common.result.message.MessageError;
import org.springframework.http.HttpStatus;

/**
 * 网关隔离层与过滤器错误体：仅 {@code code}、{@code msg}，无 {@code data}。
 * <p>
 * {@code code} 为 11 位字符串：{@link org.peach.gateway.common.bootstrap.cache.ModuleCodeCache#get()} 四位模块前缀 + 三位 HTTP
 * 状态数字 + {@link MessageCode} / {@link MessageError} 的 {@code code()} 四位末段（见各 {@code static} 工厂方法内拼接式）。
 * </p>
 *
 * @author leiyangjun
 */
public class ErrorResult {

	private final String code;
	private String msg;

	private ErrorResult(String code, String msg) {
		this.code = code;
		this.msg = msg;
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

	/**
	 * 仅用于日志等场景的简要文本输出；与 {@code ResponseEntity<ErrorResult>} 的 JSON 响应体无关（响应由
	 * Jackson 序列化属性）。
	 */
	@Override
	public String toString() {
		return "ErrorResult[code=" + code + ", msg=" + msg + "]";
	}

	/**
	 * 构造 {@code 模块前缀 + 400 + messageCode} 的校验类错误体；{@code msg} 为 {@code messageCode.msg()}。
	 */
	public static ErrorResult validWarn(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.get() + HttpStatus.BAD_REQUEST.value() + messageCode.code(),
				messageCode.msg());
	}

	/** 同上，支持 {@code %s} 占位与附加片段拼接。 */
	public static ErrorResult validWarn(MessageCode messageCode, String validMsg) {
		String template = messageCode.msg();
		String msg;
		if (template != null && template.contains("%s")) {
			String slot = StringUtils.isNotBlank(validMsg) ? validMsg : "—";
			msg = String.format(template, slot);
		} else {
			msg = Objects.toString(template, "") + Objects.toString(validMsg, "");
		}
		return new ErrorResult(ModuleCodeCache.get() + HttpStatus.BAD_REQUEST.value() + messageCode.code(), msg);
	}

	/** 基于 {@link MessageError} 的 HTTP 状态值与末段四位拼接 {@code code}；{@code msg} 取自枚举（无附加片段）。 */
	public static ErrorResult httpStatus(MessageError statusCode) {
		return httpStatus(statusCode, null);
	}

	/** 同上，第二参数参与 {@code %s} 或末尾拼接。 */
	public static ErrorResult httpStatus(MessageError statusCode, String errorMsg) {
		String template = statusCode.msg();
		String msg;
		if (template != null && template.contains("%s")) {
			String slot = StringUtils.isNotBlank(errorMsg) ? errorMsg : "—";
			msg = String.format(template, slot);
		} else {
			msg = Objects.toString(template, "") + Objects.toString(errorMsg, "");
		}
		return new ErrorResult(ModuleCodeCache.get() + statusCode.httpStatus().value() + statusCode.code(), msg);
	}

	/** {@code 模块前缀 + 500 + messageCode}。 */
	public static ErrorResult error(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.get() + HttpStatus.INTERNAL_SERVER_ERROR.value() + messageCode.code(),
				messageCode.msg());
	}

	/** {@code 模块前缀 + 401 + messageCode}，供认证失败场景。 */
	public static ErrorResult unauthorized(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.get() + HttpStatus.UNAUTHORIZED.value() + messageCode.code(),
				messageCode.msg());
	}

	/** {@code 模块前缀 + 403 + messageCode}。 */
	public static ErrorResult forbidden(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.get() + HttpStatus.FORBIDDEN.value() + messageCode.code(),
				messageCode.msg());
	}

	/** {@code 模块前缀 + 404 + messageCode}。 */
	public static ErrorResult notFound(MessageCode messageCode) {
		return new ErrorResult(ModuleCodeCache.get() + HttpStatus.NOT_FOUND.value() + messageCode.code(),
				messageCode.msg());
	}
}
