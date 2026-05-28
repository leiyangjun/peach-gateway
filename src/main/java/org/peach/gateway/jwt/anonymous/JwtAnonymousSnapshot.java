package org.peach.gateway.jwt.anonymous;

import java.util.List;

/**
 * Redis 中的 JWT 匿名路径全量快照（与 common-service 发布结构一致）。
 */
public class JwtAnonymousSnapshot {

	private long revision;

	private String updatedAt;

	private List<JwtAnonymousRuleItem> items;

	public long getRevision() {
		return revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<JwtAnonymousRuleItem> getItems() {
		return items;
	}

	public void setItems(List<JwtAnonymousRuleItem> items) {
		this.items = items;
	}
}
