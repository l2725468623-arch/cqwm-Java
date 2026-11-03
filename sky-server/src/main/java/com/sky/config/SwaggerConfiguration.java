package com.sky.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
@Configuration
public class SwaggerConfiguration {
    @Bean public Docket adminApi() {
        ApiInfo info = new ApiInfoBuilder().title("苍穹外卖管理端接口")
                .description("员工登录、身份校验、分页查询与工作台")
                .version("1.0").build();
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(info).groupName("管理端")
                .select().apis(RequestHandlerSelectors.basePackage("com.sky.controller.admin"))
                .paths(PathSelectors.any()).build();
    }
}
