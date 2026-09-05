package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.MateriaCardDTO;
import com.uteq.SCLI.dto.PaseListaVM;
import com.uteq.SCLI.service.AsistenciaService;
import com.uteq.SCLI.service.SesionService;
import com.uteq.SCLI.service.PdfReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.MultiValueMap;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard/docente/asistencias")
public class AsistenciaDocenteController {

  private static final Logger log = LoggerFactory.getLogger(AsistenciaDocenteController.class);

  private final AsistenciaService svc;
  private final SesionService sesion;
  private final PdfReport pdfReport;

  public AsistenciaDocenteController(AsistenciaService svc, SesionService sesion, PdfReport pdfReport) {
    this.svc = svc;
    this.sesion = sesion;
    this.pdfReport = pdfReport;
  }

  /** Resuelve id_docente por varias vías; incluye fallback de desarrollo. */
  private Integer ensureIdDocente(HttpServletRequest request) {
    Integer idDocente = null;
    try { idDocente = sesion.idDocente(); } catch (Exception ignore) {}
    if (idDocente != null) {
      log.debug("[Asistencias] id_docente por SesionService: {}", idDocente);
      return idDocente;
    }

    var httpSession = request.getSession(false);
    if (httpSession != null) {
      Object v = httpSession.getAttribute("idDocente");
      if (v instanceof Integer idD) return idD;

      Object p1 = httpSession.getAttribute("id_persona");
      if (p1 instanceof Integer idP1) {
        Integer x = svc.idDocentePorIdPersona(idP1);
        if (x != null) return x;
      }
      Object p2 = httpSession.getAttribute("idPersona");
      if (p2 instanceof Integer idP2) {
        Integer x = svc.idDocentePorIdPersona(idP2);
        if (x != null) return x;
      }
    }

    String username = null;
    Principal principal = request.getUserPrincipal();
    if (principal != null) username = principal.getName();
    if (username == null && httpSession != null) {
      Object u1 = httpSession.getAttribute("username");
      if (u1 instanceof String s) username = s;
      if (username == null) {
        Object u2 = httpSession.getAttribute("usuario");
        if (u2 instanceof String s2) username = s2;
      }
    }
    if (username != null && !username.isBlank()) {
      Integer x = svc.idDocentePorUsername(username);
      if (x != null) return x;
    }

    String forced = System.getProperty("scli.dev.user");
    if (forced == null || forced.isBlank()) forced = System.getenv("SCLI_DEV_USER");
    if (forced != null && !forced.isBlank()) {
      Integer x = svc.idDocentePorUsername(forced);
      if (x != null) {
        log.warn("[Asistencias] Usando fallback DEV (scli.dev.user='{}') -> id_docente={}", forced, x);
        return x;
      }
    }

    Integer x = svc.idDocentePorUsername("joker");
    if (x != null) {
      log.warn("[Asistencias] Fallback temporal a username='joker' -> id_docente={}", x);
      return x;
    }

    log.warn("[Asistencias] No se pudo resolver id_docente");
    return null;
  }

  @GetMapping
  public String index(Model model, HttpServletRequest request) {
    Integer idDocente = ensureIdDocente(request);
    List<MateriaCardDTO> materias = (idDocente != null) ? svc.materiasDeDocente(idDocente) : List.of();
    log.info("[Asistencias] id_docente={} materias={}", idDocente, materias.size());
    model.addAttribute("materias", materias);
    return "dashboard/docente/asistencias";
  }

  @GetMapping("/{idMateria}")
  public String abrir(@PathVariable Integer idMateria,
                      @RequestParam(required = false)
                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                      @RequestParam(required = false) Integer idLaboratorio,
                      @RequestParam(required = false) String tema,
                      Model model,
                      HttpServletRequest request) {
    Integer idDocente = ensureIdDocente(request);
    LocalDate f = (fecha != null) ? fecha : LocalDate.now();
    PaseListaVM vm = svc.abrirPase(idDocente, idMateria, f, idLaboratorio, tema);
    model.addAttribute("vm", vm);
    model.addAttribute("idMateria", idMateria);
    return "dashboard/docente/asistencias-pase";
  }

 // AsistenciaDocenteController.java
// AsistenciaDocenteController.java
@PostMapping("/{idRegistro}")
public String guardar(@PathVariable Integer idRegistro,
                      @RequestParam MultiValueMap<String, String> form,
                      RedirectAttributes ra,
                      HttpServletRequest request) {

  Integer idDocente = ensureIdDocente(request);

  // construir mapa idEstudiante -> presente?
  Map<Integer, Boolean> marcas = new HashMap<>();
  for (String k : form.keySet()) {
    if (k.startsWith("present[")) {
      int idEst = Integer.parseInt(k.substring(8, k.length() - 1));
      var valores = form.get(k);  // puede venir ["0"] o ["0","1"]
      boolean checked = valores != null && (
              valores.contains("1") ||
              valores.stream().anyMatch(v -> "on".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v))
      );
      marcas.put(idEst, checked);
    }
  }

  svc.guardar(idDocente, idRegistro, marcas);
  ra.addFlashAttribute("ok", "Asistencia guardada.");

  String fecha = Optional.ofNullable(form.getFirst("fecha")).orElse("");
  String idMateria = form.getFirst("idMateria");
  return "redirect:/dashboard/docente/asistencias/" + idMateria + (fecha.isBlank() ? "" : ("?fecha=" + fecha));
}


  @GetMapping(value = "/{idRegistro}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @ResponseBody
  public ResponseEntity<byte[]> pdf(@PathVariable Integer idRegistro) {
    byte[] bytes = pdfReport.render(svc.pdf(idRegistro));
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=asistencia-" + idRegistro + ".pdf");
    return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
  }
}
