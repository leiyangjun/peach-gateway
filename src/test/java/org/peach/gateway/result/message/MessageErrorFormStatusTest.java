package org.peach.gateway.result.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 与 {@link MessageError#formStatus(int)} 当前实现一致：已注册 HTTP 状态取对应枚举，否则回落为
 * {@link MessageError#INTERNAL_SERVER_ERROR}。
 */
class MessageErrorFormStatusTest {

	@Test
	void registered404MapsToNotFound() {
		assertThat(MessageError.formStatus(HttpStatus.NOT_FOUND.value())).isEqualTo(MessageError.NOT_FOUND);
		assertThat(MessageError.NOT_FOUND.code()).isEqualTo(4013);
	}

	@Test
	void unregisteredStatusFallsBackToInternalServerError() {
		assertThat(MessageError.formStatus(999)).isEqualTo(MessageError.INTERNAL_SERVER_ERROR);
	}
}
