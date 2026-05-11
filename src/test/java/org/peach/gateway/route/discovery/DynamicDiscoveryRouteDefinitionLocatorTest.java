package org.peach.gateway.route.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinition;

/**
 * 动态路由目标均为 {@code lb://{serviceId}}。
 */
class DynamicDiscoveryRouteDefinitionLocatorTest {

	@Test
	void allServicesUseLbUri() {
		DiscoveryClient client = mock(DiscoveryClient.class);
		when(client.getServices()).thenReturn(List.of("peach-gateway", "peach-auth-service"));
		DynamicDiscoveryRouteDefinitionLocator loc = new DynamicDiscoveryRouteDefinitionLocator(client);
		List<RouteDefinition> defs = loc.getRouteDefinitions().collectList().block(Duration.ofSeconds(5));
		assertThat(defs).hasSize(2);
		assertThat(defs.stream().map(RouteDefinition::getUri)).containsExactlyInAnyOrder(
				URI.create("lb://peach-gateway"),
				URI.create("lb://peach-auth-service"));
	}
}
