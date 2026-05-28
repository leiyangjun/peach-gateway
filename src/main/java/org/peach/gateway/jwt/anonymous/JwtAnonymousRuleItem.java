package org.peach.gateway.jwt.anonymous;

/**
 * 单条匿名规则。
 */
public class JwtAnonymousRuleItem {

	private String method;

	private String finalPath;

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
