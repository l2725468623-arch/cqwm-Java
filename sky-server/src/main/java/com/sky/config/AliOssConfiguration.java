package com.sky.config;
import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class AliOssConfiguration {
    @Bean public AliOssUtil aliOssUtil(AliOssProperties properties) {return new AliOssUtil(properties);}
}
