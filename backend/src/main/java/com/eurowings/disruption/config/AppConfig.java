package com.eurowings.disruption.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public ExecutorService mockExecutorService() {
        // One search per source and route (3 x 3) runs concurrently; spare threads for parallel requests
        return Executors.newFixedThreadPool(16);
    }
}
