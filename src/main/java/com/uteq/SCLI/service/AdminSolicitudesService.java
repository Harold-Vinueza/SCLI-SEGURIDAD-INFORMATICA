package com.uteq.SCLI.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para listar solicitudes agrupadas (por lote o docente+motivo+jornada),
 * validar laboratorios y aprobar/rechazar/proponer en bloque.
 * Guarda siempre id_materia (si es posible) y el NOMBRE REAL de la materia en asignacion_laboratorio.materia.
 * (Sin pre-chequeo de choques de docente: dejamos que el trigger decida; solo traducimos el error).
 */
@Service
@RequiredArgsConstructor
public class AdminSolicitudesService {

    @PersistenceContext
    private EntityManager em;

    // ====== DTOs internos ligeros ======
    public record BloqueDTO(Integer idHorario, String diaSemana, Time horaInicio, Time horaFin) {}
    public record GrupoDTO(
            String grupoId,                // "LOTE:<lote>" o "AGR:<docente>|<motivo>|<jornada>"
            String docente, String materia, String jornada, String estado,
            List<BloqueDTO> bloques,       // todos los horarios del grupo
            List<Long> solicitudesIds      // ids de s.id_solicitud que pertenecen al grupo
    ) {}

    // ====== Listar agrupadas ======
    @SuppressWarnings("unchecked")
    public List<GrupoDTO> listarAgrupadas(String estado) {
        boolean filtrarPorEstado = (estado != null && !estado.isBlank());

        String sql = """
          SELECT
            s.id_solicitud                         AS id_solicitud,
            COALESCE(s.lote, '')                   AS lote,
            (p.nombres || ' ' || p.apellidos)      AS docente,
            s.materia                              AS materia,   -- aquí es el MOTIVO de la solicitud
            h.jornada                              AS jornada,
            s.estado                               AS estado,
            h.id_horario                           AS id_horario,
            h.dia_semana                           AS dia_semana,
            h.hora_inicio                          AS hora_inicio,
            h.hora_fin                             AS hora_fin
          FROM public.solicitudasignacion s
          JOIN public.docente  d ON d.id_docente = s.id_docente
          JOIN public.persona  p ON p.id_persona = d.id_persona
          JOIN public.horario  h ON h.id_horario = s.id_horario
        """;

        if (filtrarPorEstado) {
            sql += " WHERE s.estado = :estado ";
        }

        sql += """
          ORDER BY s.materia ASC,
                   (p.nombres || ' ' || p.apellidos) ASC,
                   h.dia_semana,
                   h.hora_inicio
        """;

        var q = em.createNativeQuery(sql);
        if (filtrarPorEstado) q.setParameter("estado", estado);

        List<Object[]> rows = q.getResultList();

        final int ID_SOL=0, LOTE=1, DOC=2, MAT=3, JOR=4, EST=5, ID_HOR=6, DIA=7, HINI=8, HFIN=9;

        Map<String, GrupoDTO> grupos = new LinkedHashMap<>();

        for (Object[] r : rows) {
            Long idSol     = ((Number) r[ID_SOL]).longValue();
            String lote    = String.valueOf(r[LOTE]);
            String docente = String.valueOf(r[DOC]);
            String motivo  = String.valueOf(r[MAT]); // MOTIVO (etiqueta 'materia' en solicitud)
            String jornada = String.valueOf(r[JOR]);
            String est     = String.valueOf(r[EST]);
            Integer idHor  = ((Number) r[ID_HOR]).intValue();
            String dia     = String.valueOf(r[DIA]);
            Time hi        = (Time) r[HINI];
            Time hf        = (Time) r[HFIN];

            String key = (lote != null && !lote.isBlank())
                    ? "LOTE:" + lote
                    : "AGR:" + docente + "|" + motivo + "|" + jornada;

            grupos.compute(key, (k, g) -> {
                if (g == null) {
                    return new GrupoDTO(
                            key, docente, motivo, jornada, est,
                            new ArrayList<>(List.of(new BloqueDTO(idHor, dia, hi, hf))),
                            new ArrayList<>(List.of(idSol))
                    );
                } else {
                    g.bloques().add(new BloqueDTO(idHor, dia, hi, hf));
                    g.solicitudesIds().add(idSol);
                    return g;
                }
            });
        }

        // ordenar bloques dentro de cada grupo
        List<String> ordenDias = List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo");
        Comparator<BloqueDTO> cmp = Comparator
                .comparing((BloqueDTO b) -> {
                    int idx = ordenDias.indexOf(b.diaSemana());
                    return idx < 0 ? 99 : idx;
                })
                .thenComparing(BloqueDTO::horaInicio);

        return grupos.values().stream()
                .peek(g -> g.bloques().sort(cmp))
                .collect(Collectors.toList());
    }

