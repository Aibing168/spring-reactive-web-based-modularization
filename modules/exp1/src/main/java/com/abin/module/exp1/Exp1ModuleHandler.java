package com.abin.module.exp1;

import com.abin.common.ModuleHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Exp1ModuleHandler implements ModuleHandler {

    AntPathMatcher matcher = new AntPathMatcher();
    Set<String> paths = new HashSet<>(){{
       add("/{expId}");
    }};

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public boolean supports(ServerWebExchange exchange, WebFilterChain chain) {
        return paths.stream().anyMatch(pattern -> matcher.match(pattern, exchange.getRequest().getPath().value()));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain) {
        String pattern = "/{expId}";
        String path = exchange.getRequest().getPath().value();
        Map<String, String> map = matcher.extractUriTemplateVariables(pattern, path);
        return exchange.getResponse().writeAndFlushWith(Mono.just(Mono.just(exchange.getResponse().bufferFactory().wrap(map.get("expId").getBytes()))));
    }

}
