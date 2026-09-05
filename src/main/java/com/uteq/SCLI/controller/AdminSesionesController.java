// src/main/java/com/uteq/SCLI/controller/AdminSesionesController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.session.SessionTracker;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dashboard/admin/sesiones")
public class AdminSesionesController {

  private final SessionTracker tracker;

  public AdminSesionesController(SessionTracker tracker) {
    this.tracker = tracker;
  }

  @GetMapping
  public String listado(Model model) {
    model.addAttribute("items", tracker.listAll());
    return "dashboard/admin/sesiones";
  }

  @PostMapping("/kick")
  public String kick(@RequestParam("sessionId") String targetHttpSessionId,
                     HttpSession currentAdminSession) {

    // Resolver aquí el UUID de auditoría de la sesión objetivo (si se puede)
    String knownAuditUuid = tracker.resolveAuditUuid(targetHttpSessionId);

    // Pasar al tracker los 3 datos: sesión a expulsar, sesión del admin y el UUID ya resuelto
    tracker.kick(targetHttpSessionId, currentAdminSession.getId(), knownAuditUuid);

    return "redirect:/dashboard/admin/sesiones?ok=1";
  }
}