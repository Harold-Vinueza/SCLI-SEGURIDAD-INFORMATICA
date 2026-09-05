package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.CoordinadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CatalogosApiController {

  private final CoordinadorService service;
  private final UserSession userSession;

  /** Carreras del coordinador que está logueado */
  @GetMapping("/admin/carreras-mias")
  public ResponseEntity<List<Map<String,Object>>> carrerasMias() {
    if (userSession == null || userSession.getIdPersona() == null) {
      return ResponseEntity.status(401).build();
    }
    var rows = service.carrerasDelCoordinador(userSession.getIdPersona());
    var out = new ArrayList<Map<String,Object>>();
    for (Object[] r : rows) {
      Map<String,Object> m = new HashMap<>();
      m.put("id", ((Number) r[0]).intValue());
      m.put("nombre", (String) r[1]);
      out.add(m);
    }
    return ResponseEntity.ok(out);
  }

  /** Slots de horario por jornada. Ej: /api/horarios?jornada=Vespertina */
  @GetMapping("/horarios")
  public ResponseEntity<List<Map<String,Object>>> horarios(@RequestParam(required = false) String jornada) {
    var rows = service.horariosPorJornada(jornada);
    var out = new ArrayList<Map<String,Object>>();
    for (Object[] r : rows) {
      Map<String,Object> m = new HashMap<>();
      m.put("id_horario", ((Number) r[0]).intValue());
      m.put("jornada",     r[1]);
      m.put("dia_semana",  r[2]);
      m.put("hora_inicio", r[3]);   // 'HH24:MI'
      m.put("hora_fin",    r[4]);
      out.add(m);
    }
    return ResponseEntity.ok(out);
  }
}
