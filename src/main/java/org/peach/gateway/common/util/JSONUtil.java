package org.peach.gateway.common.util;

import java.nio.charset.StandardCharsets;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 网关 JSON 序列化工具，供过滤器、隔离层异常处理与统一返回体使用（Jackson 3）。
 */
public final class JSONUtil {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	private JSONUtil() {
	}

	/** 将对象序列化为 JSON 字节数组；{@code null} 参数字面量序列化为 {@code null}；序列化异常时当前实现返回 {@code null}。 */
	public static byte[] toJsonBytes(Object value) {
		if (value == null) {
			return "null".getBytes(StandardCharsets.UTF_8);
		}
		try {
			return MAPPER.writeValueAsBytes(value);
		}
		catch (JacksonException e) {
			return null;
		}
	}
}
