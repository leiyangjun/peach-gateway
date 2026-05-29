package org.peach.gateway.apis.model;

import java.util.List;

/**
 * Redis 中的 JWT 匿名路径全量快照（与 common-service 发布结构一致）。
 */
public class ApiSnapshot {

	private long revision;

	private String updatedAt;

	private List<ApiModel> items;

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

	public List<ApiModel> getItems() {
		return items;
	}

	public void setItems(List<ApiModel> items) {
		this.items = items;
	}
}
