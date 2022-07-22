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

which can be found by searching for __springfox boot starter__ in the by selecting __generate\>add dependency__ on the pom.xml menu.

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

@Configuration       // Tell Spring Boot that this is a configuration file
@EnableSwagger2      // Initialise Swagger 2
public class SwaggerConfig {
    @Bean            // This method is intended to provide a Docket to spring boot 
                     // Docket is a configuration file describing what we wish Swagger to display
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




# Converting to using JPA and Hibernate

Hibernate is an instance of JPA, it is the default JPA type for spring boot.

First add JPA configuration to the __pom.xml__

search for __spring-boot-starter-data-jpa__

This should insert 

```
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
            <version>2.7.0</version>
        </dependency>
```

There is an intelliJ plugin called JPA buddy, You may find this usefuls, though this will not be used here.

We then enable JPA by adding the __@EnableJpaRepositories__ annotaion to the __ActorsApplication__ class.

```
@SpringBootApplication
@EnableJpaRepositories
public class ActorsApplication {
```

First we need to modify ArcCtor to be an entity bean which directly maps to the database table __actor__.

This is done by adding annotations
The class needs the annotation __@Entity__ the __actorId__ field needs __@Id__
annotation and a __@GeneratedValue__ annotation because this value is autogenertade by the database.
An __@Column__ annotation can be added to a field where the field name in the database is different from 
the field name in the entiuty class.

The Id will also need to be converted to an Integer so it can be null

Also because, in this simple application, The Entity bean is to be directy serialised out through the API we need to the annotatiuon
```
@JsonIgnoreProperties("hibernateLazyInitializer")
```
So the serialiser does not try to serialise internal class data that has been added by JPA.

Now the start of the class now looks like

```
@Entity(name = "actor")
@JsonIgnoreProperties("hibernateLazyInitializer")
public class Actor {
    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name = "actor_id")
    private Integer actorId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "last_update")
    private Date lastUpdate;

```


We can now create a JPA\_Repository object

This is an interface, jpa will automatically generate the required class.

This is very simple for some basic facilities.

In com.sparta.actors.services create an interface called ActorRepo
```
package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorRepo extends JpaRepository<Actor, Integer>{
}
```

It extends the JPARepository interface which contains reallyt usefull methods like save and get.

## Using the JPA Repository rather than JDBC


Modify __SakilaDAO__ so that it will use the __ActorRepo__ class instead of a JDBC connection.

This means removing a lot of lines of code, so rather than describing individual changes here is how the 
class will now look.

```
package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import com.sparta.actors.dataobjects.RequestActor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SakilaDAO {
    private final ActorRepo repo;

    public SakilaDAO(ActorRepo repo) {
        this.repo = repo;
    }

    public Actor getActor(int id) { // uses a prepared statement with an ID parameter
        return repo.getReferenceById(id);
    }
    public List<Actor> getaAllActors() {
        return repo.findAll();
    }

    public Actor createActors(RequestActor actorNoId) {
        Actor actor = new Actor();
        actor.setFirstName(actorNoId.getFirstName());
        actor.setLastName(actorNoId.getLastName());
        // note that the id has not been set and neither has the date, these are both generated in the database
        repo.saveAndFlush(actor);
        // when the actor is saved the autogenerated id will be set into the actor class
        // There is a problem here, the lastUpdated field is null because hibernate does not realise 
        // that it should read it from the database.
        return actor;
    }

}
```

You can now test this using __Swagger__ or __postman__.

