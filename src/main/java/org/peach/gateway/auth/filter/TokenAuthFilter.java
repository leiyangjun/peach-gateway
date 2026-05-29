package org.peach.gateway.auth.filter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.peach.gateway.common.result.message.Message400;
import org.peach.gateway.common.result.web.ErrorResult;
import org.peach.gateway.common.util.JSONUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bearer JWT 校验（HS256）：白名单跳过；否则校验签名与结构，将 {@code sub} 展开为 {@code peach_*} 查询参数。
 *
 * @author leiyangjun
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TokenAuthFilter implements GlobalFilter {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

	private static final String QUERY_USER_ID = "peach_user_id";

	private static final String QUERY_USER_TYPE = "peach_user_type";

	private static final String QUERY_USERNAME = "peach_username";

	private static final String QUERY_NICKNAME = "peach_nickname";

	private static final String QUERY_REAL_NAME = "peach_real_name";

	private static final String QUERY_MOBILE = "peach_mobile";

	private static final String QUERY_EMAIL = "peach_email";

	private static final String QUERY_AVATAR = "peach_avatar";

	private static final String QUERY_GENDER = "peach_gender";

	private static final Set<String> PEACH_QUERY_KEYS_TO_STRIP = Set.of(QUERY_USER_ID, QUERY_USER_TYPE, QUERY_USERNAME,
		QUERY_NICKNAME, QUERY_REAL_NAME, QUERY_MOBILE, QUERY_EMAIL, QUERY_AVATAR, QUERY_GENDER);

	/** 与 peach-auth-service 签发默认一致；仅内存常量，不由配置文件注入 */
	private static final String JWT_HS256_SECRET_PLAINTEXT = "550e8400-e29b-41d4-a716-446655440000";

	private volatile SecretKey verificationKey;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (request.getMethod() == HttpMethod.OPTIONS) {
			return chain.filter(exchange);
		}
		Boolean skipAuth = exchange.getAttribute("skipAuth");
		if (skipAuth != null && skipAuth) {
			return chain.filter(exchange);
		}
		String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(auth) || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return unauthorized(exchange, Message400.GATEWAY_AUTH_HEADER_MISSING);
		}
		String token = auth.substring(7).trim();
		if (!StringUtils.hasText(token)) {
			return unauthorized(exchange, Message400.GATEWAY_AUTH_BEARER_EMPTY);
		}
		try {
			Claims claims = parseAndValidateClaims(token);
			ServerHttpRequest mutated = appendIdentityQuery(request, claims);
			return chain.filter(exchange.mutate().request(mutated).build());
		} catch (JwtException ex) {
			return unauthorized(exchange, Message400.GATEWAY_AUTH_JWT_INVALID);
		} catch (IllegalStateException ex) {
			return unauthorized(exchange, Message400.GATEWAY_AUTH_JWT_CONFIG);
		}
	}

	private Claims parseAndValidateClaims(String compactJwt) {
		SecretKey key = verificationKey();
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(compactJwt).getPayload();
	}

	private SecretKey verificationKey() {
		SecretKey k = this.verificationKey;
		if (k != null) {
			return k;
		}
		synchronized (this) {
			if (this.verificationKey == null) {
				byte[] bytes = JWT_HS256_SECRET_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
				if (bytes.length < 32) {
					throw new IllegalStateException("JWT HS256 秘钥 UTF-8 长度须至少 32 字节");
				}
				this.verificationKey = Keys.hmacShaKeyFor(bytes);
			}
			return this.verificationKey;
		}
	}

	private static ServerHttpRequest appendIdentityQuery(ServerHttpRequest request, Claims claims) {
		String sub = claims.getSubject();
		if (!StringUtils.hasText(sub)) {
			throw new JwtException("JWT subject 为空");
		}
		JsonNode root;
		try {
			root = OBJECT_MAPPER.readTree(sub.trim());
		} catch (Exception ex) {
			throw new JwtException("JWT subject 非合法 JSON");
		}
		if (root == null || !root.isObject()) {
			throw new JwtException("JWT subject 须为 JSON 对象");
		}

		URI uri = request.getURI();
		String mergedRawQuery = mergePeachIdentityRawQuery(uri.getRawQuery(), root);
		URI forwarded = rebuildUriPreservingRawPath(uri, mergedRawQuery);
		return request.mutate().uri(forwarded).build();
	}

	private static String mergePeachIdentityRawQuery(String rawQuery, JsonNode root) {
		List<String> segments = new ArrayList<>();
		if (StringUtils.hasText(rawQuery)) {
			for (String segment : rawQuery.split("&")) {
				if (!StringUtils.hasText(segment)) {
					continue;
				}
				int eq = segment.indexOf('=');
				String name = eq >= 0 ? segment.substring(0, eq) : segment;
				if (PEACH_QUERY_KEYS_TO_STRIP.contains(name)) {
					continue;
				}
				segments.add(segment);
			}
		}
		appendEncodedPeachParam(segments, root, "id", QUERY_USER_ID);
		appendEncodedPeachParam(segments, root, "username", QUERY_USERNAME);
		appendEncodedPeachParam(segments, root, "nickname", QUERY_NICKNAME);
		appendEncodedPeachParam(segments, root, "realName", QUERY_REAL_NAME);
		appendEncodedPeachParam(segments, root, "mobile", QUERY_MOBILE);
		appendEncodedPeachParam(segments, root, "email", QUERY_EMAIL);
		appendEncodedPeachParam(segments, root, "avatar", QUERY_AVATAR);
		appendEncodedPeachParam(segments, root, "gender", QUERY_GENDER);
		return String.join("&", segments);
	}

	private static void appendEncodedPeachParam(List<String> segments, JsonNode root, String jsonProperty,
		String queryParam) {
		if (!root.has(jsonProperty) || root.get(jsonProperty).isNull()) {
			return;
		}
		JsonNode n = root.get(jsonProperty);
		String text = n.isNumber() ? n.numberValue().toString() : n.asText("");
		if (!StringUtils.hasText(text)) {
			return;
		}
		String encName = UriUtils.encodeQueryParam(queryParam, StandardCharsets.UTF_8);
		String encVal = UriUtils.encodeQueryParam(text.trim(), StandardCharsets.UTF_8);
		segments.add(encName + "=" + encVal);
	}

	private static URI rebuildUriPreservingRawPath(URI uri, String mergedRawQuery) {
		String rawPath = uri.getRawPath();
		if (!StringUtils.hasText(rawPath)) {
			rawPath = uri.getPath();
		}
		if (rawPath == null) {
			rawPath = "";
		}
		UriComponentsBuilder b = UriComponentsBuilder.newInstance().scheme(uri.getScheme()).host(uri.getHost());
		if (uri.getPort() != -1) {
			b.port(uri.getPort());
		}
		if (StringUtils.hasText(uri.getRawUserInfo())) {
			b.userInfo(uri.getRawUserInfo());
		}
		b.path(rawPath);
		if (StringUtils.hasText(mergedRawQuery)) {
			b.replaceQuery(mergedRawQuery);
		}
		if (StringUtils.hasText(uri.getRawFragment())) {
			b.fragment(uri.getRawFragment());
		}
		return b.build(true).toUri();
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange, Message400 message400) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
		ErrorResult err = ErrorResult.unauthorized(message400);
		byte[] body = JSONUtil.toJsonBytes(err);
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
	}

}
