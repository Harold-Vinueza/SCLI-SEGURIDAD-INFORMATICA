package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping({"/", ""})
    public String root(HttpSession session) {
        UserSession us = (UserSession) session.getAttribute("userSession");
        if (us != null && us.getNombreRol() != null) {
            return "redirect:/dashboard"; // lo atrapará el router de arriba
        }
        return "redirect:/login";
    }
}
