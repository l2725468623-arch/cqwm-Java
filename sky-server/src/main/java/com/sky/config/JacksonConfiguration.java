package com.sky.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.json.JacksonObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class JacksonConfiguration {
    @Bean public ObjectMapper objectMapper() { return new JacksonObjectMapper(); }
}
