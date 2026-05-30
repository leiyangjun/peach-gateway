package org.peach.gateway.permission.model;

/**
 * 角色授权 API 条目：method + finalPath，与 common-service {@code RoleApiItem} JSON 字段一致。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
public class ApiPermModel {

	private String method;

	private String finalPath;

	public ApiPermModel() {
	}

	public ApiPermModel(String method, String finalPath) {
		this.method = method;
		this.finalPath = finalPath;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getFinalPath() {
		return finalPath;
	}

	public void setFinalPath(String finalPath) {
		this.finalPath = finalPath;
	}
}
