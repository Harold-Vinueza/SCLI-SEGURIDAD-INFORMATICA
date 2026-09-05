package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.SolicitudAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<SolicitudAsignacion, Integer> {

    // Listado simple por docente (si prefieres usar la vista, podemos cambiarlo luego)
    @Query(value = """
        SELECT s.id_solicitud, s.estado, s.estado_redireccion, s.fecha_solicitud, 
               s.id_horario, h.dia_semana, h.hora_inicio, h.hora_fin, h.jornada,
               s.materia, s.tipo_solicitud
        FROM public.solicitudasignacion s
        JOIN public.horario h ON h.id_horario = s.id_horario
        WHERE s.id_docente = :idDocente
        ORDER BY s.fecha_solicitud DESC, 
                 ARRAY_POSITION(ARRAY['Lunes','Martes','Miércoles','Jueves','Viernes']::text[], h.dia_semana),
                 h.hora_inicio
    """, nativeQuery = true)
    List<Object[]> findMisSolicitudes(@Param("idDocente") Integer idDocente);
}
