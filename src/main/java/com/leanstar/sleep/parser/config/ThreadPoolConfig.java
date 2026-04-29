package com.leanstar.sleep.parser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Bean("sleepDataExecutor")
    public Executor sleepDataExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);              // 核心线程数
        executor.setMaxPoolSize(50);               // 最大线程数
        executor.setQueueCapacity(10000);          // 队列容量
        executor.setThreadNamePrefix("sleep-data-");
        executor.initialize();
        return executor;
    }

}
