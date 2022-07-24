package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import com.sparta.actors.dataobjects.RequestActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
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

        assertThat(res)
                .extracting("actorId", "firstName", "lastName")
                .contains(TEST_ID2, FIRST2, LAST2);
        verify(repo).saveAndFlush(any());
    }
}