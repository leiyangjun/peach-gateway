package org.peach.gateway.support.message;

/**
 * 网关侧客户端类文案与末段语义参考；完整 11 位 {@code code} 见 {@link org.peach.gateway.support.code.GatewayApiResultCodeComposer}。
 * 网关无下游业务域，<strong>不做</strong>业务微服务常见的末四位「区间码」合法性校验（如按 4100–4999 等分段约束）。
 *
 * @author leiyangjun
 */
public enum Message400 implements MessageCode {
	/** 资源不存在 */
	GATEWAY_NOT_FOUND(4001, "资源不存在"),
	/** 未授权 / 令牌相关 */
	GATEWAY_UNAUTHORIZED(4002, "令牌无效或已过期"),
	/** 访问被拒绝 */
	GATEWAY_FORBIDDEN(4002, "访问被拒绝"),
	/** 请求无法处理 */
	GATEWAY_BAD_REQUEST(4003, "请求无法处理"),
	/** Authorization 头缺失或格式错误 */
	GATEWAY_AUTH_HEADER_MISSING(4002, "缺少或格式错误的 Authorization Bearer 令牌"),
	/** Bearer 值为空 */
	GATEWAY_AUTH_BEARER_EMPTY(4002, "Bearer 令牌为空"),
	/** JWT 校验或解析失败 */
	GATEWAY_AUTH_JWT_INVALID(4002, "令牌无效或已过期"),
	/** JWT 密钥或算法配置错误 */
	GATEWAY_AUTH_JWT_CONFIG(4002, "JWT 配置错误");

	private final int code;
	private final String msg;

	Message400(int code, String msg) {
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
