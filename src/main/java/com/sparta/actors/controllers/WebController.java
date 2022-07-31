package com.sparta.actors.controllers;

import com.sparta.actors.services.SakilaDAO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // assumes that this will return HTML and use the template provided (thymeleaf in this case)
public class WebController { //this is a list of web endpoints
    private SakilaDAO dao ; //this object is going to provide data using hibernate in this case
    public WebController(SakilaDAO dao) { //constructor
        this.dao = dao; // when
    }

    @GetMapping("/web/actors")
    public String getActors(Model model) {//when constructing the template we need a model that uses name value pairs
        model.addAttribute("actList", dao.getaAllActors()); // get values from dao and present to thymeleaf as actList
       return "actors"; //this returns the name of a template as found in the templates folder
   }
}
