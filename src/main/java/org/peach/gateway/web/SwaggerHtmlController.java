package org.peach.gateway.web;

import org.peach.gateway.config.condition.ConditionalOnSwaggerHtmlEnabled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import reactor.core.publisher.Mono;

/**
 * Swagger 门户静态页（根路径 {@code /index.html}）。
 */
@Controller
@ConditionalOnSwaggerHtmlEnabled
@ConditionalOnProperty(prefix = "peach.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerHtmlController {

	private static final Resource HTML = new ClassPathResource("org/peach/gateway/docportal/index.html");

	@GetMapping(path = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public Mono<Resource> portal() {
		return Mono.just(HTML);
	}
}
