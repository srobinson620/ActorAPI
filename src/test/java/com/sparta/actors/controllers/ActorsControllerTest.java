package com.sparta.actors.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.actors.dataobjects.Actor;
import com.sparta.actors.dataobjects.RequestActor;
import com.sparta.actors.services.ActorRepo;
import com.sparta.actors.services.SakilaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActorsController.class)
class ActorsControllerTest {

    @MockBean
    ActorRepo repo;
    @MockBean
    SakilaDAO dao;
    @Autowired
    MockMvc mvc;

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

    @Test
    void basic() throws Exception {
        MvcResult res = mvc.perform(get("/"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        assertThat(res.getResponse().getContentAsString()).isEqualTo("<h1>Hello</h1>");
        verifyNoInteractions(repo, dao);
    }    @Test

    void actorDetails() throws Exception {
        when(dao.getActor(TEST_ID2)).thenReturn(actor2);
        MvcResult res = mvc.perform(get("/actor/{id}", TEST_ID2))
                .andReturn();
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentType()).isEqualTo("application/json");
        var jsonConverter = new ObjectMapper();
        Actor resData = jsonConverter.readValue(res.getResponse().getContentAsString(), Actor.class);

        assertThat(resData)
                .extracting("actorId", "firstName", "lastName")
                        .containsExactly(TEST_ID2, FIRST2, LAST2);

        verify(dao).getActor(TEST_ID2);
        verifyNoInteractions(repo);
    }

    @Test
    void actorDetailsAlphaActorId() throws Exception {
        mvc.perform(get("/actor/{id}", "ABCDE")).andExpect(status().isBadRequest());
    }

    @Test
    void actorDetailsTest() throws Exception {
        List<Actor> allActors = asList(actor1, actor2); // create a list of 2 actors
        when(dao.getaAllActors()).thenReturn(allActors);
        MvcResult res = mvc.perform(get("/actors"))
                .andReturn();
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentType()).isEqualTo("application/json");
        var jsonConverter = new ObjectMapper();
        List<Map<String, String>> resData = jsonConverter.readValue(res.getResponse().getContentAsString(), List.class);

        assertThat(resData).hasSize(2);
        assertThat(resData)
                .extracting("actorId", "firstName", "lastName")
                .containsExactly(tuple(TEST_ID1, FIRST1, LAST1), tuple(TEST_ID2, FIRST2, LAST2));
        verify(dao).getaAllActors();
        verifyNoInteractions(repo);
    }

    @Test
    void addActor() throws Exception {
        List<RequestActor> createActorParams = new ArrayList<>();
        when(dao.createActors(any())).thenAnswer(context ->{
            createActorParams.add((RequestActor)context.getArgument(0));
            return actor1;
        });
        Map<String, String> requestData = Map.of("lastName", LAST1, "firstName", FIRST1);
        var jsonConverter = new ObjectMapper();
        String body = jsonConverter.writeValueAsString(requestData);
        MvcResult res = mvc.perform(put("/actor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        Actor resBody = jsonConverter.readValue(res.getResponse().getContentAsString(), Actor.class);
        assertThat(resBody)
                .extracting("actorId", "firstName", "lastName")
                .containsExactly(actor1.getActorId(), actor1.getFirstName(), actor1.getLastName());
        verifyNoInteractions(repo);
        assertThat(createActorParams).hasSize(1);
        assertThat(createActorParams.get(0))
                .extracting("firstName", "lastName")
                .containsExactly(FIRST1, LAST1);
    }
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