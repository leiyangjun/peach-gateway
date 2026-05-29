package org.peach.gateway.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * {@link CommRedisKeyBuilder} 键规则与校验单测。
 */
class CommRedisKeyBuilderTest {

	@Test
	void commKey_usesCommModuleAndUppercaseProfile() {
		CommRedisKeyBuilder builder = new CommRedisKeyBuilder("dev");
		assertEquals("COMM-DEV-UNAUTHAPI", builder.commKey("UNAUTHAPI"));
	}

	@Test
	void buildKey_concatenatesUppercaseModuleAndProfile() {
		CommRedisKeyBuilder builder = new CommRedisKeyBuilder("ignored");
		assertEquals("COMM-TEST-UNAUTHAPI", builder.buildKey("COMM", "test", "UNAUTHAPI"));
	}

	@Test
	void rejectBlankModuleOrProfileInBuildKey() {
		CommRedisKeyBuilder builder = new CommRedisKeyBuilder("dev");
		assertThrows(IllegalArgumentException.class, () -> builder.buildKey("  ", "dev", "k"));
		assertThrows(IllegalArgumentException.class, () -> builder.buildKey("COMM", "", "k"));
	}

	@Test
	void rejectEmptyOrDashOrColonInBusinessKey() {
		CommRedisKeyBuilder builder = new CommRedisKeyBuilder("dev");
		assertThrows(IllegalArgumentException.class, () -> builder.commKey(""));
		assertThrows(IllegalArgumentException.class, () -> builder.commKey("   "));
		assertThrows(IllegalArgumentException.class, () -> builder.commKey("a-b"));
		assertThrows(IllegalArgumentException.class, () -> builder.commKey("a:b"));
		assertThrows(IllegalArgumentException.class, () -> builder.commKey(null));
	}
}
