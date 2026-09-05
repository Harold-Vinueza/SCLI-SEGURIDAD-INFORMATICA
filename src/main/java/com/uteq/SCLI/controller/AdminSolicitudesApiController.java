package com.uteq.SCLI.controller;

import com.uteq.SCLI.service.AdminSolicitudesService;
import com.uteq.SCLI.service.ReservaTemporalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * API de administración de solicitudes (agrupadas por lote o por docente+materia+jornada).
 * Endpoints: listar grupos, labs por grupo, aprobar/rechazar/proponer grupo, grilla de horarios
 * y aprobar directamente por propuesta aceptada.
 */
@RestController
@RequestMapping("/api/admin/solicitudes")
@RequiredArgsConstructor
public class AdminSolicitudesApiController {

    private final AdminSolicitudesService service;
    private final ReservaTemporalService reservaTemporalService; // ⭐ nuevo service para manejar fecha

    // === Listar agrupadas (por lote o docente+materia+jornada)
    @GetMapping
    public ResponseEntity<?> listarAgrupadas(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarAgrupadas(estado));
    }

    // === Laboratorios disponibles para TODO el grupo
    @GetMapping("/{grupoId}/labs-grupo")
    public ResponseEntity<?> labsGrupo(@PathVariable String grupoId) {
        return ResponseEntity.ok(service.labsParaGrupo(grupoId));
    }

    // === Resumen del grupo (cuántos bloques requiere) - para el modal Proponer
    @GetMapping("/{grupoId}/resumen-grupo")
    public ResponseEntity<?> resumenGrupo(@PathVariable String grupoId) {
        return ResponseEntity.ok(service.resumenGrupo(grupoId));
    }

    // === Aprobar TODO el grupo (usando fecha de solicitud como fecha_asignacion)
    // Body esperado: { idLaboratorio:number, idMateria?:number }
    @PostMapping("/{grupoId}/aprobar-grupo")
    public ResponseEntity<?> aprobarGrupo(@PathVariable String grupoId,
                                          @RequestBody Map<String,Object> body) {
        try {
            Integer idLaboratorio = (body.get("idLaboratorio") instanceof Number)
                    ? ((Number) body.get("idLaboratorio")).intValue()
                    : null;
            Integer idMateria = (body.get("idMateria") instanceof Number)
                    ? ((Number) body.get("idMateria")).intValue()
                    : null;

            if (idLaboratorio == null) {
                return ResponseEntity.badRequest().body(Map.of("error","idLaboratorio requerido"));
            }

            // 👉 usa la lógica de fechas en ReservaTemporalService
            reservaTemporalService.aprobarGrupoUsandoFechaSolicitud(grupoId, idLaboratorio);

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "fecha", java.time.OffsetDateTime.now().toString(),
                    "mensaje", "Solicitud inválida",
                    "detalle", ex.getMessage()
            ));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "mensaje", "Error al aprobar el grupo",
                    "detalle", ex.getMessage()
            ));
        }
    }

    // === Rechazar TODO el grupo
    @PostMapping("/{grupoId}/rechazar-grupo")
    public ResponseEntity<?> rechazarGrupo(@PathVariable String grupoId,
                                           @RequestBody(required = false) Map<String,Object> body) {
        service.rechazarGrupo(grupoId);
        return ResponseEntity.ok().build();
    }

    // === Horarios para propuesta (grilla por jornada + laboratorio opcional)
    @GetMapping("/{grupoId}/horarios-grupo")
    public ResponseEntity<?> horariosGrupo(@PathVariable String grupoId,
                                           @RequestParam(required = false) String jornada,
                                           @RequestParam(required = false) Integer idLaboratorio) {
        return ResponseEntity.ok(service.horariosParaPropuestaGrupo(grupoId, jornada, idLaboratorio));
    }

    // === Proponer alternativa para TODO el grupo (soporta varios horarios)
    // Body: { idLaboratorio:number, idHorarios:number[], mensaje?:string }
    @PostMapping("/{grupoId}/proponer-grupo")
    public ResponseEntity<?> proponerGrupo(@PathVariable String grupoId,
                                           @RequestBody Map<String,Object> body) {
        Integer idLaboratorio = (body.get("idLaboratorio") instanceof Number)
                ? ((Number) body.get("idLaboratorio")).intValue() : null;
        String mensaje = body.get("mensaje") instanceof String ? (String) body.get("mensaje") : null;

        List<Integer> idHorarios = new ArrayList<>();
        Object rawList = body.get("idHorarios");
        if (rawList instanceof Collection<?> col) {
            for (Object o : col) if (o instanceof Number n) idHorarios.add(n.intValue());
        }
        if (idHorarios.isEmpty() && body.get("idHorario") instanceof Number n) {
            idHorarios = List.of(n.intValue());
        }
        if (idLaboratorio == null || idHorarios.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "idLaboratorio e idHorarios (o idHorario) son requeridos"));
        }
        service.proponerGrupo(grupoId, idHorarios, idLaboratorio, mensaje);
        return ResponseEntity.ok().build();
    }

    // === ¿El grupo tiene una propuesta completa lista para aprobar?
    @GetMapping("/{grupoId}/tiene-propuesta")
    public ResponseEntity<?> tienePropuesta(@PathVariable String grupoId) {
        Map<String, Object> det = service.propuestaGrupo(grupoId);
        if (det == null) return ResponseEntity.ok(Map.of("ok", false));
        det.put("ok", true);
        return ResponseEntity.ok(det);
    }

    // === Aprobar grupo directamente usando la propuesta aceptada por el docente
    @PostMapping("/{grupoId}/aprobar-por-propuesta")
    public ResponseEntity<?> aprobarPorPropuesta(@PathVariable String grupoId,
                                                 @RequestBody(required = false) Map<String,Object> body) {
        Integer idMateria = (body != null && body.get("idMateria") instanceof Number)
                ? ((Number) body.get("idMateria")).intValue()
                : null;
        service.aprobarPorPropuesta(grupoId, idMateria);
        return ResponseEntity.ok().build();
    }

    // === Manejo de errores conocidos (400 en vez de 500)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleKnown(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "fecha", java.time.OffsetDateTime.now().toString(),
                "mensaje", "Solicitud inválida",
                "detalle", ex.getMessage()
        ));
    }
}

