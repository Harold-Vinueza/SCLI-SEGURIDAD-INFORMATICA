// src/main/java/com/uteq/SCLI/controller/HorarioDocenteController.java
package com.uteq.SCLI.controller;

import com.uteq.SCLI.repository.DocenteRepository;
import com.uteq.SCLI.service.HorarioDocenteService;
import com.uteq.SCLI.util.PdfHorarioUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/docente/horarios")
public class HorarioDocenteController {

    private final HorarioDocenteService svc;
    private final PdfHorarioUtil pdfUtil;
    private final DocenteRepository docenteRepo;

    public HorarioDocenteController(HorarioDocenteService svc,
                                    PdfHorarioUtil pdfUtil,
                                    DocenteRepository docenteRepo) {
        this.svc = svc;
        this.pdfUtil = pdfUtil;
        this.docenteRepo = docenteRepo;
    }

    @GetMapping
    public String vista(Model model,
                        HttpServletRequest request,
                        @RequestParam(required = false) Long idDocente,
                        @RequestParam(required = false) Long idPeriodo) {

        // 1) docente: param -> sesión (idDocente / id_docente) -> resolver por id_persona -> default
        Long docente = idDocente != null ? idDocente
                : (Long) request.getSession().getAttribute("idDocente");
        if (docente == null) docente = (Long) request.getSession().getAttribute("id_docente");

        if (docente == null) {
            Integer idPersona = (Integer) request.getSession().getAttribute("idPersona");
            if (idPersona == null) idPersona = (Integer) request.getSession().getAttribute("id_persona");
            if (idPersona != null) {
                try {
                    docente = docenteRepo.findIdDocenteByIdPersona(idPersona);
                } catch (Exception ignored) {}
            }
        }
        if (docente == null) docente = 3L; // último recurso (cámbialo si quieres)

        // 2) periodo: param -> sesión (idPeriodo / id_periodo / idPeriodoActivo) -> default
        Long periodo = idPeriodo != null ? idPeriodo
                : (Long) request.getSession().getAttribute("idPeriodo");
        if (periodo == null) periodo = (Long) request.getSession().getAttribute("id_periodo");
        if (periodo == null) periodo = (Long) request.getSession().getAttribute("idPeriodoActivo");
        if (periodo == null) periodo = 2L;

        // 3) Persistir en sesión y exponer en modelo
        request.getSession().setAttribute("idDocente", docente);
        request.getSession().setAttribute("idPeriodo", periodo);
        model.addAttribute("idDocente", docente);
        model.addAttribute("idPeriodo", periodo);

        // 4) Construir tabla
        var tabla = svc.construirTabla(docente, periodo);
        model.addAttribute("tabla", tabla);
        model.addAttribute("dias", List.of("Lunes","Martes","Miércoles","Jueves","Viernes"));

        return "dashboard/docente/mis-horarios";
    }

    @GetMapping("/pdf")
    public void pdf(HttpServletRequest request,
                    HttpServletResponse resp,
                    @RequestParam(required = false) Long idDocente,
                    @RequestParam(required = false) Long idPeriodo) throws Exception {

        Long docente = idDocente != null ? idDocente : (Long) request.getSession().getAttribute("idDocente");
        if (docente == null) docente = (Long) request.getSession().getAttribute("id_docente");
        Long periodo = idPeriodo != null ? idPeriodo : (Long) request.getSession().getAttribute("idPeriodo");
        if (periodo == null) periodo = (Long) request.getSession().getAttribute("id_periodo");
        if (docente == null) docente = 3L;
        if (periodo == null) periodo = 2L;

        var tabla = svc.construirTabla(docente, periodo);
        var dias  = List.of("Lunes","Martes","Miércoles","Jueves","Viernes");
        String titulo = "Horario Docente – " + tabla.periodo();

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition","inline; filename=mis-horarios.pdf");
        try (var os = resp.getOutputStream()) {
            pdfUtil.writeHorarioPdf(os, tabla, dias, titulo);
        }
    }

   



}
