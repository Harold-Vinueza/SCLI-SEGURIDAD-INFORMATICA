package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.CrearSolicitudRequest;
import com.uteq.SCLI.dto.SolicitudItemDTO;
import com.uteq.SCLI.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SolicitudReservaService {

    private final SolicitudRepository solicitudRepository;
    private final JdbcTemplate jdbc;
    private final ReservaTemporalService reservaTemporalService;

    /** Fija la GUC para RLS y evita errores usando queryForObject. */
    private void setDocenteGUC(Integer idDocente) {
        Objects.requireNonNull(idDocente, "idDocente no puede ser null");
        jdbc.queryForObject(
                "SELECT set_config('app.current_docente_id', ?, true)",
                String.class,
                String.valueOf(idDocente)
        );
    }

    /** Verifica que la materia pertenezca al docente (usa DocenteMateria). */
    private boolean materiaPerteneceADocente(Integer idDocente, Integer idMateria) {
        try {
            Integer n = jdbc.queryForObject(
                    """
                    SELECT 1
                      FROM DocenteMateria dm
                     WHERE dm.id_docente = ? AND dm.id_materia = ?
                     LIMIT 1
                    """,
                    Integer.class, idDocente, idMateria
            );
            return n != null;
        } catch (Exception ignore) {
            return false;
        }
    }

    /** Intenta hallar el admin de piso asociado al horario por un laboratorio relacionado. */
    private Integer resolverAdminPisoPorHorario(Integer idHorario) {
        try {
            return jdbc.queryForObject(
                    """
                    SELECT ap.id_admin_piso
                      FROM public.horario h
                      JOIN public.asignacion_laboratorio al ON al.id_horario = h.id_horario
                      JOIN public.laboratorio l  ON l.id_laboratorio = al.id_laboratorio
                      JOIN public.piso p         ON p.id_piso        = l.id_piso
                      JOIN public.administradorpiso ap ON ap.id_piso = p.id_piso
                     WHERE h.id_horario = ?
                     ORDER BY ap.id_admin_piso
                     LIMIT 1
                    """,
                    Integer.class, idHorario
            );
        } catch (Exception ignore) {}

        try {
            return jdbc.queryForObject(
                    "SELECT ap.id_admin_piso FROM public.administradorpiso ap ORDER BY ap.id_admin_piso LIMIT 1",
                    Integer.class
            );
        } catch (Exception ignore) {}

        return null;
    }

    /* ===========================
       Listado para el frontend
       =========================== */
    @Transactional(readOnly = true)
    public List<SolicitudItemDTO> misSolicitudes(Integer idDocente) {
        setDocenteGUC(idDocente);

        List<Object[]> rows = solicitudRepository.findMisSolicitudes(idDocente);
        List<SolicitudItemDTO> out = new ArrayList<>();
        for (Object[] r : rows) {
            SolicitudItemDTO dto = new SolicitudItemDTO();
            int i = 0;
            dto.setIdSolicitud((Integer) r[i++]);
            dto.setEstado((String) r[i++]);
            dto.setEstadoRedireccion((String) r[i++]);
            dto.setFechaSolicitud((r[i] != null) ? ((java.sql.Date) r[i]).toLocalDate() : null); i++;
            dto.setIdHorario((Integer) r[i++]);
            dto.setDiaSemana((String) r[i++]);
            dto.setHoraInicio(((java.sql.Time) r[i++]).toLocalTime());
            dto.setHoraFin(((java.sql.Time) r[i++]).toLocalTime());
            dto.setJornada((String) r[i++]);
            dto.setMateria((String) r[i++]);
            dto.setTipoSolicitud((String) r[i++]);
            out.add(dto);
        }
        return out;
    }

    /* ===========================
       Crear solicitud (usa fecha del modal)
       =========================== */
    @Transactional
    public Integer crearSolicitud(Integer idDocente, CrearSolicitudRequest req) {
        setDocenteGUC(idDocente);

        if (req.getIdHorario() == null)
            throw new IllegalArgumentException("idHorario es requerido");

        if (req.getIdMateria() == null)
            throw new IllegalArgumentException("idMateria es requerido");

        if (req.getFechaUso() == null)
            throw new IllegalArgumentException("fechaUso es requerida (YYYY-MM-DD)");

        // Si no envían tipo, por defecto tratamos como Temporal
        if (req.getTipoSolicitud() == null || req.getTipoSolicitud().isBlank())
            req.setTipoSolicitud("Temporal");

        // Validar relación docente-materia (si aplica)
        if (!materiaPerteneceADocente(idDocente, req.getIdMateria()))
            throw new IllegalArgumentException("La materia seleccionada no pertenece al docente.");

        // Resolver admin de piso (necesario por NOT NULL en la tabla)
        Integer idAdminPiso = req.getIdAdminPiso();
        if (idAdminPiso == null) {
            idAdminPiso = resolverAdminPisoPorHorario(req.getIdHorario());
            if (idAdminPiso == null) {
                throw new IllegalStateException(
                        "No se encontró Administrador de Piso para el horario " + req.getIdHorario() +
                                " y no existe un admin de piso de fallback. Crea al menos un Administrador de Piso."
                );
            }
        }

        // Insert en solicitudasignacion (con fecha elegida)
        reservaTemporalService.crearSolicitudTemporal(
                idDocente,
                req.getIdMateria(),
                req.getIdHorario(),
                (req.getMotivo() == null ? "" : req.getMotivo().trim()),
                req.getFechaUso(),
                req.getTipoSolicitud(),
                idAdminPiso  // <<--- ahora se envía y se inserta
        );

        // Si quisieras devolver el id real, tendrías que consultarlo luego del insert.
        return 1;
    }

    /* ===== Aceptar/Rechazar propuesta (Docente) ===== */

    @Transactional
    public int docenteAceptarPropuesta(Integer idDocente, Integer idSolicitud) {
        setDocenteGUC(idDocente);
        String sql = """
            UPDATE public.solicitudasignacion
               SET estado = 'Pendiente',
                   estado_redireccion = 'Prop. aceptada'
             WHERE id_solicitud = ?
               AND id_docente   = ?
               AND TRIM(estado) ILIKE 'revisi%%'
        """;
        return jdbc.update(sql, idSolicitud, idDocente);
    }

    @Transactional
    public int docenteRechazarPropuesta(Integer idDocente, Integer idSolicitud) {
        setDocenteGUC(idDocente);
        String sql = """
            UPDATE public.solicitudasignacion
               SET estado = 'Rechazada',
                   estado_redireccion = 'Prop. rechazada'
             WHERE id_solicitud = ?
               AND id_docente   = ?
               AND TRIM(estado) ILIKE 'revisi%%'
        """;
        return jdbc.update(sql, idSolicitud, idDocente);
    }
}