// src/main/java/com/uteq/SCLI/controller/CoordinadorApiController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.dto.coordinador.NuevaSolicitudDTO;
import com.uteq.SCLI.dto.coordinador.SolicitudCreadaDTO;
import com.uteq.SCLI.dto.coordinador.SolicitudDetalleDTO;
import com.uteq.SCLI.dto.coordinador.SolicitudItemDTO;
import com.uteq.SCLI.service.CoordinadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coordinador")
public class CoordinadorApiController {

  private final CoordinadorService service;
  private final UserSession userSession;

  // ------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------
  private boolean esCoord() {
    return userSession != null
        && userSession.getNombreRol() != null
        && userSession.getNombreRol().equalsIgnoreCase("coordinador");
  }

  // ------------------------------------------------------------
  // Catálogos
  // ------------------------------------------------------------

  // Materias (opcional: ?idCarrera=)
  @GetMapping("/materias")
  public List<Map<String, Object>> materias(@RequestParam(required = false) Integer idCarrera) {
    var rows = service.materiasPorCarrera(idCarrera);
    var out = new ArrayList<Map<String, Object>>();
    for (Object[] r : rows) {
      Map<String, Object> m = new HashMap<>();
      m.put("id",     ((Number) r[0]).intValue());
      m.put("codigo", r[1]);
      m.put("nombre", r[2]);
      out.add(m);
    }
    return out;
  }

  // Docentes por materia (?idMateria=)
  @GetMapping("/docentes-por-materia")
  public List<Map<String, Object>> docentes(@RequestParam Integer idMateria) {
    var rows = service.docentesPorMateria(idMateria);
    var out = new ArrayList<Map<String, Object>>();
    for (Object[] r : rows) {
      Map<String, Object> m = new HashMap<>();
      m.put("id",    ((Number) r[0]).intValue());
      m.put("label", r[1]);
      out.add(m);
    }
    return out;
  }

  // Laboratorios disponibles para un slot (combo del modal)
  // ?idCarrera=...&idHorario=...
  @GetMapping("/labs-disponibles")
  public ResponseEntity<List<Map<String, Object>>> labsDisponibles(
      @RequestParam Integer idCarrera,
      @RequestParam Integer idHorario
  ) {
    if (!esCoord()) return ResponseEntity.status(403).build();
    return ResponseEntity.ok(service.labsDisponiblesMap(idCarrera, idHorario));
  }

  // Ocupación de la jornada (para pintar en la grilla)
  // ?jornada=Matutina|Vespertina|Nocturna
  @GetMapping("/ocupacion")
  public ResponseEntity<List<int[]>> ocupacion(@RequestParam String jornada) {
    if (!esCoord()) return ResponseEntity.status(403).build();
    return ResponseEntity.ok(service.ocupacionJornada(jornada));
  }

  // ------------------------------------------------------------
  // Solicitudes
  // ------------------------------------------------------------

  // Crear solicitud con múltiples celdas/slots
  @PostMapping("/solicitudes")
  public ResponseEntity<SolicitudCreadaDTO> crear(@RequestBody NuevaSolicitudDTO body) {
    if (!esCoord()) return ResponseEntity.status(403).build();
    return ResponseEntity.ok(service.crearSolicitud(body));
  }

  // Listar solicitudes (?estado=Pendiente|Aprobada|Rechazada|Anulada)
  @GetMapping("/solicitudes")
  public ResponseEntity<List<SolicitudItemDTO>> listar(@RequestParam(required = false) String estado) {
    if (!esCoord()) return ResponseEntity.status(403).build();
    return ResponseEntity.ok(service.listarSolicitudes(estado));
  }

  // Detalles de una solicitud
  @GetMapping("/solicitudes/{id}/detalles")
  public ResponseEntity<List<SolicitudDetalleDTO>> detalles(@PathVariable("id") Integer id) {
    if (!esCoord()) return ResponseEntity.status(403).build();
    try {
      return ResponseEntity.ok(service.detallesSolicitud(id));
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(403).build();
    }
  }

  // Anular una solicitud (si está Pendiente)
  @PatchMapping("/solicitudes/{id}/anular")
  public ResponseEntity<Map<String, Object>> anular(@PathVariable("id") Integer id) {
    if (!esCoord()) return ResponseEntity.status(403).build();
    try {
      boolean ok = service.anular(id);
      Map<String, Object> resp = Map.of(
          "idSolicitud", id,
          "ok", ok,
          "mensaje", ok ? "Solicitud anulada" : "No se pudo anular (¿ya no está Pendiente?)"
      );
      return ResponseEntity.ok(resp);
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(403).build();
    }
  }


  // Labs por carrera (para seleccionar ANTES de cargar la grilla)
@GetMapping("/labs-carrera")
public ResponseEntity<List<Map<String,Object>>> labsCarrera(@RequestParam Integer idCarrera){
  if (!esCoord()) return ResponseEntity.status(403).build();
  return ResponseEntity.ok(service.labsPorCarreraMap(idCarrera));
}

// Ocupación por laboratorio + jornada
@GetMapping("/ocupacion-lab")
public ResponseEntity<List<int[]>> ocupacionPorLab(@RequestParam String jornada,
                                                   @RequestParam Integer idLaboratorio){
  if (!esCoord()) return ResponseEntity.status(403).build();
  return ResponseEntity.ok(service.ocupacionPorLaboratorio(jornada, idLaboratorio));
}

}
