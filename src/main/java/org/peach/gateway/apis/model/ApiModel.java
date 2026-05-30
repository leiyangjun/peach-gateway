package org.peach.gateway.apis.model;

/**
 * 单条匿名规则。
 */
public class ApiModel {

	private String method;

	private String finalPath;

	/** 访问类型：1=免登录 2=需登录免权限；缺省或未知值不生效 */
	private Short accessType;

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

	public Short getAccessType() {
		return accessType;
	}

	public void setAccessType(Short accessType) {
		this.accessType = accessType;
	}
}
