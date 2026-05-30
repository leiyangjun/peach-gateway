package org.peach.gateway.permission.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.peach.gateway.permission.cache.ApisCache;
import org.peach.gateway.permission.loader.RoleApisLoader;

/**
 * {@link ApisPersChannelListener} 单测。
 */
@ExtendWith(MockitoExtension.class)
class RoleApisChannelListenerTest {

	@Mock
	private RoleApisLoader snapshotLoader;

	private ApisPersChannelListener listener;

	@BeforeEach
	void setUp() {
		listener = new ApisPersChannelListener(snapshotLoader);
		ApisCache.setRevision(5L);
	}

	@AfterEach
	void tearDown() {
		ApisCache.setRevision(0L);
	}

	@Test
	void onMessage_skipsWhenRevisionNotIncreased() {
		listener.onMessage("5");

		verify(snapshotLoader, never()).refreshFromRedis();
	}

	@Test
	void onMessage_refreshesWhenRevisionIncreased() {
		listener.onMessage("6");

		verify(snapshotLoader).refreshFromRedis();
	}

	@Test
	void onMessage_parsesJacksonLongJson() {
		listener.onMessage("7");

		verify(snapshotLoader).refreshFromRedis();
	}
}
