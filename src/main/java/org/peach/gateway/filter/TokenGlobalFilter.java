package org.peach.gateway.filter;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.peach.gateway.result.message.Message400;
import org.peach.gateway.result.web.ErrorResult;
import org.peach.gateway.util.JSONUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * 全局 Bearer JWT 校验（HS256）：校验签名与结构；将 {@code sub} 作为 JSON 对象解析后，以 {@code peach_*}
 * 查询参数附加到下游请求（并清除同名旧参数）。
 * <p>
 * HS256 密钥为类内常量字符串的 UTF-8 字节（长度须 ≥ 32），与签发方约定一致，不由配置文件注入。
 * </p>
 * <p>
 * 未通过校验时返回 401，体为 {@link ErrorResult}（{@code code} 为模块前缀 + 401 + {@link Message400} 末四位，
 * 见 {@link ErrorResult#unauthorized(MessageCode)}）。
 * </p>
 *
 * @author leiyangjun
 */
@Component
public class TokenGlobalFilter implements GlobalFilter, Ordered {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String QUERY_USER_ID = "peach_user_id";

	private static final String QUERY_USER_TYPE = "peach_user_type";

	private static final String QUERY_USERNAME = "peach_username";

	private static final String QUERY_NICKNAME = "peach_nickname";

	private static final String QUERY_REAL_NAME = "peach_real_name";

	private static final String QUERY_MOBILE = "peach_mobile";

	private static final String QUERY_EMAIL = "peach_email";

	private static final String QUERY_AVATAR = "peach_avatar";

	private static final String QUERY_GENDER = "peach_gender";

	/** JWT 不作校验的匿名路径（Ant）；含文档门户、登录全流程（含滑块挑战）、Swagger。 */
	private static final List<String> ANONYMOUS_PATTERNS = List.of("/", "/index.html", "/routes", "/v3/api-docs",
			"/v3/api-docs/**", "/v3/api-docs.yaml", "/v3/api-docs.yml", "/swagger-ui.html", "/swagger-ui/**",
			"/webjars/**", "/peach-doc-portal/**", "/*/auth/login/**", "/*/auth/login/slider/**", "/*/v3/api-docs/**",
			"/*/v3/api-docs.yaml", "/*/v3/api-docs.yml", "/*/swagger-ui/**", "/*/swagger-ui.html", "/*/routes/**",
			"/*/webjars/**");

	private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 300;

	/** 与 peach-auth-service 签发默认一致；仅内存常量，不由配置文件注入 */
	private static final String JWT_HS256_SECRET_PLAINTEXT = "550e8400-e29b-41d4-a716-446655440000";

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private volatile SecretKey verificationKey;

	/**
	 * {@code OPTIONS} 直接放行；匿名路径放行；否则要求 {@code Authorization: Bearer}，解析 JWT 成功后替换请求 URI
	 * 查询串再进入后续过滤器链。
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (request.getMethod() == HttpMethod.OPTIONS) {
			return chain.filter(exchange);
		}
		String path = request.getURI().getPath();
		if (isAnonymousPath(path)) {
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

	private boolean isAnonymousPath(String path) {
		for (String pattern : ANONYMOUS_PATTERNS) {
			if (pathMatcher.match(pattern, path)) {
				return true;
			}
		}
		return false;
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
		var b = UriComponentsBuilder.fromUri(uri);
		clearPeachParams(b);
		putJsonField(b, root, "id", QUERY_USER_ID);
		putJsonField(b, root, "username", QUERY_USERNAME);
		putJsonField(b, root, "nickname", QUERY_NICKNAME);
		putJsonField(b, root, "realName", QUERY_REAL_NAME);
		putJsonField(b, root, "mobile", QUERY_MOBILE);
		putJsonField(b, root, "email", QUERY_EMAIL);
		putJsonField(b, root, "avatar", QUERY_AVATAR);
		putJsonField(b, root, "gender", QUERY_GENDER);

		// build(true) 会把查询参数当「已编码」并严格校验；nickname 等含中文时必须先 UTF-8 百分号编码
		URI forwarded = b.build().encode(StandardCharsets.UTF_8).toUri();
		return request.mutate().uri(forwarded).build();
	}

	private static void clearPeachParams(UriComponentsBuilder b) {
		b.replaceQueryParam(QUERY_USER_ID);
		b.replaceQueryParam(QUERY_USER_TYPE);
		b.replaceQueryParam(QUERY_USERNAME);
		b.replaceQueryParam(QUERY_NICKNAME);
		b.replaceQueryParam(QUERY_REAL_NAME);
		b.replaceQueryParam(QUERY_MOBILE);
		b.replaceQueryParam(QUERY_EMAIL);
		b.replaceQueryParam(QUERY_AVATAR);
		b.replaceQueryParam(QUERY_GENDER);
	}

	/**
	 * 将 JSON 对象字段写入查询参数：数值与字符串均转为文本（与 servlet 侧解析一致）。
	 */
	private static void putJsonField(UriComponentsBuilder b, JsonNode root, String jsonProperty, String queryParam) {
		if (!root.has(jsonProperty) || root.get(jsonProperty).isNull()) {
			return;
		}
		JsonNode n = root.get(jsonProperty);
		String text = n.isNumber() ? n.numberValue().toString() : n.asText("");
		if (StringUtils.hasText(text)) {
			b.replaceQueryParam(queryParam, text.trim());
		}
	}

	/**
	 * 写入 401 与 JSON {@link ErrorResult}，立即结束请求（不进入下游）。
	 */
	private Mono<Void> unauthorized(ServerWebExchange exchange, Message400 message400) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
		ErrorResult err = ErrorResult.unauthorized(message400);
		byte[] body = JSONUtil.toJsonBytes(err);
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
	}

	@Override
	public int getOrder() {
		return ORDER;
	}
}
