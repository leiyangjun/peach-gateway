package org.peach.gateway.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link GatewayRedisAccessor} 委托 gatewayRedisTemplate 读写完整键。
 */
@ExtendWith(MockitoExtension.class)
class GatewayRedisAccessorTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private GatewayRedisAccessor accessor;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		accessor = new GatewayRedisAccessor(redisTemplate);
	}

	@Test
	void getAndSetUseFullKey() {
		accessor.set("COMM-DEV-UNAUTHAPI", "{\"revision\":1}");
		verify(valueOperations).set(eq("COMM-DEV-UNAUTHAPI"), eq("{\"revision\":1}"));
		when(valueOperations.get("COMM-DEV-UNAUTHAPI")).thenReturn("{\"revision\":1}");
		assertEquals("{\"revision\":1}", accessor.get("COMM-DEV-UNAUTHAPI"));
	}

	@Test
	void getReturnsNullWhenMissing() {
		when(valueOperations.get("missing")).thenReturn(null);
		assertNull(accessor.get("missing"));
	}

}
