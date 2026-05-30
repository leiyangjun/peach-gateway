package org.peach.gateway.permission.model;

import java.util.List;
import java.util.Map;

/**
 * 角色-用户 Redis 全量快照：键为稳定业务编码 {@code roleCode}，值为用户 ID 列表。
 *
 * @author leiyangjun
 * @date 2026-05-30
 */
public class RoleUsersSnapshot {

	private long revision;

	private String updatedAt;

	/** roleCode → userId 列表 */
	private Map<String, List<Long>> roles;

	public RoleUsersSnapshot() {
	}

	public RoleUsersSnapshot(long revision, String updatedAt, Map<String, List<Long>> roles) {
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

	public Map<String, List<Long>> getRoles() {
		return roles;
	}

	public void setRoles(Map<String, List<Long>> roles) {
		this.roles = roles;
	}
}
