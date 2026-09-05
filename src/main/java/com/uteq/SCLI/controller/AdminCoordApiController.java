package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.AprobacionDTO;              // <-- DTO externo con labsMateria
import com.uteq.SCLI.service.AdminCoordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coord")
public class AdminCoordApiController {

  private final AdminCoordService service;

  @GetMapping("/solicitudes")
  public List<Map<String,Object>> listar(@RequestParam(required = false) String estado) {
    return service.listar(estado);
  }

  @GetMapping("/solicitudes/{id}/detalles")
  public List<Map<String,Object>> detalles(@PathVariable Integer id) {
    return service.detalles(id);
  }

  // Labs disponibles (para poblar el combo por fila)
  @GetMapping("/labs-disponibles")
  public List<Map<String,Object>> labsDisponibles(@RequestParam Integer idSolicitud,
                                                  @RequestParam Integer idHorario) {
    return service.labsDisponibles(idSolicitud, idHorario);
  }

  // Aprobar con la selección de labs (por-horario y por-materia)
  @PostMapping("/solicitudes/{id}/aprobar")
public ResponseEntity<Map<String,Object>> aprobar(@PathVariable Integer id,
                                                  @RequestBody AprobacionDTO dto) {
  try {
    var r = service.aprobar(id, dto);
    // Siempre 200: si fue parcial, el body tendrá parcial=true
    return ResponseEntity.ok(r);
  } catch (IllegalStateException ex) {
    // Solo casos realmente excepcionales (sin período activo, etc.)
    return ResponseEntity.status(409).body(Map.of("ok", false, "motivo", ex.getMessage()));
  }
}


  @PostMapping("/solicitudes/{id}/rechazar")
  public Map<String,Object> rechazar(@PathVariable Integer id, @RequestBody ObsDTO body) {
    service.rechazar(id, body == null ? null : body.observaciones);
    return Map.of("ok", true, "idSolicitud", id);
  }

  @PostMapping("/solicitudes/{id}/proponer")
  public Map<String,Object> proponer(@PathVariable Integer id, @RequestBody PropuestaDTO body) {
    return service.proponer(id,
        body == null ? null : body.getObservaciones(),
        body == null ? List.of() : body.getCeldas());
  }

  // ===== DTOs request simples =====
  @Data public static class ObsDTO { String observaciones; }
  @Data public static class PropuestaDTO {
    String observaciones;
    List<Map<String,Object>> celdas; // [{idHorario, idMateria, idDocente?}]
  }
}
