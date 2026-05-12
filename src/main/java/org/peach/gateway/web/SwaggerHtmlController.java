package org.peach.gateway.web;

import org.peach.gateway.config.condition.ConditionalOnSwaggerHtmlEnabled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Hidden;
import reactor.core.publisher.Mono;

/**
 * 提供类路径静态资源 {@code docportal/index.html} 作为 Swagger 文档门户入口（{@code GET /index.html}）。
 * <p>
 * 受 {@link org.peach.gateway.config.condition.ConditionalOnSwaggerHtmlEnabled} 与 {@code peach.swagger.enabled}
 * 条件控制是否注册。
 * </p>
 *
 * @author leiyangjun
 */
@Hidden
@Controller
@ConditionalOnSwaggerHtmlEnabled
@ConditionalOnProperty(prefix = "peach.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerHtmlController {

	private static final Resource HTML = new ClassPathResource("org/peach/gateway/docportal/index.html");

	/** 返回门户 HTML 资源（不读磁盘路径，自 classpath 加载）。 */
	@GetMapping(path = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public Mono<Resource> portal() {
		return Mono.just(HTML);
	}
}
