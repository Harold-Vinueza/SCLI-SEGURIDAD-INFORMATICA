package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.EstTablaHorario;
import com.uteq.SCLI.repository.EstudianteIdResolver;
import com.uteq.SCLI.service.EstudianteHorarioService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/estudiante")
public class EstudianteHorarioController {

    private static final Logger log = LoggerFactory.getLogger(EstudianteHorarioController.class);

    private final EstudianteHorarioService horarioService;
    private final EstudianteIdResolver idResolver;

    public EstudianteHorarioController(EstudianteHorarioService horarioService,
                                       EstudianteIdResolver idResolver) {
        this.horarioService = horarioService;
        this.idResolver = idResolver;
    }

    @GetMapping("/horarios")
    public String misHorarios(
            HttpSession session,
            @RequestParam(value = "idEst", required = false) Integer idEstParam,
            @RequestParam(value = "idMateria", required = false) String idMateriaStr,
            Model model
    ) {

        // 1) Intenta leer ID_ESTUDIANTE desde sesión con múltiples claves posibles
        Integer idEst = getInt(session,
                "ID_ESTUDIANTE", "id_estudiante", "idEstudiante");

        // 2) Si no hay, usa ?idEst=...
        if (idEst == null) idEst = idEstParam;

        // 3) Si no hay, intenta resolverlo por ID_PERSONA (leyendo varias claves)
        if (idEst == null) {
            Integer idPersona = getInt(session,
                    "ID_PERSONA", "id_persona", "idPersona");
            if (idPersona != null) {
                idEst = idResolver.findIdEstByIdPersona(idPersona);
            }
        }

        // 4) Parsear idMateria ("" -> null)
        Integer idMateria = (idMateriaStr == null || idMateriaStr.isBlank())
                ? null
                : Integer.valueOf(idMateriaStr.trim());

        log.info("[/estudiante/horarios] idEst={}, idMateria={}", idEst, idMateria);

        // Orden de días que usa tu vista
        var dias = List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado");

        // Si no hay idEst, devolvemos tabla vacía pero con aviso visible
        EstTablaHorario tabla = horarioService.armarTabla(idEst, idMateria);

        model.addAttribute("dias", dias);
        model.addAttribute("tabla", tabla);
        model.addAttribute("idEstudiante", idEst);
        model.addAttribute("idMateria", idMateria);

        if (idEst == null) {
            model.addAttribute("warn",
                    "No se pudo identificar al estudiante. " +
                            "Abra esta página con ?idEst=12 para probar, o inicie sesión con un usuario estudiante.");
        }

        return "dashboard/estudiante/mis-horarios";
    }

    /** Lee un Integer desde sesión probando varias claves; ignora tipos/formatos inválidos. */
    private Integer getInt(HttpSession session, String... keys) {
        for (String k : keys) {
            Object v = session.getAttribute(k);
            if (v == null) continue;
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s && !s.isBlank()) {
                try { return Integer.valueOf(s.trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}