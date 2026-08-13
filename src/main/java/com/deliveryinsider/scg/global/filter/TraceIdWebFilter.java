package com.deliveryinsider.scg.global.filter;

import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdWebFilter implements WebFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        String traceId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming;
        ServerWebExchange mutated = exchange.mutate().request(builder -> builder.header(TRACE_HEADER, traceId)).build();
        mutated.getResponse().getHeaders().set(TRACE_HEADER, traceId);
        return chain.filter(mutated);
    }
}
