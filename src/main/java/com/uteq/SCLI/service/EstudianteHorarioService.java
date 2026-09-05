package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.EstCeldaHorario;
import com.uteq.SCLI.dto.EstFilaHorario;
import com.uteq.SCLI.dto.EstTablaHorario;
import com.uteq.SCLI.repository.EstudianteHorarioRepository;
import com.uteq.SCLI.repository.HorarioRepository;
import com.uteq.SCLI.repository.HorarioRepository.Rango;
import com.uteq.SCLI.repository.EstudianteHorarioRepository.Slot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

@Service
public class EstudianteHorarioService {

    private final HorarioRepository horarioRepo;
    private final EstudianteHorarioRepository estRepo;

    public EstudianteHorarioService(HorarioRepository horarioRepo, EstudianteHorarioRepository estRepo) {
        this.horarioRepo = horarioRepo;
        this.estRepo = estRepo;
    }

    @Transactional(readOnly = true)
    public EstTablaHorario armarTabla(Integer idEst, Integer idMateria) {
        var tabla = new EstTablaHorario("—");
        List<String> dias = List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado");

        List<Rango> rangos = horarioRepo.rangos();
        if (rangos == null || rangos.isEmpty()) {
            rangos = List.of(
                    rango(LocalTime.of(7,30),  LocalTime.of(8,30)),
                    rango(LocalTime.of(8,30),  LocalTime.of(9,30)),
                    rango(LocalTime.of(9,30),  LocalTime.of(10,30)),
                    rango(LocalTime.of(10,30), LocalTime.of(11,30)),
                    rango(LocalTime.of(11,30), LocalTime.of(12,30)),
                    rango(LocalTime.of(12,30), LocalTime.of(13,30)),
                    rango(LocalTime.of(13,30), LocalTime.of(14,30)),
                    rango(LocalTime.of(14,30), LocalTime.of(15,30)),
                    rango(LocalTime.of(15,30), LocalTime.of(16,30)),
                    rango(LocalTime.of(16,30), LocalTime.of(17,30))
            );
        }

        Map<String, Integer> indiceFilaPorRango = new HashMap<>();
        int idx = 0;
        for (Rango r : rangos) {
            EstFilaHorario fila = new EstFilaHorario(r.getHoraInicio(), r.getHoraFin());
            for (String d : dias) fila.getCeldas().put(d, null);
            tabla.getFilas().add(fila);
            indiceFilaPorRango.put(key(r.getHoraInicio(), r.getHoraFin()), idx++);
        }

        if (idEst == null) return tabla;

        List<Slot> slots = estRepo.slotsPorEstudiante(idEst, idMateria);

        for (Slot s : slots) {
            Integer i = indiceFilaPorRango.get(key(s.getHoraInicio(), s.getHoraFin()));
            if (i == null) continue;
            EstFilaHorario fila = tabla.getFilas().get(i);
            fila.getCeldas().put(
                    s.getDiaSemana(),
                    new EstCeldaHorario(true, s.getMateria(), s.getLaboratorio(), s.getJornada())
            );
        }

        return tabla;
    }

    private static String key(LocalTime ini, LocalTime fin) { return ini + "|" + fin; }

    private static Rango rango(LocalTime ini, LocalTime fin) {
        return new Rango() {
            @Override public LocalTime getHoraInicio() { return ini; }
            @Override public LocalTime getHoraFin()    { return fin; }
        };
    }
}