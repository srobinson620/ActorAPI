package com.sparta.actors.services;

import com.sparta.actors.dataobjects.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository //hibernate repository for actor - this interface provides
// a set of methods that allow hibernate to access the database
public interface ActorRepo extends JpaRepository<Actor, Integer>{
    //this interface provides some methods for the DAO and in this case
    // says the Repository (DAO in this case) has an Integer key
    // every entity/table in the database requires a @Repository annotation
    // hibernate can automate this but it is less flexible
}
