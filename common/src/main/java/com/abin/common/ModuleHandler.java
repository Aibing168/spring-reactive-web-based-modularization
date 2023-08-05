package com.abin.common;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public interface ModuleHandler {

    default int priority() {
        return 0;
    }
    default boolean supports(ServerWebExchange exchange, WebFilterChain chain) {
        return false;
    }
    Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain);

}
