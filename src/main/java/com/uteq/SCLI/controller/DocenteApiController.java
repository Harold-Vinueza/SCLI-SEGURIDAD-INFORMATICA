package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.AvisoDTO;
import com.uteq.SCLI.dto.CrearSolicitudRequest;
import com.uteq.SCLI.dto.DocHorarioDTO;
import com.uteq.SCLI.dto.OptionDTO;
import com.uteq.SCLI.dto.SolicitudItemDTO;
import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.repository.DocenteRepository;
import com.uteq.SCLI.repository.HorarioQueriesRepository;
import com.uteq.SCLI.service.ReservaEspecialService;
import com.uteq.SCLI.service.SolicitudReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docentes")
@RequiredArgsConstructor
public class DocenteApiController {

    private final HorarioQueriesRepository qrepo;
    private final SolicitudReservaService solicitudService;   // <- USAR ESTE
    private final UserSession userSession;
    private final DocenteRepository docenteRepository;
    private final ReservaEspecialService reservaEspecialService;

    /** Obtiene el id_docente de la sesión, o lanza excepción si no corresponde. */
    private Integer requireDocenteId() {
        Integer idPersona = userSession.getIdPersona();
        if (idPersona == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Sesión no inicializada. Vuelve a iniciar sesión."
            );
        }
        Integer idDocente = docenteRepository.findIdByPersona(idPersona);
        if (idDocente == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La persona no es Docente o no tiene registro en public.docente."
            );
        }
        return idDocente;
    }

    /* ===== Combos existentes ===== */
    @GetMapping("/select")
    public List<OptionDTO> select(@RequestParam(required = false) String q) {
        List<Map<String,Object>> rows = qrepo.docentesSelect(q);
        return rows.stream()
                .map(m -> new OptionDTO(((Number)m.get("id")).intValue(),
                        String.valueOf(m.get("label"))))
                .toList();
    }

    @GetMapping("/por-materia/{idMateria}")
    public List<OptionDTO> porMateria(@PathVariable Integer idMateria) {
        return qrepo.docentesPorMateria(idMateria).stream()
                .map(m -> new OptionDTO(((Number)m.get("id")).intValue(),
                        (String)m.get("label")))
                .toList();
    }

    /* ===== Materias asignadas al docente logueado ===== */
    @GetMapping("/materias-asignadas")
    public List<OptionDTO> materiasAsignadas() {
        Integer idDocente = requireDocenteId();
        var rows = qrepo.materiasDeDocente(idDocente);
        return rows.stream()
                .map(m -> new OptionDTO(((Number)m.get("id")).intValue(),
                        String.valueOf(m.get("label"))))
                .toList();
    }

    /* ===== Mis reservas ===== */
    @GetMapping("/solicitudes")
    public ResponseEntity<List<SolicitudItemDTO>> misSolicitudes() {
        Integer idDocente = requireDocenteId();
        return ResponseEntity.ok(solicitudService.misSolicitudes(idDocente));
    }

    /* ===== Crear solicitud ===== */
    @PostMapping("/solicitudes")
    public ResponseEntity<Integer> crearSolicitud(@RequestBody CrearSolicitudRequest req) {
        Integer idDocente = requireDocenteId();
        // IMPORTANTE: Delegar al servicio que resuelve id_admin_piso y fechaUso
        Integer id = solicitudService.crearSolicitud(idDocente, req);
        return ResponseEntity.ok(id);
    }

    /* ===== Horarios para la grilla (por jornada) ===== */
    @GetMapping("/horarios")
    public ResponseEntity<List<DocHorarioDTO>> horariosPorJornada(
            @RequestParam(defaultValue = "Matutina") String jornada
    ) {
        var rows = qrepo.bloquesPorJornada(jornada);
        var list = rows.stream().map(m -> {
            DocHorarioDTO dto = new DocHorarioDTO();
            dto.setId(((Number)m.get("id_horario")).intValue());
            dto.setDiaSemana(String.valueOf(m.get("dia_semana")));
            dto.setHoraInicio(String.valueOf(m.get("hora_inicio"))); // "HH:mm"
            dto.setHoraFin(String.valueOf(m.get("hora_fin")));       // "HH:mm"
            dto.setJornada(jornada);
            dto.setDisponible(true);
            return dto;
        }).toList();
        return ResponseEntity.ok(list);
    }

    /* ===== Avisos (Reservas Especiales publicadas) ===== */
    @GetMapping("/avisos")
    public List<AvisoDTO> avisosPublicados() {
        return reservaEspecialService.publicadas()
                .stream().map(AvisoDTO::from).toList();
    }
}
