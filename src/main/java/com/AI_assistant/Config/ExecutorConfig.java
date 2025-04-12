package com.AI_assistant.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean
    public Executor getExecutor(){
        Executor executor = Executors.newFixedThreadPool(5);
        return executor;
    }

}
