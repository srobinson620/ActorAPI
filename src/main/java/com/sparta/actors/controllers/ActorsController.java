package com.sparta.actors.controllers;

import com.sparta.actors.dataobjects.Actor;
import com.sparta.actors.dataobjects.RequestActor;
import com.sparta.actors.services.SakilaDAO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

@RestController
@Validated
public class ActorsController {
    SakilaDAO dao; // the data access object that the controller will use

    public ActorsController(SakilaDAO dao) { // instantiate with DAO
        this.dao = dao;
    }

    @GetMapping("/actor/{id}") // the get mapping links a data provider to a data request
    public Actor actorDetails(@PathVariable int id){
        return  dao.getActor(id);
        //new Actor(id,"Fred","Bloggs",new Date());
    }
    @GetMapping("/actors")
    public List<Actor> actorDetails(){
        return dao.getaAllActors();
    }
    @GetMapping("/")
    public String basic(){
        return "<h1>Hello</h1>";
    }
    @PutMapping("/actor")
    public Actor addActor(@Valid @RequestBody RequestActor actorNoId){
        return dao.createActors(actorNoId);
    }

}
