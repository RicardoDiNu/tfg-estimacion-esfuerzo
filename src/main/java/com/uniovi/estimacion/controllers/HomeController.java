package com.uniovi.estimacion.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String getIndex() {
        return "home/index";
    }

    @GetMapping("/about")
    public String about() {
        return "home/about";
    }

    @GetMapping("/help")
    public String help() {
        return "home/help";
    }

}