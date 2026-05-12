package org.peach.gateway.result.message;

/**
 * 成功类消息末段（当前仅 {@link #OK}），供 {@link org.peach.gateway.result.web.ApiResult#ok(Object)} 拼装
 * {@code code}。
 *
 * @author leiyangjun
 */
public enum Message200 implements MessageCode {
	/** 操作成功 */
	OK(2001, "操作成功");

	private final int code;
	private final String msg;

	Message200(int code, String msg) {
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
