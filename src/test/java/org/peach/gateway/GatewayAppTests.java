package org.peach.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@ActiveProfiles("test")
class GatewayAppTests {

	@MockitoBean
	private DiscoveryClient discoveryClient;

	@Autowired
	private ApplicationContext applicationContext;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		when(discoveryClient.getServices()).thenReturn(List.of());
		webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void docPortalEndpointRegistered() {
		webTestClient.get().uri("/peach-doc-portal/services").exchange().expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON);
	}

	@Test
	void docPortalHtmlRegisteredInDefaultTestProfile() {
		webTestClient.get().uri("/index.html").exchange().expectStatus().isOk()
			.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
	}

	@Test
	void discoveryClientControllerBeanExists(@Autowired org.peach.gateway.web.DiscoveryClientController controller) {
		assertThat(controller).isNotNull();
	}
}
