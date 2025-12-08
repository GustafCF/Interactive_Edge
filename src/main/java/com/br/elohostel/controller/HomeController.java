package com.br.elohostel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/guest")
    public String identity() {
        return "guest";
    }
    
    @GetMapping("/room")
    public String room() {
        return "room";
    }
    
    @GetMapping("/reserve")
    public String reserve() {
        return "reserve";
    }

    @GetMapping("/calendario")
    public String calendario() {
        return "calendario";
    }

    @GetMapping("/room-calendar")
    public String roomCalender() {
        return "room-calendar";
    }

    @GetMapping("/airbnb-setup")
    public String airbnbSetup() {
        return "airbnb-setup";
    }

    @GetMapping("/calendar-airbnb-setup")
    public String calendarAirbnb() {
        return "calendar-airbnb-setup";
    }

    @GetMapping("/financas") 
    public String financas() {
        return "financial";
    }
}