package com.uteq.SCLI.repository;

import com.uteq.SCLI.dto.AdminSolicitudItemDTO;
import com.uteq.SCLI.dto.LabOpcionDTO;
import com.uteq.SCLI.dto.HorarioOpcionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

@Repository
public class AdminSolicitudesRepository {

    private final JdbcTemplate jdbc;

    public AdminSolicitudesRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /* ========= util ========= */

    private static final RowMapper<AdminSolicitudItemDTO> RM = (ResultSet rs, int i) -> {
        AdminSolicitudItemDTO dto = new AdminSolicitudItemDTO();
        dto.setIdSolicitud(rs.getInt("id_solicitud"));
        dto.setDocente(rs.getString("docente"));
        dto.setMateria(rs.getString("materia"));
        dto.setDiaSemana(rs.getString("dia_semana"));
        dto.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
        dto.setHoraFin(rs.getTime("hora_fin").toLocalTime());
        dto.setJornada(rs.getString("jornada"));
        dto.setEstado(rs.getString("estado"));
        dto.setEstadoRedireccion(rs.getString("estado_redireccion"));
        return dto;
    };

    private boolean hasColumn(String table, String column) {
        final String sql = """
            SELECT COUNT(*) > 0
            FROM information_schema.columns
            WHERE table_schema='public' AND table_name=? AND column_name=?
        """;
        Boolean ok = jdbc.queryForObject(sql, Boolean.class, table, column);
        return ok != null && ok;
    }

    /* ========= queries ========= */

    public Integer findIdAdminPisoByPersona(Integer idPersona) {
        String sql = """
            SELECT ap.id_admin_piso
            FROM public.administradorpiso ap
            WHERE ap.id_persona = ?
            ORDER BY ap.id_admin_piso
            LIMIT 1
        """;
        List<Integer> l = jdbc.query(sql, (rs,i)->rs.getInt(1), idPersona);
        return l.isEmpty()? null : l.get(0);
    }

    public List<AdminSolicitudItemDTO> listarPorPisoYEstado(Integer idAdminPiso, String estado) {
        String base = """
            SELECT s.id_solicitud,
                   (p.nombres || ' ' || p.apellidos) AS docente,
                   s.materia, h.dia_semana, h.hora_inicio, h.hora_fin, h.jornada,
                   s.estado, s.estado_redireccion
            FROM public.solicitudasignacion s
            JOIN public.docente  d ON d.id_docente = s.id_docente
            JOIN public.persona  p ON p.id_persona = d.id_persona
            JOIN public.horario  h ON h.id_horario = s.id_horario
            WHERE s.id_admin_piso = ?
        """;
        String order = """
            ORDER BY CASE h.dia_semana
                       WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miércoles' THEN 3
                       WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 ELSE 9 END,
                     h.hora_inicio, s.id_solicitud
        """;
        if (estado == null || estado.isBlank()) {
            return jdbc.query(base + " " + order, RM, idAdminPiso);
        } else {
            return jdbc.query(base + " AND s.estado = ? " + order, RM, idAdminPiso, estado);
        }
    }

    public List<AdminSolicitudItemDTO> listarPorEstadoGlobal(String estado) {
        String base = """
            SELECT s.id_solicitud,
                   (p.nombres || ' ' || p.apellidos) AS docente,
                   s.materia, h.dia_semana, h.hora_inicio, h.hora_fin, h.jornada,
                   s.estado, s.estado_redireccion
            FROM public.solicitudasignacion s
            JOIN public.docente  d ON d.id_docente = s.id_docente
            JOIN public.persona  p ON p.id_persona = d.id_persona
            JOIN public.horario  h ON h.id_horario = s.id_horario
            WHERE 1=1
        """;
        String order = """
            ORDER BY CASE h.dia_semana
                       WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miércoles' THEN 3
                       WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 ELSE 9 END,
                     h.hora_inicio, s.id_solicitud
        """;
        if (estado == null || estado.isBlank()) {
            return jdbc.query(base + " " + order, RM);
        } else {
            return jdbc.query(base + " AND s.estado = ? " + order, RM, estado);
        }
    }

    /* ========= acciones — SOLO por id_solicitud ========= */

    public int aprobarPorId(Integer idSolicitud, Integer idLaboratorio) {
        boolean tieneColLab = hasColumn("solicitudasignacion", "id_laboratorio_asignado");

        if (tieneColLab) {
            String sql = """
                UPDATE public.solicitudasignacion
                   SET estado = 'Aprobada',
                       estado_redireccion = 'Aprobada por admin',
                       observaciones_admin = NULL,
                       id_laboratorio_asignado = COALESCE(?, id_laboratorio_asignado)
                 WHERE id_solicitud = ?
            """;
            return jdbc.update(sql, idLaboratorio, idSolicitud);
        } else {
            String sql = """
                UPDATE public.solicitudasignacion
                   SET estado = 'Aprobada',
                       estado_redireccion = 'Aprobada por admin',
                       observaciones_admin = NULL
                 WHERE id_solicitud = ?
            """;
            return jdbc.update(sql, idSolicitud);
        }
    }

