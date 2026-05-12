package org.peach.gateway.support.message;

/**
 * 网关统一消息码：参与 {@code code} 末段数值与对外 {@code msg}。
 *
 * @author leiyangjun
 */
public interface MessageCode {

	int code();

	String msg();
}
