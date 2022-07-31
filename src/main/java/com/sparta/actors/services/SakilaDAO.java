package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import com.sparta.actors.dataobjects.RequestActor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SakilaDAO {
    private final ActorRepo repo; //An object instance based on JPARepository Interface
    // that will provide hibernate access to the database (actor table in this case)
    public SakilaDAO(ActorRepo repo) {
        this.repo = repo;
    }
    public Actor getActor(int id) { // uses a prepared statement with an ID parameter
        return repo.getReferenceById(id);
    } //matches with what the controller maps
    public List<Actor> getaAllActors() {
        return repo.findAll();
    } // used in both web and actors controller
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
