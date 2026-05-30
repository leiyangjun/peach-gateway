package org.peach.gateway.permission.model;

import java.util.List;
import java.util.Map;

/**
 * 角色-API Redis 全量快照：键为稳定业务编码 {@code roleCode}，值为 API 列表。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
public class RoleApisSnapshot {

	private long revision;

	private String updatedAt;

	/** roleCode → API 列表（method + finalPath） */
	private Map<String, List<ApiPermModel>> roles;

	public RoleApisSnapshot() {
	}

	public RoleApisSnapshot(long revision, String updatedAt, Map<String, List<ApiPermModel>> roles) {
		this.revision = revision;
		this.updatedAt = updatedAt;
		this.roles = roles;
	}

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

	public Map<String, List<ApiPermModel>> getRoles() {
		return roles;
	}

	public void setRoles(Map<String, List<ApiPermModel>> roles) {
		this.roles = roles;
	}
}
