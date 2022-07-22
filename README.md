# ActorAPI
requires [sakila download] https://dev.mysql.com/doc/sakila/en/sakila-installation.html




## Adding Swagger to the API

First add the ifollowing dependancy to your pom.xml

```
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-boot-starter</artifactId>
            <version>3.0.0</version>
        </dependency>
```

which can be found by searching for "springfox boot starter" in the by selecting "generate>add dependency" on the pom.xml menu.

Add a new class at com.sparta.actors and calkl it SwaggerConfig

```
package com.sparta.actors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration        // Tell Spring Boot that this is a configuration file
@EnableSwagger2       // Initialise Swagger 2
public class SwaggerConfig {
    @Bean							// This method is intended to provide a Docket to spring boot 
                      // Docket is a configuration file describing what we widh Swagger to display
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage(
                        "com.sparta.actors.controllers")) // where we find API interface controllers for swagger to describe
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
```

Add the following line to application.properties

```
spring.mvc.pathmatch.matching-strategy=ant-path-matcher
```

This tells spring boot to use a mechanism for matching urls that is compatible with swagger

If you now restart the application

```
http://localhost:8088/swagger-ui/
```

will provide swagger documentation.

Swagger provides a number of annotations that can be used to improve the documentation.

for instance if you add the annoitation 

```
@ApiOperation("Get Actor identified by actor_id specified in the path")
```

to the actorDetails method in ActorsController

and add 

```
@ApiParam("actor_id")
```

To the parameter of that method 

so we get 

```
    @GetMapping("/actor/{id}") // the get mapping links a data provider to a data request
    @ApiOperation("Get Actor identified by actor_id specified in the path")
    public Actor actorDetails(@ApiParam("actor_id") @PathVariable int id){
```

These will be added to the swagger outout..





