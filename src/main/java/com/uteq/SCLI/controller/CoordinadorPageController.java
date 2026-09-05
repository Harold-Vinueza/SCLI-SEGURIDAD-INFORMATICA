package com.uteq.SCLI.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CoordinadorPageController {

    @GetMapping("/coordinador/horarios")
    public String horarios() {
        return "coordinador/horarios";
    }

    @GetMapping("/coordinador/solicitudes")
    public String solicitudes() {
        return "coordinador/solicitudes";
    }
}