    public int rechazarPorId(Integer idSolicitud, String motivo) {
        String sql = """
            UPDATE public.solicitudasignacion
               SET estado = 'Rechazada',
                   observaciones_admin = COALESCE(?, observaciones_admin)
             WHERE id_solicitud = ?
        """;
        return jdbc.update(sql, motivo, idSolicitud);
    }

    public int proponerPorId(Integer idSolicitud, Integer idHorarioAlt, Integer idLabAlt, String msg) {
        String nota = "Propuesta -> horario=" + idHorarioAlt + ", laboratorio=" + idLabAlt
                + (msg == null || msg.isBlank() ? "" : (". " + msg));
        String sql = """
            UPDATE public.solicitudasignacion
               SET estado = 'Revisión',
                   estado_redireccion = 'Propuesta enviada',
                   observaciones_admin = ?
             WHERE id_solicitud = ?
        """;
        return jdbc.update(sql, nota, idSolicitud);
    }

    /* ========= debug ========= */

    public int debugCount(Integer id) {
        String sql = "SELECT COUNT(*) FROM public.solicitudasignacion WHERE id_solicitud = ?";
        Integer c = jdbc.queryForObject(sql, Integer.class, id);
        return c == null ? 0 : c;
    }

    public String debugEstado(Integer id) {
        String sql = "SELECT '['||estado||']' FROM public.solicitudasignacion WHERE id_solicitud = ?";
        List<String> l = jdbc.query(sql, (rs,i)->rs.getString(1), id);
        return l.isEmpty()? null : l.get(0);
    }

    /* ========= DTOs y listas para modales ========= */

    private static final RowMapper<LabOpcionDTO> RM_LAB = (rs, i) -> {
        LabOpcionDTO dto = new LabOpcionDTO();
        dto.setId(rs.getInt("id"));
        dto.setNombre(rs.getString("nombre"));
        dto.setCapacidad((Integer) rs.getObject("capacidad"));
        dto.setEstado(rs.getString("estado"));
        dto.setPiso(rs.getString("piso"));
        dto.setDisponible(rs.getBoolean("disponible"));
        return dto;
    };

    public List<LabOpcionDTO> listarLaboratoriosParaSolicitud(Integer idSolicitud){
        String sql = """
            SELECT
              l.id_laboratorio          AS id,
              l.nombre_laboratorio      AS nombre,
              l.capacidad               AS capacidad,
              l.estado                  AS estado,
              COALESCE(p.descripcion,'') AS piso,
              NOT EXISTS (
                SELECT 1
                  FROM public.asignacion_laboratorio al
                  JOIN public.solicitudasignacion s2 ON s2.id_solicitud = al.id_solicitud
                 WHERE al.id_laboratorio = l.id_laboratorio
                   AND s2.estado = 'Aprobada'
                   AND s2.id_horario = h.id_horario
              ) AS disponible
            FROM public.laboratorio l
            LEFT JOIN public.piso p ON p.id_piso = l.id_piso
            JOIN public.solicitudasignacion s ON s.id_solicitud = ?
            JOIN public.horario h ON h.id_horario = s.id_horario
            ORDER BY l.nombre_laboratorio
        """;
        return jdbc.query(sql, RM_LAB, idSolicitud);
    }

    private static final RowMapper<HorarioOpcionDTO> RM_HOR = (rs, i) -> {
        HorarioOpcionDTO dto = new HorarioOpcionDTO();
        dto.setId(rs.getInt("id"));
        dto.setDiaSemana(rs.getString("dia_semana"));
        dto.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
        dto.setHoraFin(rs.getTime("hora_fin").toLocalTime());
        dto.setJornada(rs.getString("jornada"));
        dto.setDisponible(rs.getBoolean("disponible"));
        return dto;
    };

    /** Horarios por jornada para pintar la grilla del modal Proponer. */
    public List<HorarioOpcionDTO> listarHorariosParaSolicitud(Integer idSolicitud, String jornada) {
        String sql = """
            SELECT
              h.id_horario AS id,
              h.dia_semana,
              h.hora_inicio,
              h.hora_fin,
              h.jornada,
              NOT EXISTS (
                SELECT 1
                  FROM public.solicitudasignacion s2
                  JOIN public.asignacion_laboratorio al ON al.id_solicitud = s2.id_solicitud
                 WHERE s2.estado = 'Aprobada'
                   AND s2.id_horario = h.id_horario
              ) AS disponible
            FROM public.horario h
            /* Unimos a la solicitud únicamente para validar la existencia y poder filtrar si fuese necesario */
            JOIN public.solicitudasignacion s ON s.id_solicitud = ?
            WHERE h.jornada = ?
            ORDER BY CASE h.dia_semana
                       WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miércoles' THEN 3
                       WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 ELSE 9 END,
                     h.hora_inicio, h.id_horario
        """;
        return jdbc.query(sql, RM_HOR, idSolicitud, jornada);
    }
}
