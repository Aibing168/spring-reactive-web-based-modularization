package com.abin.common;

import org.springframework.context.ApplicationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

public class Module {

    private String name;
    private int priority;
    private List<ModuleHandler> handlers;
    private ClassLoader loader;
    private ApplicationContext context;

    public Module(String name, int priority, ClassLoader loader, ApplicationContext context, List<ModuleHandler> handlers) {
        this.name = name;
        this.priority = priority;
        this.loader = loader;
        this.handlers = handlers;
        this.context = context;
    }

    public String name() {
        return name;
    }

    public int priority() {
        return this.priority;
    }

    public boolean supports(ServerWebExchange exchange, WebFilterChain chain) {
        return handlers.stream().anyMatch(h -> h.supports(exchange, chain));
    }

    public Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain) {
        return handlers.stream().filter(h -> h.supports(exchange, chain)).findFirst().get().handle(exchange, chain);
    }

}
