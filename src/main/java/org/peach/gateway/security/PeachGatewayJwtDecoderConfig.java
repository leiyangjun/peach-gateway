package org.peach.gateway.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.peach.gateway.config.PeachGatewayAuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * 网关 JWT 解码器：使用共享 HS256 密钥验证自定义登录接口签发的令牌。
 */
@Configuration
public class PeachGatewayJwtDecoderConfig {

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder(PeachGatewayAuthProperties properties) {
		byte[] bytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalArgumentException("peach.gateway.auth.jwt-secret 至少需要 32 字节");
		}
		SecretKey secretKey = new SecretKeySpec(bytes, "HmacSHA256");
		NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
		OAuth2TokenValidator<Jwt> defaultValidator = StringUtils.isBlank(properties.getIssuer())
				? JwtValidators.createDefault()
				: JwtValidators.createDefaultWithIssuer(properties.getIssuer());
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator));
		return decoder;
	}
}
