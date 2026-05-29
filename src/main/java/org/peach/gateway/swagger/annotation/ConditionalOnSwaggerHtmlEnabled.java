package org.peach.gateway.swagger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.peach.gateway.swagger.condition.SwaggerEnabledCondition;
import org.springframework.context.annotation.Conditional;

/**
 * 组合条件：当 {@link SwaggerEnabledCondition} 为真时启用被标注类型（控制 Swagger 门户 HTML 等）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(SwaggerEnabledCondition.class)
public @interface ConditionalOnSwaggerHtmlEnabled {
}
