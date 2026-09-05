// src/main/java/com/uteq/SCLI/controller/CoordinadorDashboardController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.ReservaEspecialService;
import org.springframework.ui.Model;   
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CoordinadorDashboardController {


    private final ReservaEspecialService reservaEspecialService;

    public CoordinadorDashboardController(ReservaEspecialService reservaEspecialService) {
        this.reservaEspecialService = reservaEspecialService;
    }


    @GetMapping("/dashboard/coordinador")
    public String vista(HttpSession session, Model model) {
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us == null || us.getNombreRol() == null ||
            !us.getNombreRol().equalsIgnoreCase("coordinador")) {
            return "redirect:/login?error=permiso";
        }

        // Avisos para la tarjeta "Últimos avisos"
        model.addAttribute("avisos", reservaEspecialService.ultimas5Publicadas());
        return "dashboard/coordinador"; // thymeleaf
    }


}
