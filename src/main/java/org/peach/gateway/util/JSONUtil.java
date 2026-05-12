package org.peach.gateway.util;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 网关 JSON 序列化工具，供过滤器、隔离层异常处理与统一返回体使用。
 *
 * @author leiyangjun
 */
public final class JSONUtil {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JSONUtil() {
	}

	/** 将对象序列化为 JSON 字节数组；{@code null} 参数字面量序列化为 {@code null}；序列化异常时当前实现返回 {@code null}。 */
	public static byte[] toJsonBytes(Object value) {
		if (value == null) {
			return "null".getBytes(StandardCharsets.UTF_8);
		}
		try {
			return MAPPER.writeValueAsBytes(value);
		} catch (JsonProcessingException e) {
//			String mc = ModuleCodeCache.get();
//			if (mc == null || mc.isBlank()) {
//				mc = "UNKN";
//			}
//			String code = GatewayApiResultCodeComposer.compose(mc, 500, Message500.GATEWAY_JSON_SERIALIZE_FAILED.code());
//			String msg = Message500.GATEWAY_JSON_SERIALIZE_FAILED.msg();
//			String json = String.format("{\"code\":\"%s\",\"msg\":\"%s\"}", code, msg);
//			return json.getBytes(StandardCharsets.UTF_8);
			return null;
		}
	}
}
