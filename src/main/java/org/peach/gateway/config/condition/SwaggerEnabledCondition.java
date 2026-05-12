package org.peach.gateway.config.condition;

import java.util.Locale;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Swagger 门户 HTML 是否暴露：显式配置 {@code peach.swagger.html-enabled} 优先生效；
 * 未配置时，若激活 profile 为 prod / product / produce（忽略大小写）则默认关闭，否则默认开启。
 *
 * @author leiyangjun
 */
public class SwaggerEnabledCondition implements Condition {

	/** 与 {@link ConditionalOnSwaggerHtmlEnabled} 配套：判断是否注册 Swagger 门户 HTML 相关 Bean。 */
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment env = context.getEnvironment();
		Boolean explicit = env.getProperty("peach.swagger.html-enabled", Boolean.class);
		if (explicit != null) {
			return explicit;
		}
		for (String p : env.getActiveProfiles()) {
			if (p == null) {
				continue;
			}
			String l = p.toLowerCase(Locale.ROOT);
			if ("prod".equals(l) || "product".equals(l) || "produce".equals(l)) {
				return false;
			}
		}
		return true;
	}
}
