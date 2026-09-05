package com.uteq.SCLI.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservaTemporalService {

    private final JdbcTemplate jdbc;

    // Tablas
    private static final String T_SOL   = "public.solicitudasignacion";
    private static final String T_ASIG  = "public.asignacion_laboratorio";
    private static final String T_HOR   = "public.horario";
    private static final String T_DOC   = "public.docente";
    private static final String T_PER   = "public.persona";
    private static final String T_MAT   = "public.materia";
    private static final String T_PERI  = "public.periodolectivo";

    private static final Integer DEFAULT_PERIODO_ID = 2;

    // ===================== Crear solicitud (usa fecha_solicitud del modal) =====================
    @Transactional
    public void crearSolicitudTemporal(Integer idDocente,
                                       Integer idMateria,
                                       Integer idHorario,
                                       String motivo,
                                       LocalDate fechaUso,
                                       String tipoSolicitud,
                                       Integer idAdminPiso) {
        if (idDocente == null || idHorario == null || fechaUso == null) {
            throw new IllegalArgumentException("Faltan datos obligatorios (docente, horario o fecha).");
        }
        if (fechaUso.getDayOfWeek() == DayOfWeek.SATURDAY || fechaUso.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("No se permiten sábados ni domingos.");
        }
        if (idAdminPiso == null) {
            throw new IllegalArgumentException("id_admin_piso es requerido para solicitudasignacion.");
        }

        // Obtiene el nombre de materia exacto según tu tabla: public.materia.nombre_materia
        String materiaNombre = "";
        if (idMateria != null) {
            try {
                materiaNombre = jdbc.queryForObject(
                        "SELECT nombre_materia FROM " + T_MAT + " WHERE id_materia = ?",
                        String.class, idMateria
                );
                if (materiaNombre == null) materiaNombre = "";
            } catch (DataAccessException ignore) {
                materiaNombre = "";
            }
        }

        // Insert incluyendo columnas no nulas: estado_redireccion e id_admin_piso
        try {
            jdbc.update("""
                INSERT INTO public.solicitudasignacion
                  (id_docente, id_horario, materia, tipo_solicitud, estado,
                   estado_redireccion, observaciones_admin, fecha_solicitud, id_admin_piso)
                VALUES (?, ?, ?, COALESCE(?, 'Temporal'), 'Pendiente',
                        'Sin redirección', ?, ?, ?)
            """,
                    idDocente, idHorario, materiaNombre, tipoSolicitud,
                    (motivo == null ? "" : motivo),
                    Date.valueOf(fechaUso),
                    idAdminPiso
            );
        } catch (DataAccessException ex) {
            String msg = (ex.getMostSpecificCause() != null)
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();
            throw new IllegalStateException("No se pudo insertar la solicitud temporal: " + msg, ex);
        }
    }

    // ==================== Aprobar grupo (copiando fecha_solicitud a fecha_asignacion) ====================
    @Transactional
    public void aprobarGrupoUsandoFechaSolicitud(String grupoId, Integer idLaboratorio) {
        if (grupoId == null || grupoId.isBlank() || idLaboratorio == null) {
            throw new IllegalArgumentException("grupoId o idLaboratorio inválidos.");
        }

        Integer idPeriodoVigente = periodoVigenteId();

        boolean isLote = grupoId.startsWith("LOTE:");
        boolean isAgr  = grupoId.startsWith("AGR:");

        List<Map<String, Object>> solicitudes;

        if (isLote) {
            String loteRaw = grupoId.substring("LOTE:".length()).trim();
            Object loteParam;
            try { loteParam = Integer.valueOf(loteRaw); }
            catch (NumberFormatException ex) { loteParam = loteRaw; }

            solicitudes = jdbc.queryForList("""
                SELECT s.id_solicitud, s.id_horario, s.id_docente, s.materia, s.fecha_solicitud
                  FROM public.solicitudasignacion s
                 WHERE s.estado IN ('Pendiente','Revisión','Revision')
                   AND s.lote = ?
            """, loteParam);

        } else if (isAgr) {
            String payload = grupoId.substring("AGR:".length());
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("Formato AGR inválido: " + grupoId);
            }
            String docenteNombre = parts[0].trim();
            String materiaNombre = parts[1].trim();
            String jornada       = parts[2].trim();

            Integer idDoc = jdbc.query(
                    """
                    SELECT d.id_docente
                      FROM public.docente d
                      JOIN public.persona p ON p.id_persona = d.id_persona
                     WHERE UPPER(TRIM(CONCAT_WS(' ', p.nombres, p.apellidos))) = ?
                     LIMIT 1
                    """,
                    ps -> ps.setString(1, docenteNombre.replaceAll("\\s+"," ").trim().toUpperCase()),
                    rs -> rs.next() ? rs.getInt(1) : null
            );
            if (idDoc == null) {
                throw new IllegalStateException("No se encontró id_docente para: " + docenteNombre);
            }

            solicitudes = jdbc.queryForList("""
                SELECT s.id_solicitud, s.id_horario, s.id_docente, s.materia, s.fecha_solicitud
                  FROM public.solicitudasignacion s
                  JOIN public.horario h ON h.id_horario = s.id_horario
                 WHERE s.estado IN ('Pendiente','Revisión','Revision')
                   AND s.id_docente = ?
                   AND UPPER(TRIM(s.materia)) = UPPER(TRIM(?))
                   AND UPPER(TRIM(h.jornada)) = UPPER(TRIM(?))
            """, idDoc, materiaNombre, jornada);

        } else {
            throw new IllegalArgumentException("Tipo de grupo no soportado: " + grupoId);
        }

        if (solicitudes.isEmpty()) {
            throw new IllegalStateException("No hay solicitudes en el grupo seleccionado.");
        }

        // Validar choques por FECHA (mismo horario y misma fecha)
        for (var r : solicitudes) {
            Integer idHorario = ((Number) r.get("id_horario")).intValue();
            Date fsol = (Date) r.get("fecha_solicitud");
            if (fsol == null) throw new IllegalStateException("Solicitud sin fecha_solicitud definida.");
            LocalDate fechaUso = fsol.toLocalDate();

            Integer ocupado = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM public.asignacion_laboratorio
                 WHERE id_horario = ? AND fecha_asignacion = ?
            """, Integer.class, idHorario, Date.valueOf(fechaUso));

            if (ocupado != null && ocupado > 0) {
                throw new IllegalStateException("Choque de horario para la fecha " + fechaUso);
            }
        }

        // Insertar asignaciones (id_materia = NULL; materia = texto de solicitud) y actualizar estado
        for (var r : solicitudes) {
            Integer idSolicitud = ((Number) r.get("id_solicitud")).intValue();
            Integer idHorario   = ((Number) r.get("id_horario")).intValue();
            Integer idDocente   = ((Number) r.get("id_docente")).intValue();
            String  materiaTxt  = String.valueOf(r.get("materia"));
            LocalDate fechaUso  = ((Date) r.get("fecha_solicitud")).toLocalDate();

            jdbc.update("""
                INSERT INTO public.asignacion_laboratorio
                  (id_solicitud, id_horario, id_laboratorio, fecha_asignacion,
                   id_periodo, id_admin_piso, id_docente, id_materia, materia)
                VALUES (?, ?, ?, ?, ?, NULL, ?, NULL, ?)
                ON CONFLICT DO NOTHING
            """, idSolicitud, idHorario, idLaboratorio, Date.valueOf(fechaUso),
                    idPeriodoVigente,
                    idDocente, materiaTxt);

            jdbc.update("UPDATE public.solicitudasignacion SET estado='Aprobada' WHERE id_solicitud = ?",
                    idSolicitud);
        }
    }

    // ===================== Limpieza diaria (solo TEMPORALES) =====================
    @Transactional
    public void limpiarPasados() {
        // 1) Eliminar SOLO asignaciones que provienen de solicitudes TEMPORALES y ya pasaron
        jdbc.update("""
            DELETE FROM public.asignacion_laboratorio al
            USING public.solicitudasignacion s
            WHERE al.id_solicitud = s.id_solicitud
              AND s.tipo_solicitud ILIKE 'Temporal'
              AND al.fecha_asignacion < CURRENT_DATE
        """);

        // 2) Marcar como Expirada SOLO las solicitudes TEMPORALES pasadas
        jdbc.update("""
            UPDATE public.solicitudasignacion
               SET estado = 'Expirada'
             WHERE tipo_solicitud ILIKE 'Temporal'
               AND fecha_solicitud < CURRENT_DATE
               AND estado IN ('Pendiente','Revisión','Revision','Aprobada')
        """);
    }

    // ===================== Período lectivo vigente (periodolectivo) =====================
    private Integer periodoVigenteId() {
        try {
            Integer id = jdbc.queryForObject("""
                SELECT id_periodo
                  FROM public.periodolectivo
                 WHERE activo = true
                 ORDER BY fecha_inicio DESC NULLS LAST
                 LIMIT 1
            """, Integer.class);
            if (id != null) return id;
        } catch (Exception ignored) {}

        try {
            Integer id = jdbc.queryForObject("""
                SELECT id_periodo
                  FROM public.periodolectivo
                 WHERE CURRENT_DATE BETWEEN COALESCE(fecha_inicio, CURRENT_DATE)
                                       AND COALESCE(fecha_fin, CURRENT_DATE)
                 ORDER BY fecha_inicio DESC NULLS LAST
                 LIMIT 1
            """, Integer.class);
            if (id != null) return id;
        } catch (Exception ignored) {}

        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM public.periodolectivo WHERE id_periodo = ?",
                    Integer.class, DEFAULT_PERIODO_ID
            );
            if (count != null && count > 0) return DEFAULT_PERIODO_ID;
        } catch (Exception ignored) {}

        try {
            Integer id = jdbc.queryForObject("""
                SELECT id_periodo
                  FROM public.periodolectivo
                 ORDER BY id_periodo DESC
                 LIMIT 1
            """, Integer.class);
            if (id != null) return id;
        } catch (Exception ignored) {}

        throw new IllegalStateException("No se encontró un período en public.periodolectivo. Crea uno o ajusta DEFAULT_PERIODO_ID.");
    }
}