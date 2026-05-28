package org.peach.gateway.jwt.anonymous;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * 网关 JWT 匿名路径本地缓存：优先 Redis 快照；无快照时使用内置兜底规则（method 须与请求动词一致，不支持 ALL）。
 */
@Component
public class JwtAnonymousRuleCache {

	private static final Logger LOG = LoggerFactory.getLogger(JwtAnonymousRuleCache.class);


	/** Redis 不可用或尚未加载时的兜底规则（与 init_data 内置项对齐） */
	//rule("GET", GatewayPublicPathConstants.DOC_PORTAL_PREFIX + "/**"),
	private static final List<Rule> FALLBACK_RULES = List.of();

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private volatile List<Rule> dynamicRules = List.of();

	private volatile long revision;

	public boolean matches(HttpMethod httpMethod, String path) {
		if (!StringUtils.hasText(path)) {
			return false;
		}
		List<Rule> dynamic = this.dynamicRules;
		if (!dynamic.isEmpty()) {
			return matchRules(dynamic, httpMethod, path);
		}
		return matchRules(FALLBACK_RULES, httpMethod, path);
	}

	public void replaceFromSnapshot(JwtAnonymousSnapshot snapshot) {
		if (snapshot == null || snapshot.getItems() == null) {
			this.dynamicRules = List.of();
			this.revision = 0L;
			return;
		}
		List<Rule> next = new ArrayList<>(snapshot.getItems().size());
		for (JwtAnonymousRuleItem item : snapshot.getItems()) {
			if (item == null || !StringUtils.hasText(item.getFinalPath())) {
				continue;
			}
			String method = item.getMethod() == null ? "" : item.getMethod().trim();
			if (!StringUtils.hasText(method) || "ALL".equalsIgnoreCase(method)) {
				LOG.warn("跳过无效匿名规则 method={} path={}", method, item.getFinalPath());
				continue;
			}
			next.add(rule(method, item.getFinalPath().trim()));
		}
		this.dynamicRules = List.copyOf(next);
		this.revision = snapshot.getRevision();
	}

	public long getRevision() {
		return revision;
	}

	private boolean matchRules(List<Rule> rules, HttpMethod httpMethod, String path) {
		for (Rule rule : rules) {
			if (methodMatches(rule.method(), httpMethod) && pathMatcher.match(rule.finalPath(), path)) {
				return true;
			}
		}
		return false;
	}

	private static boolean methodMatches(String ruleMethod, HttpMethod requestMethod) {
		if (requestMethod == null || !StringUtils.hasText(ruleMethod)) {
			return false;
		}
		return ruleMethod.equalsIgnoreCase(requestMethod.name());
	}

	private static Rule rule(String method, String finalPath) {
		return new Rule(method, finalPath);
	}

	private record Rule(String method, String finalPath) {
	}
}
