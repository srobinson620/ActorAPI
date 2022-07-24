# ActorAPI
requires [sakila download] https://dev.mysql.com/doc/sakila/en/sakila-installation.html/EntityManager

When we use Spring Initializr to build our initial application it generates a single test
__ActorsApplicationTests.java__

This is a trivial test that just initialises the spring system, useful to run this initially, 
however it will try to initialise things that may have a dependancy outside of the code, that
is it may initialise JDBC or Hibernate and if the database is not available at test time which
is often the case this test is likely to fail so it should be removed.

```
$ rm src/test/java/com/sparta/actors/ActorsApplicationTests.java
```


## Adding Swagger to the API

First add the following dependancy to your pom.xml

```
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-boot-starter</artifactId>
            <version>3.0.0</version>
        </dependency>
```

which can be found by searching for __springfox boot starter__
by selecting __generate\>add dependency__ on the pom.xml menu.

Add a new class at __com.sparta.actors__ and call it __SwaggerConfig__

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
                        "com.sparta.actors.controllers")) // where to find API controllers for swagger to describe
                                                          // not this is a string so is easily set wrong it should
                                                          // be a package nameswagger will search it and all 
                                                          // packages within it for files annotated with
                                                          // @Controller or @RestController
                .paths(PathSelectors.any())
                .build().apiInfo(new ApiInfoBuilder()
                        .title("ACTORS-API")             // Title of swagger page
                        .description("""
                                    Demonstration of a simple API
                                    Using Spring boot
                                    to access mysql database sakila and it's table actors
                                    """)                // Description for swagger page
                        .version("1.0.0")
                        .build());
    }
}
```
In the above __@Bean__ followed by __public Docket api() {__ 
tells Spring that in order to create a Docket (should it find one needed, and it will when it starts up Swagger)
rather than just trying to call it's constructor call the annotated method in this case __SwaggerConfig.api()__.

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

for instance if you add 

```
@ApiOperation("Get Actor identified by actor_id specified in the path")
```

to the __actorDetails__ method in __ActorsController__

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

These will be added to the swagger outout as can be seen

http://localhost:8088/swagger-ui/#/actors-controller/actorDetailsUsingGET

obviously your application needs to be running locally on port 8088.


# Converting to using JPA and Hibernate

Hibernate is an instance of JPA, it is the default JPA type for spring boot.

First add JPA configuration to the __pom.xml__

search for __spring-boot-starter-data-jpa__ under the generate optiion in 
the __pom.xml__'s right click menu.

This should insert 

```
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
            <version>2.7.0</version>
        </dependency>
