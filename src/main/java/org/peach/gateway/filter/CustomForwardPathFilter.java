package org.peach.gateway.filter;


import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomForwardPathFilter implements GatewayFilter, Ordered {
    // 用于在 Exchange 属性中保存原始路径的 key
    public static final String ORIGINAL_PATH_ATTRIBUTE = "originalRequestPath";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 从 Exchange 中获取当前请求
        var request = exchange.getRequest();
        // 获取原始的请求路径
        String originalPath = request.getURI().getRawPath();

        // 将原始路径存入 Exchange 的属性中，以便后续的过滤器能够读取
        exchange.getAttributes().put(ORIGINAL_PATH_ATTRIBUTE, originalPath);
        
        // 继续执行过滤器链
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 返回一个最小的 Order 值，确保它优先于其他过滤器执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}