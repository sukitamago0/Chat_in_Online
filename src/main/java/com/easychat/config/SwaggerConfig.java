package com.easychat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        // ===== Swagger 核心配置 =====
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描你项目中 Controller 所在的包
                .apis(RequestHandlerSelectors.basePackage("com.easychat.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("EasyJava 项目接口文档")
                .description("用于调试与测试后端接口的 Swagger 配置")
                .version("1.0")
                .build();
    }

    //    <!--        swagger-->
    //        <dependency>
    //            <groupId>io.springfox</groupId>
    //            <artifactId>springfox-swagger2</artifactId>
    //            <version>2.9.2</version>
    //        </dependency>
    //        <dependency>
    //            <groupId>io.springfox</groupId>
    //            <artifactId>springfox-swagger-ui</artifactId>
    //            <version>2.9.2</version>
    //        </dependency>
    //        <dependency>
    //            <groupId>com.google.guava</groupId>
    //            <artifactId>guava</artifactId>
    //            <version>31.1-jre</version>
    //        </dependency>
}