```

There is an intelliJ plugin called JPA buddy, You may find this useful, though this will not be used here.

First we need to modify __Actor__ to be an entity bean which directly maps to the database table __actor__.

This is done by adding annotations
The class needs the annotation __@Entity__ the __actorId__ field needs __@Id__
annotation and a __@GeneratedValue__ annotation because this value is autogenertade by the database.
An __@Column__ annotation can be added to a field where the field name in the database is different from 
the field name in the entiuty class.

The __Id__ will also need to be converted to an __Integer__ rather than __int__
so it can be null which we need it to be to trigger
the __autoincrement__ facility on MySql.

Also because, in this simple application, The Entity bean is to be directy serialised out through the API we need to the annotation
```
@JsonIgnoreProperties("hibernateLazyInitializer")
```
So the serialiser does not try to serialise internal class data that has been added by JPA.

In larger applications we would probably not directly serialise JAS data instead we would have
classes for sending data back to the user.

Now the start of the class now looks like:

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
_Note that the lastUpadate column has a problem, the value for this is autogenerated
inside MySql and there is no mechanism to notify JPA oif this fact, the
__@GeneratedValue__ annotation only works with __@Id__ fields and there can
only be one which must be a primary key. _

This is bacause the field is described as 

* Defaulting to CURRENT_TIMESTAMP
* on update CURRENT_TIMESTAMP

which will override any value which we put with the current timestamp on the server
partially this is because the __JPA__ tends to do more work than necessary and 
when it often creates an empty record then updates it, but in any case we would
still have the problem when we do updates.

We can now create a __JPA\_Repository__ object

This is an interface, JPA will automatically generate the required class.

This is very simple for the basic facilities we need at the moment.

In __com.sparta.actors.services__ create an interface called __ActorRepo__
```
package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorRepo extends JpaRepository<Actor, Integer>{
}
```

It extends the __JPARepository__ which is a generic taking two types, the first is the
Type of the Entity Object
and the second is the type of the primary key.
The generated class contains really useful methods.

In a bigger application you would create one of these for each Entity you have,
and we may add special methods with complex queries or functions.

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
Most of these methods are trivial:-
* The constructor set the __ACtorRepo__.
* getActor simply calls __getReferenceById__.
* getAllActors ccalls __findAll__.


Create actor is a little more complex.

The createActor method sets the first and last names into an __Actor__ object and saves it,
we have not set __actorId__ because it will be autogenerated and back written into the field, the field
__lastUpdated__ has also not set because 
it will be set inside the database, however the object we return will have this value as null as __JPA/Hibernate__ 
does not know it needs to read this field back. (see note above related to creation of the Entity).

This is a __JPA__ design issue, there is a lot of caching and until the __EntityManager__ (Which is the internal class that 
communicates with the Database in JDBC) is refreshed at the end of a transaction it will not go back to the database
to read this field.

We could have added a value to this field but it would not be correct as it would still be overriden by the database.

However a following transaction will read the right data, it will use a new __EntityManager Session__ automatically.

It is possible to correct this problem but it seemed to be out of scope of this small demonstration application, as
it would need us to take control of the __EntityManager__ or extend the __ActorRepo__ to access __EntityManager__ 
then create a refresh method or use raw JDBC which we are trying to avoid.

This sort of annoyance is why I personally do not have JPA as my first choice, it's too cunning.

You can now test this using __Swagger__ or __postman__.


# UNIT TESTS.

Unit tests are needed for all the classes in which we have functional code, so we can leave __ActorRepo__ as all the code is autogenerated.
There is little point in testing the data transfer objects (__Actor__ and __RequestActor__) as they are trivial and contain no function.

However we can and should test:-

* ActorsController
* SakilaDAO

Most of the tests written here will be happy path tests, in the real world we should also write tests to test
both the extremes of the happy path (smallest supported input largest supported input etc)  and all failing options.
This is really beyond the scope of a demonstration, though an odd one has been included to show the concept.

We will be using __Mockito__ to mock objects and __WebMvc__ to invoke the API end pointe. We are also using the
__assertThat__ static method from  AspectJ to verify values, ensure that the import statement is correct as there are 
a number of assertThat methods about, the one used has more facilities than most.

```
import static org.assertj.core.api.Assertions.assertThat;
```

### Invoking tests

Inside intellij you can right click on the project name in the Project pane on the left hand side (usually) and select
__Run All Tests__, or with a test class displayed you can right click within it and select __Run "xxx"__
where xxx is the class name to execute just the tests you are working on.

## Testing SakilaDAO

First create a test class __com.sparta.actors.services.SakilaDAOTest__ which is the same package as __SakilaDAO__
except that test packages are placed under __src/test/java__ rather than __src/main/java__.

The class can be automatically created in the right place by going to the __SakilaDAO__ class in InjelliJ right click and select 
__Generate__ then select __Test...__

A popup opens to select what you wish it to create:-

* __Junit5__ as the test library.
* __SakilaDAOTest__ as the name.
* superclass should be blank.
* Destination package, accept the defaulted value __com.sparta.actors.services__.
* tick setUp/*Before
* tick getActor, getAllActors and createActors.

The created class will look like:-

```
class SakilaDAOTest {
    @BeforeEach
    void setUp() {
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

We are going to mock __ActorRepo__ and we will need a couple of instances of __Actor__ so add them as class properties;

```
ActorsRepo repo;
Actor actor1;
Actor actor2;
```

We will need the instance to test so add it as another class property.

```
SakilaDAO inst;
```

We are also likely to need an actorId and a couple of names to use so it's easiest to create a 
set of constants

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

Now configure these within the __setUp__ method which will be automaticall run before each test 
as it has the __@BeforeEach__ annotation.

```
        repo = mock(ActorRepo.class);
        actor1 = new Actor(TEST_ID1, FIRST1, LAST1, D1);
        actor2 = new Actor(TEST_ID2, FIRST2, LAST2, D2);
        inst = new SakilaDAO(repo);
 ```

The first line creates a mock instance of __ActorRepo__, the second and third real instances of __Actor__
(there are arguments about weather a trivial none functional class should be used as is or mocked,
to me it really does not matter and, it is in this case easier to use the real thing, that may not
be the case if it contained many values)
the fourth creates a test instance of 
__SaklilaDAO__ set up with the mock repo created above.


### Test getActor

Within the getActor test method all we need to do is:

* setup the mock repo so that when __getReferenceById__ is called it returns an __Actor__.
* Check that the __Actio__n returned is the one expected
* chack that __getReferenceById__ was actually called.

This is done with the three lines.

```
        when(repo.getReferenceById(TEST_ID1)).thenReturn(actor1);

        assertThat(inst.getActor(TEST_ID1)).isEqualTo(actor1);
        verify(repo).getReferenceById(TEST_ID1);
```

* The first line says when __repo.getReferenceById__ is called with __TEST_ID1__
as it's parameter then return the Actor __actor1__.
* The second line says test that when __inst.getActor(TEST_ID1)__ is called that the returned value is __actor1__.
* The third line says verify that __getReferenceById__ was called with the parameter __TEST_ID1__.

### Test getAllActors

We are going to need a list to return, this can be generated using the method __Arrays.aslist()__ so the body of this test is:-

```
        List<Actor> list = asList(actor1, actor2);

        when(repo.findAll()).thenReturn(list);

        List<Actor> res = inst.getaAllActors();

        assertThat(res.size()).isEqualTo(2);
        assertThat(res).containsExactly(actor1, actor2);

        verify(repo).findAll();

```

* The first line creates a list.
* The second returns that list from the repo if __findAll__ is called.
* The third line runs the test.
* The next two lines chack that the returned entity is the expected list.
* The final line chacks that __findAll__ was called.


### Test create an Actor

The method __createActors__ will call repo's method __saveAndFlush__.

One of the complicated things here is that __saveAndFlush__ modifies the entity object it is given, so we need to mock this.
This is done with __doAnswer__ instead of __thenReturn__ it is basically the same except we can provide some code to
__doAnswer__.


Test method is we create is

```
        RequestActor reqAct = new RequestActor();
        reqAct.setFirstName(FIRST2);
        reqAct.setLastName(LAST2);

        when(repo.saveAndFlush(any())).thenAnswer(context->{
            Actor a = (Actor)context.getArgument(0);
            a.setActorId(TEST_ID2);
            return a;
        });
        Actor res = inst.createActors(reqAct);

        assertThat(res).extracting("actorId", "firstName", "lastName").contains(TEST_ID2, FIRST2, LAST2);
        verify(repo).saveAndFlush(any());
 ```

* The first 3 lines create the object we are sending.
* The next 4 lines mock the activity of __saveAndFlush__ in that it takes in
an __Actor__ then pokes the __actorId__ back into it in this case always with the value __TEST_ID2__.
** The thenAnswer takes a parameter which is a function, the parameter context contains information
about the passed in parameters the first argument will be an __Actor__ which needs an ID adding.
* We then assert that we get the right thing back and that the correct methods wer called.

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

        when(repo.saveAndFlush(any())).thenAnswer(context->{
            Actor a = (Actor)context.getArgument(0);
            a.setActorId(TEST_ID2);
            return a;
        });
        Actor res = inst.createActors(reqAct);

        assertThat(res).extracting("actorId", "firstName", "lastName").contains(TEST_ID2, FIRST2, LAST2);
        verify(repo).saveAndFlush(any());
    }
}
```

## Testing ActorsController

Testing the actor controller and it's related configuration can be done done using WebMvcTest which is a framework 
provided by the __spring-boot-starter-test__ which should already be included in your __pom.xml__.

This alows the methods of the controller to be invoked as though they had been excersized over the web.

The class can be automatically created in the right place by going to the __ActorsController__ class in InjelliJ right click and select
__Generate__ then select __Test...__

A popup opens to select what you wish it to create:-

* __Junit5__ as the test library.
* __ActorsControllerTest__ as the name.
* superclass should be blank.
* Destination package, accept the defaulted value __com.sparta.actors.services__.
* tick setUp/*Before but not tearDown/@After
* tick actorDetails(id:int), actorDetails(), basic(), addActor.

Annotate the constructed test class with 
```
@WebMvcTest(ActorsController.class)
```
This notifies that spring's WebMvcTest must be used to execute the tests in this class and identifies the class to be tested.

We will here use __@MockBean__ to create mocks and __@Autowired__ to create the test instance which is a MockMvc test environment.

We need to mock :-
* __SakilaDao__ as the unit we are testing __ActorControler__ will make calls to it.
* __ActorRepo__ because if we don't the spring test environment will try to start the database connection.

So we need to  create class variables like:-

```
    @MockBean
    ActorRepo repo;
    @MockBean
    SakilaDAO dao;
    @Autowired
    MockMvc mvc;
