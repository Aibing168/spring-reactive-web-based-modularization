package com.abin.module.hello;

import com.abin.common.ModuleHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class HelloConfiguration {

    @Bean
    ModuleHandler moduleHandler() {
        return new ModuleHandler() {

            private Set<String> paths = new HashSet<>(){{
                add("");
                add("/");
                add("/hello");
            }};

            @Override
            public int priority() {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean supports(ServerWebExchange exchange, WebFilterChain chain) {
                String path = exchange.getRequest().getPath().value();
                return paths.contains(path);
            }

            @Override
            public Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain) {
                String ret = "welcome to module hello!";
                DataBuffer buffer = exchange.getResponse().bufferFactory().allocateBuffer(ret.length());
                buffer.write(ret.getBytes());
                return exchange.getResponse().writeAndFlushWith(Mono.just(Mono.just(buffer)));
            }
        };
    }

}
