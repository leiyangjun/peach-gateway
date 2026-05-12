package org.peach.gateway.support.message;

/**
 * 成功类消息码（末段 2001）。
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
