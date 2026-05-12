package org.peach.gateway.support.code;

import org.springframework.http.HttpStatus;

/**
 * 网关隔离层错误码：{@code 模块四位 + HTTP 状态三位 + 提示四位}（共 11 位）。
 * <p>
 * 与业务服务 {@code ApiResult}「族」用 200/400/401 等语义段不同：此处中间三位为<strong>实际 HTTP 状态码</strong>（如 404、403），
 * 末四位为网关节点内提示号（如 404 未匹配路由/资源用 {@code 4001}）。
 * </p>
 * <p>
 * 网关无下游业务上下文，<strong>不做</strong>业务微服务常见的末四位「区间码」约束或校验（例如按 4100–4999 等分段判定合法性）；
 * 末段仅由本类固定映射及 {@link org.peach.gateway.support.message.Message400}、{@link org.peach.gateway.support.message.Message500} 约定生成。
 * </p>
 */
public final class GatewayApiResultCodeComposer {

	private GatewayApiResultCodeComposer() {
	}

	/**
	 * @param moduleCode 四位服务编码（如 GWAY）
	 * @param httpStatusValue HTTP 状态数值（100–599）
	 * @param msgCode 四位消息码段（0–9999，内部取模规范化，非业务区间校验）
	 */
	public static String compose(String moduleCode, int httpStatusValue, int msgCode) {
		int st = Math.clamp(httpStatusValue, 100, 599);
		int msgSeg = Math.abs(msgCode) % 10000;
		return moduleCode + String.format("%03d", st) + String.format("%04d", msgSeg);
	}

	/**
	 * 按 {@link HttpStatus} 映射网关统一错误码（中间三位与响应 HTTP 状态一致）。
	 */
	public static String fullCodeForHttpStatus(String moduleCode, HttpStatus status) {
		return switch (status) {
			case NOT_FOUND -> compose(moduleCode, 404, 4001);
			case BAD_REQUEST -> compose(moduleCode, 400, 4003);
			case UNAUTHORIZED -> compose(moduleCode, 401, 4002);
			case FORBIDDEN -> compose(moduleCode, 403, 4002);
			case BAD_GATEWAY -> compose(moduleCode, 502, 5021);
			case SERVICE_UNAVAILABLE -> compose(moduleCode, 503, 5031);
			case GATEWAY_TIMEOUT -> compose(moduleCode, 504, 5041);
			case INTERNAL_SERVER_ERROR -> compose(moduleCode, 500, 5001);
			default -> {
				if (status.is4xxClientError()) {
					yield compose(moduleCode, status.value(), 4003);
				}
				if (status.is5xxServerError()) {
					yield compose(moduleCode, status.value(), 5001);
				}
				yield compose(moduleCode, 500, 5001);
			}
		};
	}
}
