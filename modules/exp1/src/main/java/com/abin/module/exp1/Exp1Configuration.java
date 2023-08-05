package com.abin.module.exp1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Exp1Configuration {

    @Bean
    public Exp1ModuleHandler exp1ModuleHandler() {
        return new Exp1ModuleHandler();
    }

}
