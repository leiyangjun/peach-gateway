package org.peach.gateway.apis.filter;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.peach.gateway.apis.cache.UnauthApiCache;
import org.peach.gateway.apis.model.ApiModel;
import org.peach.gateway.common.GatewayExchangeAttributes;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link WhitelistFilter} 单测。
 */
@ExtendWith(MockitoExtension.class)
class WhitelistFilterTest {

	@Mock
	private GatewayFilterChain chain;

	private WhitelistFilter filter;

	@BeforeEach
	void setUp() {
		filter = new WhitelistFilter();
		when(chain.filter(any())).thenReturn(Mono.empty());
	}

	@AfterEach
	void tearDown() {
		UnauthApiCache.setApis(List.of());
	}

	@Test
	void filter_setsSkipAuthForAccessType1() {
		ApiModel rule = rule("POST", "/public/**", (short) 1);
		UnauthApiCache.setApis(List.of(rule));
		ServerWebExchange exchange = exchange(HttpMethod.POST, "/public/ping");

		filter.filter(exchange, chain).block();

		assertTrue(Boolean.TRUE.equals(exchange.getAttribute(GatewayExchangeAttributes.SKIP_AUTH)));
		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_PERMISSION));
	}

	@Test
	void filter_setsSkipPermissionForAccessType2() {
		ApiModel rule = rule("GET", "/api-common/admin/profile", (short) 2);
		UnauthApiCache.setApis(List.of(rule));
		ServerWebExchange exchange = exchange(HttpMethod.GET, "/api-common/admin/profile");

		filter.filter(exchange, chain).block();

		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_AUTH));
		assertTrue(Boolean.TRUE.equals(exchange.getAttribute(GatewayExchangeAttributes.SKIP_PERMISSION)));
	}

	@Test
	void filter_ignoresMissingAccessType() {
		ApiModel rule = rule("GET", "/legacy/**", null);
		UnauthApiCache.setApis(List.of(rule));
		ServerWebExchange exchange = exchange(HttpMethod.GET, "/legacy/x");

		filter.filter(exchange, chain).block();

		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_AUTH));
		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_PERMISSION));
	}

	@Test
	void filter_ignoresUnknownAccessType() {
		ApiModel rule = rule("GET", "/unknown/**", (short) 99);
		UnauthApiCache.setApis(List.of(rule));
		ServerWebExchange exchange = exchange(HttpMethod.GET, "/unknown/x");

		filter.filter(exchange, chain).block();

		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_AUTH));
		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_PERMISSION));
	}

	@Test
	void filter_prefersSkipAuthWhenBothTypesMatch() {
		ApiModel type2 = rule("POST", "/mixed/**", (short) 2);
		ApiModel type1 = rule("POST", "/mixed/**", (short) 1);
		UnauthApiCache.setApis(List.of(type2, type1));
		ServerWebExchange exchange = exchange(HttpMethod.POST, "/mixed/x");

		filter.filter(exchange, chain).block();

		assertTrue(Boolean.TRUE.equals(exchange.getAttribute(GatewayExchangeAttributes.SKIP_AUTH)));
		assertNull(exchange.getAttribute(GatewayExchangeAttributes.SKIP_PERMISSION));
	}

	private static ApiModel rule(String method, String path, Short accessType) {
		ApiModel m = new ApiModel();
		m.setMethod(method);
		m.setFinalPath(path);
		m.setAccessType(accessType);
		return m;
	}

	private static ServerWebExchange exchange(HttpMethod method, String path) {
		return MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
	}
}
