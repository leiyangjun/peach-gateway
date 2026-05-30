package org.peach.gateway.permission.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.peach.gateway.permission.cache.RoleUsersCache;
import org.peach.gateway.permission.loader.RoleUsersLoader;

/**
 * {@link RoleUsersChannelListener} 单测。
 */
@ExtendWith(MockitoExtension.class)
class RoleUsersChannelListenerTest {

	@Mock
	private RoleUsersLoader snapshotLoader;

	private RoleUsersChannelListener listener;

	@BeforeEach
	void setUp() {
		listener = new RoleUsersChannelListener(snapshotLoader);
		RoleUsersCache.setRevision(2L);
	}

	@AfterEach
	void tearDown() {
		RoleUsersCache.setRevision(0L);
	}

	@Test
	void onMessage_skipsWhenRevisionNotIncreased() {
		listener.onMessage("2");

		verify(snapshotLoader, never()).refreshFromRedis();
	}

	@Test
	void onMessage_refreshesWhenRevisionIncreased() {
		listener.onMessage("3");

		verify(snapshotLoader).refreshFromRedis();
	}
}
