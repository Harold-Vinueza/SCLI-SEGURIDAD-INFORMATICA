// src/main/java/com/uteq/SCLI/service/HorarioDocenteService.java
package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.HorarioDocenteItem;
import com.uteq.SCLI.repository.HorarioDocenteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HorarioDocenteService {

    private static final List<String> DIAS_ORDEN = List.of("Lunes","Martes","Miércoles","Jueves","Viernes");

    public record Celda(String texto, String laboratorio, String jornada, boolean ocupado) {}
    public record Fila(LocalTime inicio, LocalTime fin, Map<String, Celda> celdas) {}
    public record Tabla(String periodo, List<Fila> filas) {}

    private final HorarioDocenteRepository repo;

    public HorarioDocenteService(HorarioDocenteRepository repo) {
        this.repo = repo;
    }

    public Tabla construirTabla(long idDocente, long idPeriodo) {
        var raw = repo.findRawByDocentePeriodo(idDocente, idPeriodo);
        List<HorarioDocenteItem> items = raw.stream().map(r -> new HorarioDocenteItem(
                (String) r[0],
                (String) r[1],
                (String) r[2],
                ((java.sql.Time) r[3]).toLocalTime(),
                ((java.sql.Time) r[4]).toLocalTime(),
                (String) r[5],
                (String) r[6],
                (String) r[7],
                ((Number) r[8]).longValue()
        )).toList();

        String periodo = items.isEmpty() ? "" : items.get(0).periodo();

        // 1) construir los cortes de tiempo (límites de filas)
        SortedSet<LocalTime> cortes = new TreeSet<>();
        for (var it : items) {
            cortes.add(it.horaInicio());
            cortes.add(it.horaFin());
        }
        if (cortes.isEmpty()) {
            // si no hay horarios, ponemos 07:00–08:00 como ejemplo vacío
            cortes.add(LocalTime.of(7,0));
            cortes.add(LocalTime.of(8,0));
        }
        List<LocalTime> limites = new ArrayList<>(cortes);

        // 2) construir filas por cada [limite i, limite i+1)
        List<Fila> filas = new ArrayList<>();
        for (int i=0; i<limites.size()-1; i++) {
            LocalTime ini = limites.get(i);
            LocalTime fin = limites.get(i+1);
            Map<String, Celda> celdas = new LinkedHashMap<>();
            for (String dia : DIAS_ORDEN) {
                // busca si hay item que cubra completamente esta franja
                Optional<HorarioDocenteItem> match = items.stream()
        .filter(it -> it.diaSemana().equals(dia)
                && !it.horaInicio().isAfter(ini)   // inicio <= ini
                && it.horaFin().compareTo(fin) >= 0) // fin >= fin (cubre toda la franja)
        .findFirst();

               
            if (match.isPresent()) {
                    var it = match.get();
                    String txt = it.materia();
                    String lab = it.codLaboratorio() + " (" + it.nombreLaboratorio() + ")";
                    celdas.put(dia, new Celda(txt, lab, it.jornada(), true));
                } else {
                    celdas.put(dia, new Celda("", "", "", false));
                }
            }
            filas.add(new Fila(ini, fin, celdas));
        }
        return new Tabla(periodo, filas);
    }
}