    // ====== Laboratorios disponibles para TODO el grupo ======
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> labsParaGrupo(String grupoId) {
        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst().orElse(null);
        if (g == null) return List.of();

        String qLabs = """
          SELECT l.id_laboratorio,
                 l.nombre_laboratorio     AS nombre,
                 l.capacidad,
                 l.estado,
                 COALESCE(p.numero_piso, 0) AS piso
          FROM public.laboratorio l
          LEFT JOIN public.piso p ON p.id_piso = l.id_piso
          WHERE l.estado IS DISTINCT FROM 'Baja'
          ORDER BY l.nombre_laboratorio
        """;
        List<Object[]> labs = em.createNativeQuery(qLabs).getResultList();

        List<Map<String,Object>> out = new ArrayList<>();
        for (Object[] L : labs) {
            Integer idLab = ((Number) L[0]).intValue();
            String nombre = String.valueOf(L[1]);
            Integer cap   = L[2]==null? null : ((Number)L[2]).intValue();
            String est    = String.valueOf(L[3]);
            Integer piso  = L[4]==null? null : ((Number)L[4]).intValue();

            boolean libreEnTodos = true;
            for (BloqueDTO b : g.bloques()) {
                String conf = """
                  SELECT 1
                  FROM public.asignacion_laboratorio al
                  JOIN public.horario h2 ON h2.id_horario = al.id_horario
                  WHERE al.id_laboratorio = :lab
                    AND h2.dia_semana = :dia
                    AND NOT (h2.hora_fin <= :ini OR h2.hora_inicio >= :fin)
                  LIMIT 1
                """;
                List<?> rs = em.createNativeQuery(conf)
                        .setParameter("lab", idLab)
                        .setParameter("dia", b.diaSemana())
                        .setParameter("ini", b.horaInicio())
                        .setParameter("fin", b.horaFin())
                        .getResultList();

                if (!rs.isEmpty()) { libreEnTodos = false; break; }
            }

            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", idLab);
            m.put("nombre", nombre);
            m.put("capacidad", cap);
            m.put("estado", est);
            m.put("piso", piso);
            m.put("disponible", libreEnTodos);
            out.add(m);
        }
        return out;
    }

