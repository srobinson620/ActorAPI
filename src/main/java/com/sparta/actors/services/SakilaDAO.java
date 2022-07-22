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
    private DataSource datasource;
    public SakilaDAO(DataSource ds){
        this.datasource=ds;
    }
    public Actor getActor(int id) { // uses a prepared statement with an ID parameter
        // to get one single actor record
        try (Connection conn = datasource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM Actor WHERE actor_Id = ?")) {
                ps.setInt(1, id); //setInt replaces the ? parameter in the Prepared Statement
               try (ResultSet rs = ps.executeQuery()){
                   if (rs.next()){
                       return new Actor(rs.getInt("actor_id"),rs.getString("first_name"),
                               rs.getString("last_name"), rs.getDate("last_update"));
                   }
                   else throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
               }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);//Spring will handle this exception
        }
    }

    public List<Actor> getaAllActors() {
        try (Connection conn = datasource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM Actor")) {
                try (ResultSet rs = ps.executeQuery()){
                    List<Actor> actorList = new ArrayList<>();
                    while (rs.next()){
                        actorList.add( new Actor(rs.getInt("actor_id"),rs.getString("first_name"),
                                rs.getString("last_name"), rs.getDate("last_update")));
                    }
                    return actorList;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);//Spring will handle this exception
        }
    }

    public Actor createActors(RequestActor actorNoId) {
        try (Connection conn = datasource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Actor (first_name, last_name, last_update) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                //the above extra parameter where an autogeneration of a key occurs will return the generated key
                ps.setString(1, actorNoId.getFirstName());
                ps.setString(2, actorNoId.getLastName());
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()){ //there is a result set because we used generated keys - it will contain the new actor ID

                    if (rs.next()){ //if the new key record exists, its id will be available here
                        return getActor(rs.getInt(1)); // only one field and only one record and we will return the Actor with this newly generated ID
                    }
                    else throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);//Spring will handle this exception
        }
    }
}
