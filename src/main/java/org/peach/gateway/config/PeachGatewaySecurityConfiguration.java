package org.peach.gateway.config;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 网关作为统一 JWT 校验入口：校验认证服务（如 peach-common-service）签发的令牌。
 * 校验通过后，由 {@link org.peach.gateway.filter.PeachGatewayForwardedUserGatewayFilter} 等将当前用户写入下游请求（如查询参数）。
 */
@Configuration
@EnableWebFluxSecurity
public class PeachGatewaySecurityConfiguration {

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, PeachGatewayAuthProperties props) {
		if (!props.isEnabled()) {
			return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(ex -> ex.anyExchange().permitAll())
				.build();
		}
		return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
			.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.authorizeExchange(exchanges -> exchanges.pathMatchers(HttpMethod.OPTIONS, "/**")
				.permitAll()
				.pathMatchers(props.getWhitelistPaths().toArray(String[]::new))
				.permitAll()
				.anyExchange()
				.authenticated())
			.exceptionHandling(spec -> spec.authenticationEntryPoint((exchange, ex) ->
				writeUnauthorized(exchange, ex.getMessage() != null ? ex.getMessage() : "未授权访问")))
			.build();
	}

	private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		String body = "{\"message\":\"" + escapeJson(message) + "\",\"status\":401}";
		return exchange.getResponse()
			.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
	}

	private String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