    // ====== Helpers de período lectivo ======
    private Integer periodoActivoId() {
        try {
            Object r = em.createNativeQuery("""
                SELECT id_periodo
                FROM public.periodolectivo
                WHERE activo = TRUE
                ORDER BY fecha_inicio DESC
                LIMIT 1
            """).getSingleResult();
            return (r == null) ? null : ((Number) r).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Date fechaAsignacionPreferida(Long idSolicitud) {
        try {
            Object r = em.createNativeQuery("""
                SELECT COALESCE(s.fecha_solicitud, CURRENT_DATE)
                FROM public.solicitudasignacion s
                WHERE s.id_solicitud = :id
            """).setParameter("id", idSolicitud).getSingleResult();
            return (Date) r;
        } catch (Exception ignored) {
            return new Date(System.currentTimeMillis());
        }
    }

    // ====== Resolver Admin de Piso a partir del laboratorio ======
    private Integer adminPisoPorLaboratorio(Integer idLaboratorio) {
        try {
            Object r = em.createNativeQuery("""
                SELECT ap.id_admin_piso
                  FROM public.administradorpiso ap
                  JOIN public.piso p ON p.id_piso = ap.id_piso
                  JOIN public.laboratorio l ON l.id_piso = p.id_piso
                 WHERE l.id_laboratorio = :lab
                 ORDER BY ap.id_admin_piso
                 LIMIT 1
            """).setParameter("lab", idLaboratorio).getSingleResult();
            return (r == null) ? null : ((Number) r).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    // ---- helpers de Materia ----
    private Integer parseIdMateriaDesdeLote(Object lote) {
        if (lote == null) return null;
        try {
            String s = String.valueOf(lote).trim();
            if (s.isEmpty()) return null;
            return Integer.valueOf(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String nombreMateriaPorId(Integer idMateria) {
        if (idMateria == null) return null;
        try {
            Object r = em.createNativeQuery("""
                SELECT nombre_materia
                  FROM public.materia
                 WHERE id_materia = :id
                 LIMIT 1
            """).setParameter("id", idMateria).getSingleResult();
            return (r == null) ? null : String.valueOf(r);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer idMateriaPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        try {
            Object r = em.createNativeQuery("""
                SELECT id_materia
                  FROM public.materia
                 WHERE nombre_materia = :n
                 LIMIT 1
            """).setParameter("n", nombre).getSingleResult();
            return (r == null) ? null : ((Number) r).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private record ResMateria(Integer id, String nombre){}

    /**
     * Resuelve (id_materia, nombre) con prioridad:
     * - override explícito si viene idMateriaOverride,
     * - lote (numérico) como id,
     * - búsqueda exacta por nombre desde motivo,
     * - fallback: id=null, nombre = motivo.
     */
    private ResMateria resolverMateria(Object valLote, Object valMotivo, Integer idMateriaOverride) {
        if (idMateriaOverride != null) {
            String nombre = nombreMateriaPorId(idMateriaOverride);
            if (nombre == null) nombre = String.valueOf(valMotivo == null ? "" : valMotivo);
            return new ResMateria(idMateriaOverride, nombre);
        }

        Integer idMat = parseIdMateriaDesdeLote(valLote);
        String nombre = nombreMateriaPorId(idMat);
        String motivo = (valMotivo == null) ? null : String.valueOf(valMotivo);

        if (idMat == null) {
            Integer idPorNombre = idMateriaPorNombre(motivo);
            if (idPorNombre != null) {
                idMat = idPorNombre;
                nombre = nombreMateriaPorId(idMat);
            }
        }
        if (nombre == null) nombre = motivo; // último recurso: guarda el texto de la solicitud
        return new ResMateria(idMat, nombre);
    }

    // Mensaje amigable si el trigger de choque salta
    private RuntimeException translateChoque(RuntimeException ex){
        String msg = String.valueOf(ex.getMessage());
        if (msg != null && msg.toLowerCase().contains("choque de docente")) {
            return new IllegalStateException("Choque de docente: ya tiene una asignación en ese bloque y período.");
        }
        return ex;
    }

    // ====== Aprobar en bloque (flujo tradicional: admin elige laboratorio) ======
    @Transactional
    public void aprobarGrupo(String grupoId, Integer idLaboratorio, Integer idMateriaOverride) {
        // Validación de disponibilidad previa SOLO del laboratorio
        List<Map<String,Object>> labs = labsParaGrupo(grupoId);
        boolean ok = labs.stream()
                .anyMatch(m -> Objects.equals(((Number)m.get("id")).intValue(), idLaboratorio)
                        && Boolean.TRUE.equals(m.get("disponible")));
        if (!ok) throw new IllegalStateException("El laboratorio no está disponible para todas las horas del grupo.");

        Integer idPeriodo = periodoActivoId();
        if (idPeriodo == null) {
            throw new IllegalStateException("No existe un período lectivo activo (periodolectivo.activo=TRUE).");
        }
        Integer idAdminPiso = adminPisoPorLaboratorio(idLaboratorio);

        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        String ins = """
          INSERT INTO public.asignacion_laboratorio
            (id_solicitud, id_horario, id_laboratorio, fecha_asignacion, id_periodo, id_admin_piso,
             id_docente, id_materia, materia)
          VALUES (:idSol, :idHor, :idLab, :fec, :idPer, :idAdmin,
                  :idDoc, :idMat, :matNombre)
          ON CONFLICT DO NOTHING
        """;

        String upd = """
          UPDATE public.solicitudasignacion
             SET estado='Aprobada',
                 id_admin_piso = COALESCE(id_admin_piso, :idAdmin)
           WHERE id_solicitud = :idSol
        """;

        String qSolInfo = """
          SELECT s.id_solicitud, h.id_horario, s.id_docente, s.materia, s.lote
            FROM public.solicitudasignacion s
            JOIN public.horario h ON h.id_horario = s.id_horario
           WHERE s.id_solicitud = :id
        """;

        for (Long idSol : g.solicitudesIds()) {
            Object[] r = (Object[]) em.createNativeQuery(qSolInfo)
                    .setParameter("id", idSol)
                    .getSingleResult();

            Long    idS     = ((Number) r[0]).longValue();
            Integer idH     = ((Number) r[1]).intValue();         // horario original
            Integer idDoc   = (r[2] == null) ? null : ((Number) r[2]).intValue();
            Object  motivo  = r[3];                               // texto
            Object  lote    = r[4];                               // posible id materia en string

            ResMateria rm = resolverMateria(lote, motivo, idMateriaOverride);
            Date fecha = fechaAsignacionPreferida(idS);

            try {
                em.createNativeQuery(ins)
                        .setParameter("idSol", idS)
                        .setParameter("idHor", idH)
                        .setParameter("idLab", idLaboratorio)
                        .setParameter("fec", fecha)
                        .setParameter("idPer", idPeriodo)
                        .setParameter("idAdmin", idAdminPiso)
                        .setParameter("idDoc", idDoc)
                        .setParameter("idMat", rm.id())
                        .setParameter("matNombre", rm.nombre())
                        .executeUpdate();
            } catch (RuntimeException ex) {
                throw translateChoque(ex);
            }

            em.createNativeQuery(upd)
                    .setParameter("idSol", idS)
                    .setParameter("idAdmin", idAdminPiso)
                    .executeUpdate();
        }
    }

    // ===== Helper: ajustar fecha al PRÓXIMO día de semana objetivo =====
    private static LocalDate ajustarFechaAlDiaSemana(LocalDate base, String diaSemanaObjetivo) {
        Map<String, DayOfWeek> map = Map.of(
                "Lunes", DayOfWeek.MONDAY,
                "Martes", DayOfWeek.TUESDAY,
                "Miércoles", DayOfWeek.WEDNESDAY,
                "Miercoles", DayOfWeek.WEDNESDAY, // por si acaso sin tilde
                "Jueves", DayOfWeek.THURSDAY,
                "Viernes", DayOfWeek.FRIDAY,
                "Sábado", DayOfWeek.SATURDAY,
                "Sabado", DayOfWeek.SATURDAY,
                "Domingo", DayOfWeek.SUNDAY
        );
        DayOfWeek target = map.getOrDefault(diaSemanaObjetivo, DayOfWeek.MONDAY);
        int current = base.getDayOfWeek().getValue(); // 1..7
        int desired = target.getValue();              // 1..7
        int delta = desired - current;
        if (delta < 0) delta += 7;                    // próximo día objetivo
        return base.plusDays(delta);
    }

    // ====== Aprobar por propuesta (sin volver a elegir laboratorio) ======
    @Transactional
    public void aprobarPorPropuesta(String grupoId, Integer idMateriaOverride) {
        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        // Propuestas 1:1
        String sqlProps = """
            SELECT sp.id_solicitud, sp.id_laboratorio_propuesto, sp.id_horario_propuesto
              FROM public.solicitud_propuesta sp
             WHERE sp.id_solicitud = ANY(:ids)
        """;
        List<Object[]> props = em.createNativeQuery(sqlProps)
                .setParameter("ids", g.solicitudesIds().toArray(Long[]::new))
                .getResultList();

        if (props.size() != g.solicitudesIds().size()) {
            throw new IllegalStateException("Faltan propuestas para una o más solicitudes del grupo.");
        }

        // Laboratorio único
        Set<Integer> labs = props.stream()
                .map(r -> ((Number) r[1]).intValue())
                .collect(Collectors.toSet());
        if (labs.size() != 1) {
            throw new IllegalStateException("Las propuestas no coinciden en el laboratorio.");
        }
        Integer idLaboratorio = labs.iterator().next();

        // Validar disponibilidad del laboratorio (por bloque)
        String qHorario = "SELECT h.dia_semana, h.hora_inicio, h.hora_fin FROM public.horario h WHERE h.id_horario = :id";
        String qConf = """
            SELECT 1 FROM public.asignacion_laboratorio al
             JOIN public.horario h2 ON h2.id_horario = al.id_horario
            WHERE al.id_laboratorio = :lab
              AND h2.dia_semana = :dia
              AND NOT (h2.hora_fin <= :ini OR h2.hora_inicio >= :fin)
            LIMIT 1
        """;
        Map<Integer,String> diaPorHorario = new HashMap<>();
        for (Object[] pr : props) {
            Integer idHorProp = ((Number) pr[2]).intValue();
            Object[] h = (Object[]) em.createNativeQuery(qHorario)
                    .setParameter("id", idHorProp)
                    .getSingleResult();
            String dia = String.valueOf(h[0]);
            Time ini   = (Time) h[1];
            Time fin   = (Time) h[2];
            diaPorHorario.put(idHorProp, dia);

            List<?> conf = em.createNativeQuery(qConf)
                    .setParameter("lab", idLaboratorio)
                    .setParameter("dia", dia)
                    .setParameter("ini", ini)
                    .setParameter("fin", fin)
                    .getResultList();
            if (!conf.isEmpty()) {
                throw new IllegalStateException("El laboratorio propuesto ya no está disponible.");
            }
        }

        Integer idPeriodo = periodoActivoId();
        if (idPeriodo == null) throw new IllegalStateException("No hay período lectivo activo.");
        Integer idAdminPiso = adminPisoPorLaboratorio(idLaboratorio);

        String qSolInfo = """
          SELECT s.id_docente, s.materia, s.lote, COALESCE(s.fecha_solicitud, CURRENT_DATE)
            FROM public.solicitudasignacion s
           WHERE s.id_solicitud = :id
        """;
        String ins = """
          INSERT INTO public.asignacion_laboratorio
            (id_solicitud, id_horario, id_laboratorio, fecha_asignacion, id_periodo, id_admin_piso,
             id_docente, id_materia, materia)
          VALUES (:idSol, :idHor, :idLab, :fec, :idPer, :idAdmin, :idDoc, :idMat, :matNombre)
          ON CONFLICT DO NOTHING
        """;
        String upd = """
          UPDATE public.solicitudasignacion
             SET estado='Aprobada',
                 id_admin_piso = COALESCE(id_admin_piso, :idAdmin)
           WHERE id_solicitud = :idSol
        """;
        String delProp = "DELETE FROM public.solicitud_propuesta WHERE id_solicitud = :idSol";

        Map<Long,Integer> horarioPropuestoPorSolicitud = props.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[2]).intValue()
                ));

        for (Long idSol : g.solicitudesIds()) {
            Object[] s = (Object[]) em.createNativeQuery(qSolInfo)
                    .setParameter("id", idSol)
                    .getSingleResult();

            Integer idDoc   = (s[0] == null) ? null : ((Number) s[0]).intValue();
            Object  motivo  = s[1];
            Object  lote    = s[2];
            Date    fecOrig = (Date) s[3];
            Integer idHorProp = horarioPropuestoPorSolicitud.get(idSol);

            ResMateria rm = resolverMateria(lote, motivo, idMateriaOverride);

            // ===== AJUSTE DE FECHA: mover la fecha de la solicitud al PRÓXIMO día del horario propuesto =====
            String diaSemanaProp = diaPorHorario.get(idHorProp);
            LocalDate base = (fecOrig != null ? fecOrig.toLocalDate() : LocalDate.now());
            LocalDate fechaAjustada = ajustarFechaAlDiaSemana(base, diaSemanaProp);
            Date fecFinal = Date.valueOf(fechaAjustada);

            try {
                em.createNativeQuery(ins)
                        .setParameter("idSol", idSol)
                        .setParameter("idHor", idHorProp)
                        .setParameter("idLab", idLaboratorio)
                        .setParameter("fec", fecFinal)
                        .setParameter("idPer", idPeriodo)
                        .setParameter("idAdmin", idAdminPiso)
                        .setParameter("idDoc", idDoc)
                        .setParameter("idMat", rm.id())
                        .setParameter("matNombre", rm.nombre())
                        .executeUpdate();
            } catch (RuntimeException ex) {
                throw translateChoque(ex);
            }

            em.createNativeQuery(upd)
                    .setParameter("idSol", idSol)
                    .setParameter("idAdmin", idAdminPiso)
                    .executeUpdate();

            em.createNativeQuery(delProp)
                    .setParameter("idSol", idSol)
                    .executeUpdate();
        }
    }

    // ====== Rechazar en bloque ======
    @Transactional
    public void rechazarGrupo(String grupoId /*, String motivoOpcional */) {
        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        String upd = """
          UPDATE public.solicitudasignacion
             SET estado = 'Rechazada'
           WHERE id_solicitud = :idSol
        """;

        for (Long idSol : g.solicitudesIds()) {
            em.createNativeQuery(upd)
                    .setParameter("idSol", idSol)
                    .executeUpdate();
        }
    }

    // ====== Horarios para propuesta (grilla) ======
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> horariosParaPropuestaGrupo(String grupoId, String jornada, Integer idLaboratorio) {
        String q = """
          SELECT h.id_horario, h.dia_semana, h.hora_inicio, h.hora_fin, h.jornada
            FROM public.horario h
           WHERE (:j IS NULL OR h.jornada = :j)
           ORDER BY CASE h.dia_semana
                      WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miércoles' THEN 3
                      WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 WHEN 'Sábado' THEN 6 ELSE 7
                    END, h.hora_inicio
        """;
        var rows = em.createNativeQuery(q)
                .setParameter("j", (jornada == null || jornada.isBlank()) ? null : jornada)
                .getResultList();

        String qConflicto = """
          SELECT 1
            FROM public.asignacion_laboratorio al
            JOIN public.horario h2 ON h2.id_horario = al.id_horario
           WHERE al.id_laboratorio = :lab
             AND h2.dia_semana = :dia
             AND h2.jornada = :j
             AND NOT (h2.hora_fin <= :ini OR h2.hora_inicio >= :fin)
           LIMIT 1
        """;

        List<Map<String,Object>> out = new ArrayList<>();
        for (Object o : rows) {
            Object[] r = (Object[]) o;
            Integer idH = ((Number) r[0]).intValue();
            String  dia = String.valueOf(r[1]);
            Time    hi  = (Time) r[2];
            Time    hf  = (Time) r[3];
            String  j   = String.valueOf(r[4]);

            boolean disponible = true;
            if (idLaboratorio != null) {
                List<?> rs = em.createNativeQuery(qConflicto)
                        .setParameter("lab", idLaboratorio)
                        .setParameter("dia", dia)
                        .setParameter("j", j)
                        .setParameter("ini", hi)
                        .setParameter("fin", hf)
                        .getResultList();
                disponible = rs.isEmpty();
            }

            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", idH);
            m.put("diaSemana", dia);
            m.put("horaInicio", hi);
            m.put("horaFin", hf);
            m.put("jornada", j);
            m.put("disponible", disponible);
            out.add(m);
        }
        return out;
    }

    // OVERLOAD de compatibilidad
    public List<Map<String,Object>> horariosParaPropuestaGrupo(String grupoId, String jornada) {
        return horariosParaPropuestaGrupo(grupoId, jornada, null);
    }

    // ====== Resumen del grupo (para modal de Proponer) ======
    public Map<String,Object> resumenGrupo(String grupoId){
        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("grupoId", g.grupoId());
        out.put("docente", g.docente());
        out.put("materia", g.materia()); // aquí sigue siendo el MOTIVO del grupo (texto de solicitud)
        out.put("jornada", g.jornada());
        out.put("bloquesRequeridos", g.bloques().size());
        return out;
    }

    // ====== Proponer alternativa en bloque (selección de VARIOS horarios) ======
    @Transactional
    public void proponerGrupo(String grupoId, List<Integer> idHorarios, Integer idLaboratorio, String mensaje) {
        if (idLaboratorio == null || idHorarios == null || idHorarios.isEmpty())
            throw new IllegalArgumentException("idLaboratorio e idHorarios son requeridos.");

        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        if (idHorarios.size() != g.bloques().size()) {
            throw new IllegalArgumentException("Debes seleccionar " + g.bloques().size() + " bloques.");
        }

        Integer idAdminPiso = adminPisoPorLaboratorio(idLaboratorio);

        String updEstado = """
            UPDATE public.solicitudasignacion
               SET estado = 'Revisión',
                   id_admin_piso = COALESCE(id_admin_piso, :idAdmin)
             WHERE id_solicitud = :idSol
        """;

        String upsert = """
            INSERT INTO public.solicitud_propuesta
              (id_solicitud, id_horario_propuesto, id_laboratorio_propuesto, mensaje_admin, fecha)
            VALUES (:id, :hor, :lab, :msg, NOW())
            ON CONFLICT (id_solicitud) DO UPDATE
               SET id_horario_propuesto    = EXCLUDED.id_horario_propuesto,
                   id_laboratorio_propuesto = EXCLUDED.id_laboratorio_propuesto,
                   mensaje_admin           = EXCLUDED.mensaje_admin,
                   fecha                   = NOW()
        """;

        int i = 0;
        for (Long idSol : g.solicitudesIds()) {
            Integer h = idHorarios.get(i++);

            em.createNativeQuery(upsert)
                    .setParameter("id", idSol)
                    .setParameter("hor", h)
                    .setParameter("lab", idLaboratorio)
                    .setParameter("msg", mensaje)
                    .executeUpdate();

            em.createNativeQuery(updEstado)
                    .setParameter("idSol", idSol)
                    .setParameter("idAdmin", idAdminPiso)
                    .executeUpdate();
        }
    }

    /* ====== (Compat) Proponer con un solo horario ====== */
    @Transactional
    public void proponerGrupo(String grupoId, Integer idHorario, Integer idLaboratorio, String mensaje) {
        if (idHorario == null || idLaboratorio == null)
            throw new IllegalArgumentException("idHorario e idLaboratorio son requeridos.");
        proponerGrupo(grupoId, List.of(idHorario), idLaboratorio, mensaje);
    }

    // ====== Detalle de propuesta del grupo (para decidir aprobación directa) ======
    @SuppressWarnings("unchecked")
    public Map<String,Object> propuestaGrupo(String grupoId) {
        GrupoDTO g = listarAgrupadas(null).stream()
                .filter(x -> Objects.equals(x.grupoId(), grupoId))
                .findFirst()
                .orElse(null);
        if (g == null || g.solicitudesIds().isEmpty()) return null;

        String sqlProps = """
            SELECT sp.id_solicitud, sp.id_laboratorio_propuesto, sp.id_horario_propuesto
              FROM public.solicitud_propuesta sp
             WHERE sp.id_solicitud = ANY(:ids)
        """;
        List<Object[]> props = em.createNativeQuery(sqlProps)
                .setParameter("ids", g.solicitudesIds().toArray(Long[]::new))
                .getResultList();

        if (props.size() != g.solicitudesIds().size()) {
            return null; // no hay propuesta completa para todo el grupo
        }

        Set<Integer> labs = props.stream()
                .map(r -> ((Number) r[1]).intValue())
                .collect(Collectors.toSet());
        if (labs.size() != 1) {
            return null; // inconsistentes
        }
        Integer idLaboratorio = labs.iterator().next();

        String nomLab;
        try {
            Object r = em.createNativeQuery(
                            "SELECT nombre_laboratorio FROM public.laboratorio WHERE id_laboratorio = :id")
                    .setParameter("id", idLaboratorio)
                    .getSingleResult();
            nomLab = String.valueOf(r);
        } catch (Exception e) {
            nomLab = String.valueOf(idLaboratorio);
        }

        String qHor = """
            SELECT h.dia_semana, h.hora_inicio, h.hora_fin
              FROM public.horario h
             WHERE h.id_horario = :id
        """;
        List<Map<String,Object>> horarios = new ArrayList<>();
        for (Object[] pr : props) {
            Integer idHor = ((Number) pr[2]).intValue();
            Object[] h = (Object[]) em.createNativeQuery(qHor)
                    .setParameter("id", idHor)
                    .getSingleResult();
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("diaSemana", String.valueOf(h[0]));
            m.put("horaInicio", (Time) h[1]);
            m.put("horaFin", (Time) h[2]);
            m.put("idHorario", idHor);
            horarios.add(m);
        }

        List<String> ordenDias = List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo");
        horarios.sort(Comparator
                .comparing((Map<String,Object> m) -> {
                    int idx = ordenDias.indexOf(Objects.toString(m.get("diaSemana"), ""));
                    return idx < 0 ? 99 : idx;
                })
                .thenComparing(m -> (Time) m.get("horaInicio"))
        );

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("idLaboratorio", idLaboratorio);
        out.put("laboratorio", nomLab);
        out.put("horarios", horarios);
        return out;
    }
}