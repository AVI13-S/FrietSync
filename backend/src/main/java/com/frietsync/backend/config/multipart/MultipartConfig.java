package com.frietsync.backend.config.multipart;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        long maxSize = 100L * 1024 * 1024;
        return new MultipartConfigElement("", maxSize, maxSize, 0);
    }
}
