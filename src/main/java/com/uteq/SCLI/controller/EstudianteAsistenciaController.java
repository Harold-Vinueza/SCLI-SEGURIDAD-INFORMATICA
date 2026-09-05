package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.AsistenciaRow;
import com.uteq.SCLI.repository.EstudianteRepository;
import com.uteq.SCLI.repository.HorarioVistaRepository;
import com.uteq.SCLI.service.EstudianteAsistenciaService;

import jakarta.servlet.http.HttpSession; // Spring Boot 3 -> jakarta.*
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class EstudianteAsistenciaController {

    private final EstudianteRepository estudianteRepository;
    private final EstudianteAsistenciaService asistenciaService;
    private final HorarioVistaRepository horarioVistaRepository;

    public EstudianteAsistenciaController(EstudianteRepository estudianteRepository,
                                          EstudianteAsistenciaService asistenciaService,
                                          HorarioVistaRepository horarioVistaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.asistenciaService = asistenciaService;
        this.horarioVistaRepository = horarioVistaRepository;
    }

    /** Marca por sesión con código y tooltip (para la vista) */
    public static class SesionMarcaVM {
        private String code;    // "P" | "F" | "J" | "N"
        private String tooltip; // ej: "08-06-2025, 09:30"

        public SesionMarcaVM(String code, String tooltip) {
            this.code = code;
            this.tooltip = tooltip;
        }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTooltip() { return tooltip; }
        public void setTooltip(String tooltip) { this.tooltip = tooltip; }
    }

    /** ViewModel: una fila por materia */
    public static class AsistenciaMateriaVM {
        private Integer idMateria;      // para cruzar con el horario
        private String materia;         // "COD - NOMBRE"
        private String docente;         // el más frecuente
        private int total;
        private int presentes;
        private int faltas;
        private int justificadas;
        private int porcentaje;         // presentes/total * 100
        private List<SesionMarcaVM> sesiones;

        public Integer getIdMateria() { return idMateria; }
        public void setIdMateria(Integer idMateria) { this.idMateria = idMateria; }
        public String getMateria() { return materia; }
        public void setMateria(String materia) { this.materia = materia; }
        public String getDocente() { return docente; }
        public void setDocente(String docente) { this.docente = docente; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPresentes() { return presentes; }
        public void setPresentes(int presentes) { this.presentes = presentes; }
        public int getFaltas() { return faltas; }
        public void setFaltas(int faltas) { this.faltas = faltas; }
        public int getJustificadas() { return justificadas; }
        public void setJustificadas(int justificadas) { this.justificadas = justificadas; }
        public int getPorcentaje() { return porcentaje; }
        public void setPorcentaje(int porcentaje) { this.porcentaje = porcentaje; }
        public List<SesionMarcaVM> getSesiones() { return sesiones; }
        public void setSesiones(List<SesionMarcaVM> sesiones) { this.sesiones = sesiones; }
    }

    @GetMapping("/dashboard/estudiante/asistencia")
    public String verAsistencia(
            HttpSession session,
            @RequestParam(required = false) Integer idMateria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model
    ) {
        Integer idPersona = (Integer) session.getAttribute("idPersona");
        Integer idUsuario = (Integer) session.getAttribute("idUsuario");

        Integer idEstudiante = null;
        if (idPersona != null) {
            idEstudiante = estudianteRepository.findIdEstudianteByIdPersona(idPersona);
        }
        if (idEstudiante == null && idUsuario != null) {
            idEstudiante = estudianteRepository.findIdEstudianteByIdUsuario(idUsuario);
        }

        if (idEstudiante == null) {
            model.addAttribute("error", "No se pudo resolver tu ID de estudiante (verifica sesión).");
            return "dashboard/estudiante/asistencia";
        }

        List<AsistenciaRow> rows = asistenciaService.listar(
                idEstudiante,
                idMateria,
                (desde != null ? Date.valueOf(desde) : null),
                (hasta != null ? Date.valueOf(hasta) : null)
        );

        // ---- AGRUPADO por materia (las que tienen registros)
        List<AsistenciaMateriaVM> items = agruparPorMateria(rows);

        // ---- COMPLETAR con materias del horario que no tengan registros aún
        List<HorarioVistaRepository.MateriaLabel> materiasHorario =
                horarioVistaRepository.listarMateriasHorario(idEstudiante);

        Set<Integer> idsPresentes = items.stream()
                .map(AsistenciaMateriaVM::getIdMateria)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (HorarioVistaRepository.MateriaLabel r : materiasHorario) {
            Integer idMat = r.getIdMateria();
            String etiquetaMateria = r.getMateria(); // "COD - NOMBRE"
            if (!idsPresentes.contains(idMat)) {
                AsistenciaMateriaVM vm = new AsistenciaMateriaVM();
                vm.setIdMateria(idMat);
                vm.setMateria(etiquetaMateria);
                vm.setDocente("");
                vm.setTotal(0);
                vm.setPresentes(0);
                vm.setFaltas(0);
                vm.setJustificadas(0);
                vm.setPorcentaje(0);
                vm.setSesiones(Collections.emptyList());
                items.add(vm);
            }
        }

        // Orden final por materia (código/nombre)
        items.sort(Comparator.comparing(AsistenciaMateriaVM::getMateria, String.CASE_INSENSITIVE_ORDER));

        // KPI de cabecera (sobre todas las sesiones filtradas)
        int total = rows.size();
        int presentes = (int) rows.stream().filter(r -> Boolean.TRUE.equals(r.getAsistencia())).count();
        int faltas = total - presentes;
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("total", total);
        resumen.put("presentes", presentes);
        resumen.put("faltas", faltas);

        // ---- Modelo para la vista
        model.addAttribute("items", items);            // agrupado (una fila por materia)
        model.addAttribute("asistencias", rows);       // fallback (lista plana), por si lo usas
        model.addAttribute("resumen", resumen);

        model.addAttribute("idMateria", idMateria);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "dashboard/estudiante/asistencia";
    }

    /** Agrupa por id_materia, calcula % y marcas con tooltip en orden cronológico */
    private List<AsistenciaMateriaVM> agruparPorMateria(List<AsistenciaRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        Comparator<AsistenciaRow> byFechaHora =
                Comparator.comparing(AsistenciaRow::getFechaClase, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AsistenciaRow::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder()));

        Map<Integer, List<AsistenciaRow>> porMateria = rows.stream()
                .collect(Collectors.groupingBy(AsistenciaRow::getIdMateria, LinkedHashMap::new, Collectors.toList()));

        List<AsistenciaMateriaVM> out = new ArrayList<>();
        DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (Map.Entry<Integer, List<AsistenciaRow>> e : porMateria.entrySet()) {
            List<AsistenciaRow> list = e.getValue();
            list.sort(byFechaHora);

            AsistenciaRow first = list.get(0);
            AsistenciaMateriaVM vm = new AsistenciaMateriaVM();
            vm.setIdMateria(first.getIdMateria());
            vm.setMateria(first.getCodMateria() + " - " + first.getNombreMateria());
            vm.setDocente(docenteMasFrecuente(list));

            int total = 0, presentes = 0, faltas = 0, justificadas = 0;
            List<SesionMarcaVM> sesiones = new ArrayList<>();

            for (AsistenciaRow r : list) {
                total++;

                // Tooltip: "dd-MM-yyyy, HH:mm" si hay hora; si no, solo fecha
                String fechaStr = "";
                if (r.getFechaClase() != null) {
                    fechaStr = r.getFechaClase().toLocalDate().format(FECHA_FMT);
                }
                String horaStr = "";
                if (r.getHoraInicio() != null) {
                    String h = r.getHoraInicio().toString(); // "09:30:00" o "09:30"
                    horaStr = (h.length() >= 5 ? h.substring(0, 5) : h);
                }
                String tooltip = (horaStr == null || horaStr.isBlank()) ? fechaStr : (fechaStr + ", " + horaStr);

                if (Boolean.TRUE.equals(r.getAsistencia())) {
                    presentes++;
                    sesiones.add(new SesionMarcaVM("P", tooltip));
                } else {
                    boolean justificada = r.getObservaciones() != null &&
                            r.getObservaciones().toLowerCase().contains("justific");
                    if (justificada) {
                        justificadas++;
                        sesiones.add(new SesionMarcaVM("J", tooltip));
                    } else {
                        faltas++;
                        sesiones.add(new SesionMarcaVM("F", tooltip));
                    }
                }
            }

            vm.setTotal(total);
            vm.setPresentes(presentes);
            vm.setFaltas(faltas);
            vm.setJustificadas(justificadas);
            vm.setPorcentaje(total == 0 ? 0 : Math.round(presentes * 100f / total));
            vm.setSesiones(sesiones);

            out.add(vm);
        }

        out.sort(Comparator.comparing(AsistenciaMateriaVM::getMateria, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Toma el docente más frecuente dentro de la materia (por si varía) */
    private String docenteMasFrecuente(List<AsistenciaRow> list){
        return list.stream()
                .map(AsistenciaRow::getDocente)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("");
    }
}