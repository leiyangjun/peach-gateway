package org.peach.gateway.common.result.message;

/**
 * 网关侧 4xx 语义类消息末段与文案；通过 {@link org.peach.gateway.common.result.web.ErrorResult#validWarn(MessageCode)}、
 * {@link org.peach.gateway.common.result.web.ErrorResult#unauthorized(MessageCode)} 等与模块前缀及 HTTP 状态数字拼接为完整
 * {@code code}。网关不对业务微服务式「末四位区间」做额外合法性校验。
 *
 * @author leiyangjun
 */
public enum Message400 implements MessageCode {
	/** 资源不存在 */
	GATEWAY_NOT_FOUND(4001, "资源不存在"),
	/** 未授权 / 令牌相关 */
	GATEWAY_UNAUTHORIZED(4002, "令牌无效或已过期"),
	/** 访问被拒绝 */
	GATEWAY_FORBIDDEN(4003, "访问被拒绝"),
	/** 请求无法处理 */
	GATEWAY_BAD_REQUEST(4004, "请求无法处理"),
	/** Authorization 头缺失或格式错误 */
	GATEWAY_AUTH_HEADER_MISSING(4005, "缺少或格式错误的 Authorization Bearer 令牌"),
	/** Bearer 值为空 */
	GATEWAY_AUTH_BEARER_EMPTY(4006, "Bearer 令牌为空"),
	/** JWT 校验或解析失败 */
	GATEWAY_AUTH_JWT_INVALID(4007, "令牌无效或已过期"),
	/** JWT 密钥或算法配置错误 */
	GATEWAY_AUTH_JWT_CONFIG(4008, "JWT 配置错误");

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