/*package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.AdminSolicitudItemDTO;
import com.uteq.SCLI.dto.LabOpcionDTO;
import com.uteq.SCLI.dto.HorarioOpcionDTO;
import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.service.AdminSolicitudesService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Slf4j
public class AdminSolicitudesApiController {

    private final AdminSolicitudesService service;
    private final UserSession userSession;

    // Listados
    @GetMapping("/solicitudes")
    public ResponseEntity<List<AdminSolicitudItemDTO>> listarAntigua(@RequestParam(required = false) String estado) {
        Integer idPersona = userSession.getIdPersona();
        return ResponseEntity.ok(service.listarParaAdmin(idPersona, estado));
    }

    @GetMapping("/solicitudes-docente")
    public ResponseEntity<List<AdminSolicitudItemDTO>> listarDocente(@RequestParam(required = false) String estado) {
        Integer idPersona = userSession.getIdPersona();
        return ResponseEntity.ok(service.listarParaAdmin(idPersona, estado));
    }

    // ======= ACCIONES =======
    @PostMapping("/solicitudes/{id}/aprobar")
    public ResponseEntity<Map<String, Object>> aprobar(@PathVariable("id") Integer idSolicitud,
                                                       @RequestBody AprobarReq body) {
        Integer idPersona = userSession.getIdPersona();
        Integer idLab = body == null ? null : body.getIdLaboratorio();
        log.info("POST /api/admin/solicitudes/{}/aprobar  persona={} lab={}", idSolicitud, idPersona, idLab);

        int updated = service.aprobar(idPersona, idSolicitud, idLab);
        log.info("… actualizaron {} fila(s)", updated);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<Map<String, Object>> rechazar(@PathVariable("id") Integer idSolicitud,
                                                        @RequestBody RechazarReq body) {
        Integer idPersona = userSession.getIdPersona();
        String motivo = body == null ? null : body.getMotivo();
        log.info("POST /api/admin/solicitudes/{}/rechazar  persona={} motivo='{}'", idSolicitud, idPersona, motivo);

        int updated = service.rechazar(idPersona, idSolicitud, motivo);
        log.info("… actualizaron {} fila(s)", updated);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/solicitudes/{id}/proponer")
    public ResponseEntity<Map<String, Object>> proponer(@PathVariable("id") Integer idSolicitud,
                                                        @RequestBody ProponerReq body) {
        Integer idPersona = userSession.getIdPersona();
        Integer idHorario = body == null ? null : body.getIdHorario();
        Integer idLaboratorio = body == null ? null : body.getIdLaboratorio();
        String mensaje = body == null ? null : body.getMensaje();
        log.info("POST /api/admin/solicitudes/{}/proponer  persona={} horario={} lab={} msg='{}'",
                idSolicitud, idPersona, idHorario, idLaboratorio, mensaje);

        int updated = service.proponer(idPersona, idSolicitud, idHorario, idLaboratorio, mensaje);
        log.info("… actualizaron {} fila(s)", updated);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // ======= ENDPOINTS PARA MODALES (labs + horarios) =======
    @GetMapping("/solicitudes/{id}/labs")
    public ResponseEntity<List<LabOpcionDTO>> labs(@PathVariable("id") Integer idSolicitud) {
        return ResponseEntity.ok(service.labsParaSolicitud(idSolicitud));
    }

    @GetMapping("/solicitudes/{id}/horarios")
    public ResponseEntity<List<HorarioOpcionDTO>> horarios(@PathVariable("id") Integer idSolicitud,
                                                           @RequestParam(defaultValue = "Matutina") String jornada) {
        return ResponseEntity.ok(service.horariosParaSolicitud(idSolicitud, jornada));
    }

    // DTOs de request
    @Data public static class AprobarReq  { private Integer idLaboratorio; }
    @Data public static class RechazarReq { private String  motivo; }
    @Data public static class ProponerReq { private Integer idHorario; private Integer idLaboratorio; private String mensaje; }

    // ======= DEBUG =======
    @GetMapping("/solicitudes/{id}/debug")
    public Map<String, Object> debug(@PathVariable Integer id) {
        return Map.of(
            "count",  service.debugCount(id),
            "estado", service.debugEstado(id)
        );
    }
}
*/