package com.uniovi.estimacion.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String getIndex() {
        return "home/index";
    }

    @GetMapping("/home")
    public String getHome() {
        return "home/private";
    }
}