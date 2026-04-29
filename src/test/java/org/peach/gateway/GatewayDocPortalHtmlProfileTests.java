package org.peach.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

/**
 * prod 系 profile 下 Swagger 门户 HTML 应默认关闭。
 */
@SpringBootTest
@ActiveProfiles({ "test", "prod" })
class GatewayDocPortalHtmlProfileTests {

	@MockitoBean
	private DiscoveryClient discoveryClient;

	@Autowired
	private org.springframework.context.ApplicationContext applicationContext;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		Mockito.when(discoveryClient.getServices()).thenReturn(List.of());
		webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
	}

	@Test
	void swaggerHtmlNotRegisteredWhenProdProfile() {
		assertThat(applicationContext.getBeanNamesForType(org.peach.gateway.web.SwaggerHtmlController.class)).isEmpty();
	}

	@Test
	void swaggerHtmlReturns404WhenProdProfile() {
		webTestClient.get().uri("/index.html").exchange().expectStatus().isNotFound();
	}
}
