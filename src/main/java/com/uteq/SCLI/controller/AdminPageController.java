package com.uteq.SCLI.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

   // URL pública
  @GetMapping("/admin/solicitudes-coordinacion")
  public String solicitudesCoord() {
    // nombre lógico de la vista (classpath:/templates/admin/solicitudes-coord.html)
    return "admin/solicitudes-coord";
  }
}
