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


# UNIT TESTS.

Unit tests are needed for all the classes in which we have code, so we can leave the ActorRepo as all the code is autogenerated.
There is little point in testing ithe data transfer objects (Actor and RequestActor) as they are trivial and contains no function.

However we can test:-

* ActorsController
* SakilaDAO

We will be using Mockito to mock objects and WebMvc to invoke the API end points in a test mode.

## Testing SakilaDAO

So we first create a test class __com.sparta.actors.services.SakilaDAOTest__ which is the same package as __SakilaDAO__
except that test packages are placed under __src/test/java__ rather than __src/main/java__.

The class can be automatically created in the right place by going to the SakilaDAO class in InjelliJ right click and select 
__Generate__ then select __Test...__

A popup opens to select what you wish it to create:-

* __Junit5__ as the test library.
* __SakilaDAOTest__ as the name.
* superclass should be blank.
* Destination package, accept the defaulted value __com.sparta.actors.services__.
* tick setUp/*Before and tearDown/@After
* tick getActor, getAllActors and createActors.

The created class will look like:-

```
class SakilaDAOTest {
    @BeforeEach
    void setUp() {
    }
    @AfterEach
    void tearDown() {
    }
    @Test
    void getActor() {
    }
    @Test
    void getaAllActors() {
    }
    @Test
    void createActors() {
    }
}
```

We are going to mock __ActorRepo__ and a couple of instances of __Actor__ so add them as class properties;

```
ActorsRepo repo;
Actor actor1;
Actor actor2;
```

We will need the instance to test so add it as another class property as Autowired so spring will construct it.

```
SakilaDAO inst;
```

We are also likely to need an actorId and a couple of names to use so it's easiest to create to create a constant

```
    static final int TEST_ID1 = 23;
    static final int TEST_ID2 = 77;
    static final String FIRST1="Freddy";
    static final String LAST1="Bloggs";
    static final String FIRST2="MARY";
    static final String LAST2="SMITH";

    static final Date D1 = new Date(2020, 1, 1);
    static final Date D2 = new Date(2020, 2, 2);
```

Now configure these within the __setUp__ method which will be run before each test.

```
        repo = mock(ActorRepo.class);
        actor1 = new Actor(TEST_ID1, FIRST1, LAST1, D1);
        actor2 = new Actor(TEST_ID2, FIRST2, LAST2, D2);
        inst = new SakilaDAO(repo);
 ```

The first line creates a mock instance of __ActionRepo__, the second and third mock instances of __Action__,
the fourth creates a test instance of 
__SaklilaDAO__ set up with the mock repo created above.


#### Test getActor

Within the getActor test method all we need to do is:

* setup the mock repo so that when getReferenceById is called it returns an Action
* Check that the Action returned is the one expected
* chack that it was actually called.

This is done with the three lines.

```
        when(repo.getReferenceById(TEST_ID1)).thenReturn(actor1);

        assertThat(inst.getActor(TEST_ID1)).isEqualTo(actor1);
        verify(repo).getReferenceById(TEST_ID1);
```




The first line says when repo.getReferenceById is called with TEST_ID1 as it's parameter then return the Action __action1__.

The second line says test that when inst.getActor(TEST_ID1) is called that the returned value is __action1__.

The third line says verify that getReferenceById was called with the parameter TEST_ID1.

#### Test getAllActors

We are going to need a list to return, this can be generated using the method __Arrays.aslist()__ so the body of this test is:-

```
        List<Actor> list = asList(actor1, actor2);

        when(repo.findAll()).thenReturn(list);

        List<Actor> res = inst.getaAllActors();

        assertThat(res.size()).isEqualTo(2);
        assertThat(res).containsExactly(actor1, actor2);

        verify(repo).findAll();

```

The first line creates a list, the second returns that list from the repo if __findAll__ is called.

The third line runs the test.

The next two lines chack that the returned entity is the expected list.

and teh final line chacks that __findAll__ was called.


#### Test create an Actor

This is the final method createActors


The method __createActors__ will call repo's methods __saveAndFlush__ and __getReferenceById__.

One of the complicated things here is that __saveAndFlush__ modifies the entity object it is given, so we need to mock this action.
This is done with doAnswer instead of thenReturn within which we can put some code.


so the test method is now:

```
        RequestActor reqAct = new RequestActor();
        reqAct.setFirstName(FIRST2);
        reqAct.setLastName(LAST2);

        when(repo.getReferenceById(TEST_ID2)).thenReturn(actor2);

        when(repo.saveAndFlush(any())).thenAnswer(context->{
            Actor a = (Actor)context.getArgument(0);
            a.setActorId(TEST_ID2);
            return a;
        });
        Actor res = inst.createActors(reqAct);

        assertThat(res).extracting("actorId", "firstName", "lastName").contains(TEST_ID2, FIRST2, LAST2);
        verify(repo).saveAllAndFlush(any());
        verify(repo).getReferenceById(TEST_ID2);
 ```

The first 3 lines create the object we are sending the next line mocks the getReferenceById call which will be called at the end
to retrieve the just saved entity.

The next 4 lines mock the activity of saveAndFlush in that it takes in an Action then pokes the actorId back into it.

the thenAnswer takes a parameter which is a lambda, the parameter context contains information about the passed in parameters
the first argument will be an Action which needs an ID adding.

we then assert that we get the right thing back and that the correct methods wer called.

The final class is

```
package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import com.sparta.actors.dataobjects.RequestActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat; // CHECK THIS THERE ARE MULTIPLE asserThat methods
                                                          // available you need this one.
import static org.mockito.Mockito.*;

class SakilaDAOTest {
    ActorRepo repo;
    Actor actor1;
    Actor actor2;

    SakilaDAO inst;

    static final int TEST_ID1 = 23;
    static final int TEST_ID2 = 77;
    static final String FIRST1="Freddy";
    static final String LAST1="Bloggs";
    static final String FIRST2="MARY";
    static final String LAST2="SMITH";

    static final Date D1 = new Date(2020, 1, 1);
    static final Date D2 = new Date(2020, 2, 2);

    @BeforeEach
    public void setUp(){
        repo = mock(ActorRepo.class);
        actor1 = new Actor(TEST_ID1, FIRST1, LAST1, D1);
        actor2 = new Actor(TEST_ID2, FIRST2, LAST2, D2);
        inst = new SakilaDAO(repo);
    }

    @Test
    void getActor() {
        when(repo.getReferenceById(TEST_ID1)).thenReturn(actor1);

        assertThat(inst.getActor(TEST_ID1)).isEqualTo(actor1);
        verify(repo).getReferenceById(TEST_ID1);
    }
    @Test
    void getaAllActors() {
        List<Actor> list = asList(actor1, actor2);

        when(repo.findAll()).thenReturn(list);

        List<Actor> res = inst.getaAllActors();

        assertThat(res.size()).isEqualTo(2);
        assertThat(res).containsExactly(actor1, actor2);

        verify(repo).findAll();
    }
    @Test
    void createActors() {
        RequestActor reqAct = new RequestActor();
        reqAct.setFirstName(FIRST2);
        reqAct.setLastName(LAST2);

        when(repo.getReferenceById(TEST_ID2)).thenReturn(actor2);

        when(repo.saveAndFlush(any())).thenAnswer(context->{
            Actor a = (Actor)context.getArgument(0);
            a.setActorId(TEST_ID2);
            return a;
        });
        Actor res = inst.createActors(reqAct);

        assertThat(res).extracting("actorId", "firstName", "lastName").contains(TEST_ID2, FIRST2, LAST2);
        verify(repo).saveAllAndFlush(any());
        verify(repo).getReferenceById(TEST_ID2);
    }
}
```

