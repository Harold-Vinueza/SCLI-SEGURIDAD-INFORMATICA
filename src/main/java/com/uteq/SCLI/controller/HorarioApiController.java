package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.AsignacionRequest;
import com.uteq.SCLI.dto.CounterDTO;
import com.uteq.SCLI.dto.GridRowDTO;
import com.uteq.SCLI.dto.OptionDTO;
import com.uteq.SCLI.repository.AsignacionCrudRepository;
import com.uteq.SCLI.repository.HorarioQueriesRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planif")
public class HorarioApiController {

  private final HorarioQueriesRepository qrepo;
  private final AsignacionCrudRepository arepo;

  public HorarioApiController(HorarioQueriesRepository qrepo, AsignacionCrudRepository arepo) {
    this.qrepo = qrepo;
    this.arepo = arepo;
  }

  // ======== PERÍODOS ========

  /** Obtiene un período por id */
 @GetMapping("/periodos/{id}")
public Map<String,Object> periodo(@PathVariable Integer id) {
  return qrepo.periodoById(id); // usa el repo de abajo
}

  /** Abre/cierra un período (no exclusivo; solo cambia el flag 'activo') */
  @PatchMapping("/periodos/{id}/estado")
public ResponseEntity<?> setPeriodoEstado(@PathVariable Integer id, @RequestParam boolean activo) {
  int n = qrepo.setPeriodoEstado(id, activo);
  if (n == 0) return ResponseEntity.notFound().build();
  return ResponseEntity.ok(Map.of("ok", true, "activo", activo));
}

  /** Períodos activos para el combo */
  @GetMapping("/periodos/activos")
  public List<OptionDTO> periodosActivos() {
    return qrepo.periodosActivos().stream()
        .map(m -> new OptionDTO(((Number) m.get("id")).intValue(), (String) m.get("label")))
        .toList();
  }

  // ======== CATÁLOGOS ========

  @GetMapping("/labs")
  public List<OptionDTO> labs() {
    return qrepo.laboratorios().stream()
        .map(m -> new OptionDTO(((Number) m.get("id")).intValue(), (String) m.get("label")))
        .toList();
  }

  @GetMapping("/materias")
  public List<OptionDTO> materias(@RequestParam(required = false) String q) {
    return qrepo.materias(q).stream()
        .map(m -> new OptionDTO(((Number) m.get("id")).intValue(),
            m.get("cod_materia") + " - " + m.get("nombre_materia")))
        .toList();
  }

  @GetMapping("/materias/{idMateria}/docentes")
  public List<OptionDTO> docentesPorMateria(@PathVariable Integer idMateria) {
    return qrepo.docentesPorMateria(idMateria).stream()
        .map(m -> new OptionDTO(((Number) m.get("id")).intValue(), (String) m.get("label")))
        .toList();
  }

  @GetMapping("/horarios/bloques")
  public List<Map<String, Object>> bloques(@RequestParam String jornada) {
    return qrepo.bloquesPorJornada(jornada);
  }

  // ======== GRILLA / CONTADOR ========

  @GetMapping("/asignaciones/semana")
  public List<GridRowDTO> grilla(@RequestParam Integer lab,
                                 @RequestParam Integer periodo,
                                 @RequestParam String jornada) {
    return qrepo.grillaSemana(lab, periodo, jornada).stream()
        .map(r -> new GridRowDTO(
            (String) r.get("bloque"),
            (String) r.get("lunes"),
            (String) r.get("martes"),
            (String) r.get("miercoles"),
            (String) r.get("jueves"),
            (String) r.get("viernes")
        )).toList();
  }

  @GetMapping("/asignaciones/contador")
  public CounterDTO contador(@RequestParam Integer lab, @RequestParam Integer periodo) {
    Map<String, Object> m = qrepo.contador(lab, periodo);
    return new CounterDTO(
        ((Number) m.get("total_asignadas")).intValue(),
        ((Number) m.get("total_restantes")).intValue(),
        ((Number) m.get("matutina_asignadas")).intValue(),
        ((Number) m.get("matutina_restantes")).intValue(),
        ((Number) m.get("vespertina_asignadas")).intValue(),
        ((Number) m.get("vespertina_restantes")).intValue()
    );
  }

  // ======== CELDA / CRUD ASIGNACIONES ========

  /** Devuelve la asignación (si existe) para una celda específica */
 @GetMapping("/asignaciones/celda")
public ResponseEntity<?> asignacionCelda(@RequestParam Integer lab,
                                         @RequestParam Integer periodo,
                                         @RequestParam Integer horario) {
  Map<String,Object> m = qrepo.asignacionPorCelda(lab, periodo, horario);
  return (m == null || m.isEmpty()) ? ResponseEntity.noContent().build()
                                    : ResponseEntity.ok(m);
}

  /** Crea una nueva asignación (valida índices únicos / triggers en BD) */
  @PostMapping("/asignaciones")
@Transactional
public ResponseEntity<?> crear(@RequestBody AsignacionRequest req) {
  try {
    arepo.insertar(req.idLaboratorio(), req.idHorario(), req.idDocente(),
                   null, req.idMateria(), req.idPeriodo());
    return ResponseEntity.ok(Map.of("ok", true));
  } catch (DataIntegrityViolationException ex) {
    return ResponseEntity.status(409).body(Map.of("ok", false, "error", extractMessage(ex)));
  }
}

  /** Actualiza docente/materia de una asignación existente */
 @PutMapping("/asignaciones/{id}")
@Transactional
public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody AsignacionRequest req) {
  try {
    int n = arepo.actualizar(id, req.idDocente(), req.idMateria());
    return (n > 0) ? ResponseEntity.ok(Map.of("ok", true))
                   : ResponseEntity.status(404).body(Map.of("ok", false, "error", "No existe la asignación"));
  } catch (DataIntegrityViolationException ex) {
    return ResponseEntity.status(409).body(Map.of("ok", false, "error", extractMessage(ex)));
  }
}

  /** Elimina por id (idempotente) */
@DeleteMapping("/asignaciones/{id}")
@Transactional
public ResponseEntity<?> eliminar(@PathVariable Integer id) {
  try {
    int n = arepo.borrar(id);
    if (n == 0) {
      // idempotente: tratamos "ya no existe" como éxito suave si prefieres
      return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Ya no existe"));
    }
    return ResponseEntity.ok(Map.of("ok", true));
  } catch (DataIntegrityViolationException ex) {
    return ResponseEntity.status(409).body(Map.of("ok", false, "error", extractMessage(ex)));
  }
}

  // ======== Utils ========

  /** Extrae el mensaje del RAISE EXCEPTION de PostgreSQL si existe */
  private String extractMessage(Exception ex) {
    Throwable t = ex;
    while (t != null) {
      if (t instanceof SQLException) return t.getMessage();
      t = t.getCause();
    }
    return ex.getMessage();
  }
}
