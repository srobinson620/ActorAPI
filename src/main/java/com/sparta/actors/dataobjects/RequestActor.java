package com.sparta.actors.dataobjects;

import org.springframework.lang.NonNull;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

public class RequestActor { // no constructor as Spring will just use the getters and setters
    @NotNull // will ensure 400 error produced if this data is missed
    @Size(min=1,max=40)//
    private String firstName;
    @NotNull
    @Size(min=1,max=40)
    private String lastName;

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}