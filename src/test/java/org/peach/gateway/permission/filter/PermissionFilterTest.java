package org.peach.gateway.permission.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.peach.gateway.common.GatewayExchangeAttributes;
import org.peach.gateway.common.bootstrap.cache.ModuleCodeCache;
import org.peach.gateway.common.result.message.Message400;
import org.peach.gateway.common.result.web.ErrorResult;
import org.peach.gateway.permission.matcher.PermEvaluator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link PermissionFilter} 单测。
 */
@ExtendWith(MockitoExtension.class)
class PermissionFilterTest {

	@Mock
	private PermEvaluator rolePermEvaluator;

	@Mock
	private GatewayFilterChain chain;

	private PermissionFilter filter;

	@BeforeEach
	void setUp() {
		ModuleCodeCache.setCachedModule("GWAY");
		filter = new PermissionFilter(rolePermEvaluator);
	}

	@AfterEach
	void tearDown() {
		ModuleCodeCache.setCachedModule(null);
	}

	@Test
	void filter_skipsWhenSkipAuth() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		ServerWebExchange exchange = exchange("/peach-common-service/admin/x", null, null);
		exchange.getAttributes().put(GatewayExchangeAttributes.SKIP_AUTH, true);

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		verify(rolePermEvaluator, never()).isPermitted(any(Long.class), any(), any());
	}

	@Test
	void filter_skipsWhenSkipPermission() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		ServerWebExchange exchange = exchange("/peach-common-service/admin/x", "100", "user1");
		exchange.getAttributes().put(GatewayExchangeAttributes.SKIP_PERMISSION, true);

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		verify(rolePermEvaluator, never()).isPermitted(any(Long.class), any(), any());
	}

	@Test
	void filter_skipsOptions() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.options("/peach-common-service/admin/x").build());

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		verify(rolePermEvaluator, never()).isPermitted(any(Long.class), any(), any());
	}

	@Test
	void filter_skipsSuperAdmin() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		ServerWebExchange exchange = exchange("/peach-common-service/admin/x", "100", "admin");

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
		verify(rolePermEvaluator, never()).isPermitted(any(Long.class), any(), any());
	}

	@Test
	void filter_forbiddenWhenNotPermitted() {
		ServerWebExchange exchange = exchange("/peach-common-service/admin/user/list", "100", "user1");
		when(rolePermEvaluator.isPermitted(eq(100L), eq(HttpMethod.POST), any())).thenReturn(false);

		filter.filter(exchange, chain).block();

		assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
		verify(chain, never()).filter(exchange);
	}

	@Test
	void forbiddenUsesGateway4034003Code() {
		ErrorResult err = ErrorResult.forbidden(Message400.GATEWAY_FORBIDDEN);
		assertEquals("GWAY4034003", err.getCode());
	}

	@Test
	void filter_passesWhenPermitted() {
		when(chain.filter(any())).thenReturn(Mono.empty());
		ServerWebExchange exchange = exchange("/peach-common-service/admin/user/list", "100", "user1");
		when(rolePermEvaluator.isPermitted(eq(100L), eq(HttpMethod.POST), any())).thenReturn(true);

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
	}

	private static ServerWebExchange exchange(String path, String userId, String username) {
		MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.post(path);
		if (userId != null) {
			builder = builder.queryParam("peach_user_id", userId);
		}
		if (username != null) {
			builder = builder.queryParam("peach_username", username);
		}
		return MockServerWebExchange.from(builder.build());
	}
}
