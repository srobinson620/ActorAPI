package com.sparta.actors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

// swagger is accessible at http://localhost:8088/swagger-ui/

@Configuration // spring identifies as configuration file
@EnableSwagger2 // enables swagger
public class SwaggerConfig {
    @Bean // tells spring that this method of the configuration class provides an instance of the docket class
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage(
                        "com.sparta.actors.controllers"))
                .paths(PathSelectors.any())
                .build().apiInfo(new ApiInfoBuilder()
                        .title("ACTORS-API")
                        .description("""
                                    Demonstration of a simple API
                                    Using Spring boot
                                    to access mysql database sakila and it's table actors
                                    """)
                        .version("1.0.0")
                        .build());
    }
}
