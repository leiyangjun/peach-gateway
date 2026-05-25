package org.peach.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * WebFlux 全局 CORS：覆盖网关本机 Controller、静态资源及未命中路由的 OPTIONS 预检。
 * <p>
 * 与 {@link org.peach.gateway.filter.TokenGlobalFilter} 配合：预检 OPTIONS 在 JWT 过滤器之前由
 * {@link CorsWebFilter} 处理或放行；业务过滤器已对 OPTIONS 直接 {@code chain.filter}。
 * </p>
 * <p>
 * 规则绑定 {@link GatewayCorsProperties}（{@code peach.gateway.cors}）。勿再使用已废弃的
 * {@code spring.cloud.gateway.globalcors}；若改由 Gateway 内置 CORS，路径须为
 * {@code spring.cloud.gateway.server.webflux.globalcors} 并与本配置保持一致，避免重复响应头。
 * </p>
 *
 * @author leiyangjun
 */
@Configuration(proxyBeanMethods = false)
public class GatewayCorsWebFilterConfiguration {

	/**
	 * 最高优先级 WebFilter，确保浏览器预检在鉴权过滤器之前得到 CORS 响应头。
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public CorsWebFilter peachGatewayCorsWebFilter(GatewayCorsProperties properties) {
		CorsConfiguration config = toCorsConfiguration(properties);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return new CorsWebFilter(source);
	}

	static CorsConfiguration toCorsConfiguration(GatewayCorsProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
		config.setAllowedMethods(properties.getAllowedMethods());
		config.setAllowedHeaders(properties.getAllowedHeaders());
		config.setExposedHeaders(properties.getExposedHeaders());
		config.setAllowCredentials(properties.isAllowCredentials());
		config.setMaxAge(properties.getMaxAge());
		return config;
	}
}