```

We will also need some test data, so I'm going to create two Actor instances populated with constants,
the actors are actually constructed in the methodn __setUp__ annotated with __@BeforeEach__.


```
    Actor actor1;
    Actor actor2;

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
        actor1 = new Actor(TEST_ID1, FIRST1, LAST1, D1);
        actor2 = new Actor(TEST_ID2, FIRST2, LAST2, D2);
    }
 
```

### Testing the __/__ end point

As designed GET calls to __/__ return a fixed string __\<h1\>Hello\</h1\>__.

The method that performs this function is called __basic()__ so we have a method in the test class called __basic()__.

This is the test code we will write to test this.

```
    @Test
    void basic() throws Exception {
        MvcResult res = mvc.perform(get("/"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        assertThat(res.getResponse().getContentAsString()).isEqualTo("\<h1\>Hello\</h1\>");
        verifyNoInteractions(repo, dao);
    }
```

* The __@Test__  means this is a test method.
* The method signature contains __throws Exception__ because mvc.perform can throw Exception.
* __mvc.perform(get("/"))__ means execute a get to the url __/__ 
* __.andExpect(status().is2xxSuccessful())__ means check that the result status is in the range 200 to 299.
* __.andReturn()__ means return a structure describing the call that has just completed it is put into the variable __res__.

We can then check that the returned text is __\<h1\>Hello\</h1\>__ using the line

```
        assertThat(res.getResponse().getContentAsString()).isEqualTo("<h1>Hello</h1>");
```

Neither of the mocks shouild have been called so the line
```
        verifyNoInteractions(repo, dao);
```
shows this, if either had been called an error would be generated.

### Testing the __/actor/{id}__ end point.

We need to setup the __SakliaDao mock__ to return __actor2__ if the method __getActor__
is called with parameter __TEST_ID2__.

```
        when(dao.getActor(TEST_ID2)).thenReturn(actor2);
```

We can then do the call 

```
MvcResult res = mvc.perform(get("/actor/{id}", TEST_ID2))
                .andReturn();
```
This does the GET to /actor/77 (77 being the value of TEST_ID2) and returns a structure as res.

```
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
```
Checks that the response status is 200.
```
        assertThat(res.getResponse().getContentType()).isEqualTo("application/json");
```
Checks that the returned content type id application/json.

```
        var jsonConverter = new ObjectMapper();
        Actor resData = jsonConverter.readValue(res.getResponse().getContentAsString(), Actor.class);
```

* jsonConverter is a Jackson object mapper that can be used for converting objects to and from json strings.
** In this case it converts the received body to an Actor.
** If it is not correct an error message will be generated.
** If this is not an error then resData is now the received Actor.

We than test that the received actor is correct.

```
        assertThat(resData)
                .extracting("actorId", "firstName", "lastName")
                        .containsExactly(TEST_ID2, FIRST2, LAST2);
```
by extracting 3 field and comparing against the expected values.

```
        verify(dao).getActor(TEST_ID2);
        verifyNoInteractions(repo);
```

We know that getActor should have been called to get this data so
* The first of these two lines verifies that that method was called.
* The second line verifies that repo was never called.

We can also test for what happens if an invalid __actor\_id__ is presented, we expect it to generatea 400 __BAD\_REQUEST__ response.

The following very simple test method does this
```
    @Test
    void actorDetailsAlphaActorId() throws Exception {
        mvc.perform(get("/actor/{id}", "ABCDE")).andExpect(status().isBadRequest());
    }
    
```
We should also include tests to check what happens if the given actor\_id is valid but unknown however we know that the code as
it stands returns a 500 error rather than the expected 404, this is therefore technical debt to be fixed at a future times,
this requirement should be recorded in trello for future correction.

### Testing the __/actors__ end point.

The __/actors__ end point  is also supported by an overloaded method also called actorDetails
so Intelij created a test method called __actorDetailsTest__ to test this, noting this it would
probably be better to rename the mthod to getAllActors or similar, however we will considder that
technical debt for the future, it does not matter for now.

First we need to create test data, the __getAllActors__ wethod of __dao__ needs to return a list
of Actor, so
```
        List<Actor> allActors = asList(actor1, actor2); // create a list of 2 actors
        when(dao.getaAllActors()).thenReturn(allActors);
```

We can now invoke the end point with
```
        MvcResult res = mvc.perform(get("/actors"))
                .andReturn();
```
Which performs the __GET__ call and sets __res__ to be the webmvc data structure which can be tested.

First check that we got 200 status and content type of application/json.
```
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentType()).isEqualTo("application/json");
```

Now use Jackson to parse the recived body into a list of Maps, we could make it return a
list of Actor objects but that is more complex, at this moment I considder it unnecessarily complex for this small test.

```
        var jsonConverter = new ObjectMapper();
        List<Map<String, String>> resData = jsonConverter.readValue(res.getResponse().getContentAsString(), List.class);
```

We can then test the contents of the receieved list

```
        assertThat(resData).hasSize(2);
        assertThat(resData)
                .extracting("actorId", "firstName", "lastName")
                .containsExactly(tuple(TEST_ID1, FIRST1, LAST1), tuple(TEST_ID2, FIRST2, LAST2));
```

This form of the assertJ's assertThat ... extract ... caontainsExactly is very tidy way of testing array contents, 
I like it because it is consice and explicit, not everyone does.

Finally as with all the other tests we check that the data came from the correct mock calls.

```
        verify(dao).getaAllActors();
        verifyNoInteractions(repo);
```

### Testing the __PUT /actor__ end point

The __PUT /actor__ end point creates a new actor the addActor method is called.

We will now fill in the test method __addActor__ 

First we setup __dao.createActors__ to return __actor1__ if ever called, we cannot predetermin what will be passed to this 
as it is generated by the spring mvc framework so we have to accept any parameter, the style used here stores
whatever parameters are sent into a list called createActorParams. There are other ways to achieve this notably a
facility call Captor, but in this case this methoid is being used.

```
        List<RequestActor> createActorParams = new ArrayList<>();
        when(dao.createActors(any())).thenAnswer(context ->{
            createActorParams.add((RequestActor)context.getArgument(0));
            return actor1;
        });
 ```
We will need to construct a body string to send, this is done here using Jackson to turn a map into a JSON string.
```
        Map<String, String> requestData = Map.of("lastName", LAST1, "firstName", FIRST1);
        var jsonConverter = new ObjectMapper();
        String body = jsonConverter.writeValueAsString(requestData);
```


We then perform the put
```
        MvcResult res = mvc.perform(put("/actor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
```

The fragment
```
             put("/actor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
```
Generates the __PUT__ call with __application/json__ content type and the previously generated __JSON body__ string as the body.

The return status is checked for being between 200 and 299 then the recieved data is stored as res which we can then test.

```
        Actor resBody = jsonConverter.readValue(res.getResponse().getContentAsString(), Actor.class);
```

This converts the recieved body into an __Actor__ using the previously created Jackson ObjectMapper.

We check the received data is correct with
```
        assertThat(resBody)
                .extracting("actorId", "firstName", "lastName")
                .containsExactly(actor1.getActorId(), actor1.getFirstName(), actor1.getLastName());
```
We check that dao.createActor was called with the correct parameter using
```
        assertThat(createActorParams).hasSize(1);
        assertThat(createActorParams.get(0))
                .extracting("firstName", "lastName")
                .containsExactly(FIRST1, LAST1);
```
Which checks that the RequestActor stored in createActorParams object is what would be expected.

Finally there are two tests that test error conditions.

#### error condition tests

The first name must be provided and must have a length greater than 1, here we simply provide
a blank first name in the test __addActorBlankFirstName__ and do not provide a first name 
in __addActorMissingFirstName__ in both cases if the __@Valid__, __@NotNull__ and __@Size__
annotations used in the class under test __ActorsController__ and the related data transfer object 
__RequestActor__ are correct a __400 BAD_REQUEST__ status will be returned.

```
    @Test
    void addActorBlankFirstName() throws Exception {
        Map<String, String> requestData = Map.of("lastName", LAST1, "firstName", "");
        var jsonConverter = new ObjectMapper();
        String body = jsonConverter.writeValueAsString(requestData);
        mvc.perform(put("/actor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
    @Test
    void addActorMissingFirstName() throws Exception {
        Map<String, String> requestData = Map.of("lastName", LAST1);
        var jsonConverter = new ObjectMapper();
        String body = jsonConverter.writeValueAsString(requestData);
        mvc.perform(put("/actor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
```

__Thats all folks__

