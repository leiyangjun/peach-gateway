package org.peach.gateway.common.result.message;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * 与标准 HTTP 状态一一对应的网关侧文案与末四位数值，供 {@link org.peach.gateway.common.result.web.ErrorResult#httpStatus(MessageError, String)} 等拼装
 * {@code code} 使用。
 * <p>
 * 枚举值覆盖常见 4xx/5xx；{@link #formStatus(int)} 对未在表中显式注册的状态码回落为 {@link #INTERNAL_SERVER_ERROR}。
 * </p>
 *
 * @author leiyangjun
 */
public enum MessageError {

	/** 请求无法处理 */
	BAD_REQUEST(HttpStatus.BAD_REQUEST, 4009, "请求无法处理"),
	/** 未授权 */
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 4010, "令牌无效或已过期"),
	/** 需要付款 */
	PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, 4011, "需要付款或当前计费策略不允许访问"),
	/** 禁止访问 */
	FORBIDDEN(HttpStatus.FORBIDDEN, 4012, "访问被拒绝"),
	/** 资源不存在 */
	NOT_FOUND(HttpStatus.NOT_FOUND, 4013, "资源不存在"),
	/** 方法不允许 */
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 4014, "请求方法不被允许"),
	/** 无法协商可接受表示 */
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, 4015, "无法按请求生成可接受的响应内容"),
	/** 需要代理身份验证 */
	PROXY_AUTHENTICATION_REQUIRED(HttpStatus.PROXY_AUTHENTICATION_REQUIRED, 4016, "需要代理服务器身份验证"),
	/** 请求超时 */
	REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, 4017, "请求超时"),
	/** 冲突 */
	CONFLICT(HttpStatus.CONFLICT, 4018, "请求与资源当前状态冲突"),
	/** 资源已不可用 */
	GONE(HttpStatus.GONE, 4019, "资源已被永久删除或不可用"),
	/** 需要 Content-Length */
	LENGTH_REQUIRED(HttpStatus.LENGTH_REQUIRED, 4020, "需要指定有效的 Content-Length"),
	/** 前置条件失败 */
	PRECONDITION_FAILED(HttpStatus.PRECONDITION_FAILED, 4021, "请求前置条件不满足"),
	/** URI 过长 */
	URI_TOO_LONG(HttpStatus.URI_TOO_LONG, 4023, "请求 URI 过长"),
	/** 不支持的媒体类型 */
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 4024, "不支持的媒体类型"),
	/** 范围不满足 */
	REQUESTED_RANGE_NOT_SATISFIABLE(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, 4025, "无法满足请求的字节范围"),
	/** 期望失败 */
	EXPECTATION_FAILED(HttpStatus.EXPECTATION_FAILED, 4026, "Expect 相关头域无法满足"),
	/** 请求被错误定向 */
	MISDIRECTED_REQUEST(HttpStatus.MISDIRECTED_REQUEST, 4028, "请求被定向到无法处理的服务器"),
	/** 资源被锁定 */
	LOCKED(HttpStatus.LOCKED, 4030, "资源被锁定"),
	/** 依赖失败 */
	FAILED_DEPENDENCY(HttpStatus.FAILED_DEPENDENCY, 4031, "依赖的上一步操作失败"),
	/** 过早 */
	TOO_EARLY(HttpStatus.TOO_EARLY, 4032, "服务器不愿在条件未满足时处理该请求"),
	/** 需要升级协议 */
	UPGRADE_REQUIRED(HttpStatus.UPGRADE_REQUIRED, 4033, "需要升级到其它协议"),
	/** 需要先满足条件请求 */
	PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, 4034, "需要先满足条件请求头"),
	/** 请求过于频繁 */
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, 4035, "请求过于频繁，请稍后再试"),
	/** 请求头字段过大 */
	REQUEST_HEADER_FIELDS_TOO_LARGE(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, 4036, "请求头字段过大"),
	/** 因法律原因不可用 */
	UNAVAILABLE_FOR_LEGAL_REASONS(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, 4037, "因法律或政策原因无法提供该资源"),

	/** 系统内部错误 */
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5001, "系统内部错误"),
	/** 未实现 */
	NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, 5002, "服务器不支持实现该请求"),
	/** 网关无法连接下游 */
	BAD_GATEWAY(HttpStatus.BAD_GATEWAY, 5003, "网关无法连接下游或下游提前断开"),
	/** 服务暂不可用 */
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 5004, "网关暂时无法处理请求"),
	/** 网关等待下游超时 */
	GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, 5005, "网关等待下游响应超时"),
	/** HTTP 版本不支持 */
	HTTP_VERSION_NOT_SUPPORTED(HttpStatus.HTTP_VERSION_NOT_SUPPORTED, 5006, "不支持的 HTTP 协议版本"),
	/** 协商变体异常 */
	VARIANT_ALSO_NEGOTIATES(HttpStatus.VARIANT_ALSO_NEGOTIATES, 5007, "服务器内容协商配置异常"),
	/** 存储空间不足 */
	INSUFFICIENT_STORAGE(HttpStatus.INSUFFICIENT_STORAGE, 5008, "存储空间不足"),
	/** 检测到环 */
	LOOP_DETECTED(HttpStatus.LOOP_DETECTED, 5009, "检测到无限循环"),
	/** 需要网络身份验证 */
	NETWORK_AUTHENTICATION_REQUIRED(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, 5012, "需要网络身份验证");

	private final HttpStatus httpStatus;
	private final int code;
	private final String msg;

	private static final Map<Integer, MessageError> CODE_MAP = new HashMap<>();

	static {
		for (MessageError status : MessageError.values()) {
			CODE_MAP.put(status.httpStatus().value(), status);
		}
	}

	/**
	 * 按 HTTP 状态码查表；无精确匹配时返回 {@link MessageError#INTERNAL_SERVER_ERROR}（不返回 {@code null}）。
	 */
	public static MessageError formStatus(int status) {
		MessageError messageError = CODE_MAP.get(status);
		if (messageError == null) {
			messageError = MessageError.INTERNAL_SERVER_ERROR;
		}
		return messageError;
	}

	MessageError(HttpStatus httpStatus, int code, String msg) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.msg = msg;
	}

	public int code() {
		return code;
	}

	public String msg() {
		return msg;
	}

	public HttpStatus httpStatus() {
		return httpStatus;
	}

}
