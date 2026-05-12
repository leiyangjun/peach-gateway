package org.peach.gateway.support.message;

/**
 * 网关侧转发 / 内部错误类文案与末段语义参考；完整 11 位 {@code code} 见 {@link org.peach.gateway.support.code.GatewayApiResultCodeComposer}。
 * 网关无下游业务域，<strong>不做</strong>业务微服务常见的末四位「区间码」合法性校验。
 *
 * @author leiyangjun
 */
public enum Message500 implements MessageCode {
	/** 系统内部错误 */
	GATEWAY_INTERNAL(5001, "系统内部错误"),
	/** 网关无法连接下游或下游提前断开 */
	GATEWAY_BAD_GATEWAY(5021, "网关无法连接下游或下游提前断开"),
	/** 网关暂时无法处理请求 */
	GATEWAY_SERVICE_UNAVAILABLE(5031, "网关暂时无法处理请求"),
	/** 网关等待下游响应超时 */
	GATEWAY_TIMEOUT(5041, "网关等待下游响应超时"),
	/** JSON 序列化失败时的兜底文案（与 {@code 5005001} 段一致） */
	GATEWAY_JSON_SERIALIZE_FAILED(5001, "序列化错误");

	private final int code;
	private final String msg;

	Message500(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	@Override
	public int code() {
		return code;
	}

	@Override
	public String msg() {
		return msg;
	}
}
