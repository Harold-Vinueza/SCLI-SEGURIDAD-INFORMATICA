package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    // Proyección para devolver solo las horas, sin mapear toda la entidad
    interface Rango {
        LocalTime getHoraInicio();
        LocalTime getHoraFin();
    }

    @Query(value = """
        SELECT DISTINCT h.hora_inicio AS horaInicio, h.hora_fin AS horaFin
        FROM public.horario h
        ORDER BY h.hora_inicio
        """, nativeQuery = true)
    List<Rango> rangos();
}