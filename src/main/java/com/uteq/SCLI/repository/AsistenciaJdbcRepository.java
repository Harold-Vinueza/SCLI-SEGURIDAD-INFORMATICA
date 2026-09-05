package com.uteq.SCLI.repository;

import com.uteq.SCLI.dto.MateriaCardDTO;
import com.uteq.SCLI.dto.PaseListaVM;
import com.uteq.SCLI.dto.StudentAttendanceRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Repository
public class AsistenciaJdbcRepository {

  private static final Logger log = LoggerFactory.getLogger(AsistenciaJdbcRepository.class);
  private final JdbcTemplate jdbc;

  public AsistenciaJdbcRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  /* ================== helpers de contexto/RLS ================== */

  private void setDocenteContext(Integer idDocente) {
    if (idDocente == null) return;
    try {
      jdbc.queryForObject("SELECT set_config('app.current_docente_id', ?, true)", String.class, String.valueOf(idDocente));
     jdbc.queryForObject("SELECT set_config('app.current_estudiante_id', NULL, true)", String.class);

      jdbc.queryForObject("SELECT set_config('search_path','public,app', false)", String.class);
    } catch (DataAccessException e) {
      log.warn("No se pudo setear app.current_docente_id en la sesión: {}", e.getMessage());
    }
  }

  private void setDocenteContextByRegistro(Integer idRegistro) {
    try {
      Integer idDocente = jdbc.queryForObject(
          "SELECT id_docente FROM public.registroasistencia WHERE id_registro = ?",
          Integer.class, idRegistro
      );
      setDocenteContext(idDocente);
    } catch (DataAccessException e) {
      log.warn("No se pudo resolver id_docente desde id_registro={} -> {}", idRegistro, e.getMessage());
    }
  }

  /* ================== introspección de columnas ================== */
  private static final String SCHEMA = "public";
  private static final String TABLA  = "registroasistencia";

  private static final class ColInfo {
    final String name; final boolean nullable; final boolean hasDefault;
    ColInfo(String name, boolean nullable, boolean hasDefault) {
      this.name = name; this.nullable = nullable; this.hasDefault = hasDefault;
    }
  }

  private Map<String, ColInfo> columnasRegistro() {
    String sql = """
        SELECT column_name, is_nullable, column_default
        FROM information_schema.columns
        WHERE table_schema=? AND table_name=?""";
    List<ColInfo> lst = jdbc.query(sql, (rs, i) -> new ColInfo(
        rs.getString("column_name"),
        "YES".equalsIgnoreCase(rs.getString("is_nullable")),
        rs.getString("column_default") != null
    ), SCHEMA, TABLA);
    Map<String, ColInfo> m = new HashMap<>();
    for (ColInfo c : lst) m.put(c.name, c);
    return m;
  }

  /* ================== resoluciones de docente ================== */

  public Integer idDocentePorUsername(String username) {
    String sql = """
      SELECT d.id_docente
      FROM app.app_usuario u
      JOIN public.docente d ON d.id_persona = u.id_persona
      WHERE lower(u.username) = lower(?)
      LIMIT 1
    """;
    var list = jdbc.query(sql, (rs, i) -> rs.getInt(1), username);
    return list.isEmpty() ? null : list.get(0);
  }

  public Integer idDocentePorIdPersona(Integer idPersona) {
    String sql = """
      SELECT d.id_docente
      FROM public.docente d
      WHERE d.id_persona = ?
      LIMIT 1
    """;
    var list = jdbc.query(sql, (rs, i) -> rs.getInt(1), idPersona);
    return list.isEmpty() ? null : list.get(0);
  }

  /* ================== consultas de materias ================== */

  public List<MateriaCardDTO> materiasDeDocente(Integer idDocente) {
    String sql = """
      SELECT m.id_materia, m.nombre_materia AS nombre, NULL::text AS codigo
      FROM public.materia m
      JOIN public.docentemateria dm ON dm.id_materia = m.id_materia
      WHERE dm.id_docente = ?
      ORDER BY m.nombre_materia
    """;
    return jdbc.query(sql, (rs, i) ->
        new MateriaCardDTO(
            rs.getInt("id_materia"),
            rs.getString("nombre"),
            rs.getString("codigo")
        ), idDocente);
  }

  /* ================== get or create registro ================== */

  public Integer getOrCreateRegistro(Integer idDocente, Integer idMateria,
                                     LocalDate fechaClase,
                                     Integer idLaboratorio, String temaClase) {
    setDocenteContext(idDocente); // clave para RLS

    // 1) función v2 (ON CONFLICT)
    try {
      return jdbc.queryForObject(
          "SELECT app.fn_get_or_create_registro_v2(?,?,?,?,?)",
          Integer.class, idDocente, idMateria, fechaClase, idLaboratorio, temaClase
      );
    } catch (DataAccessException eV2) {
      // 1.2) función v1
      try {
        return jdbc.queryForObject(
            "SELECT app.fn_get_or_create_registro(?,?,?,?,?)",
            Integer.class, idDocente, idMateria, fechaClase, idLaboratorio, temaClase
        );
      } catch (DataAccessException eV1) {
        // 2) fallback a SQL directo
        Map<String, ColInfo> colsMeta = columnasRegistro();
        if (!colsMeta.containsKey("id_docente") || !colsMeta.containsKey("id_materia"))
          throw new IllegalStateException("La tabla registroasistencia no tiene id_docente/id_materia.");

        String colFecha = colsMeta.containsKey("fecha_clase") ? "fecha_clase"
                        : colsMeta.containsKey("fecha")       ? "fecha" : null;
        if (colFecha == null)
          throw new IllegalStateException("La tabla registroasistencia no tiene fecha_clase ni fecha.");

        String colTema  = colsMeta.containsKey("tema_clase") ? "tema_clase"
                        : colsMeta.containsKey("tema")       ? "tema" : null;

        try {
          Integer ya = jdbc.queryForObject(
              "SELECT id_registro FROM public.registroasistencia " +
              "WHERE id_docente=? AND id_materia=? AND " + colFecha + "=? LIMIT 1",
              Integer.class, idDocente, idMateria, fechaClase
          );
          if (ya != null) return ya;
        } catch (DataAccessException ignored) { }

        List<String> cols = new ArrayList<>(List.of("id_docente","id_materia", colFecha));
        List<Object> vals = new ArrayList<>(List.of(idDocente,   idMateria,    fechaClase));

        if (colTema != null) {
          boolean nullableTema = colsMeta.get(colTema).nullable;
          vals.add(nullableTema ? temaClase : (temaClase != null ? temaClase : ""));
          cols.add(colTema);
        }
        if (colsMeta.containsKey("id_laboratorio")) {
          ColInfo ci = colsMeta.get("id_laboratorio");
          Integer val = idLaboratorio;
          if (val == null && !ci.nullable && !ci.hasDefault) val = 0;
          cols.add("id_laboratorio");
          vals.add(val);
        }

        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sqlIns = "INSERT INTO public.registroasistencia (" +
            String.join(",", cols) + ") VALUES (" + placeholders + ") RETURNING id_registro";

        try {
          return jdbc.queryForObject(sqlIns, Integer.class, vals.toArray());
        } catch (DataAccessException ex) {
          String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
          log.error("INSERT registroasistencia falló: {} -> {}", sqlIns, msg);
          throw new IllegalStateException("No se pudo insertar registroasistencia. Motivo: " + msg, ex);
        }
      }
    }
  }

  /* ================== cabecera y filas ================== */

  public PaseListaVM cargarCabecera(Integer idRegistro) {
    // asegurar el GUC por si este método se invoca fuera del flujo de creación
    setDocenteContextByRegistro(idRegistro);

    Map<String, ColInfo> colsMeta = columnasRegistro();
    String colFecha =
        colsMeta.containsKey("fecha_clase") ? "ra.fecha_clase" :
        colsMeta.containsKey("fecha")       ? "ra.fecha" : null;
    if (colFecha == null)
      throw new IllegalStateException("La tabla registroasistencia no tiene fecha_clase ni fecha.");

    String colTema =
        colsMeta.containsKey("tema_clase") ? "ra.tema_clase" :
        colsMeta.containsKey("tema")       ? "ra.tema" : "NULL";

    String sql =
        "SELECT ra.id_registro, ra.id_materia, " + colFecha + " AS fecha_clase, " + colTema + " AS tema_clase, " +
        "       COALESCE(m.nombre_materia, '(sin materia)') AS nombre_materia, " +
        "       COALESCE(l.nombre_laboratorio, '-')         AS nombre_laboratorio, " +
        "       (pd.apellidos || ' ' || pd.nombres)         AS docente " +
        "FROM public.registroasistencia ra " +
        "LEFT JOIN public.materia     m ON m.id_materia     = ra.id_materia " +
        "LEFT JOIN public.laboratorio l ON l.id_laboratorio = ra.id_laboratorio " +
        "JOIN public.docente          d ON d.id_docente     = ra.id_docente " +
        "JOIN public.persona          pd ON pd.id_persona   = d.id_persona " +
        "WHERE ra.id_registro = ?";

    return jdbc.queryForObject(sql, (rs, i) ->
        new PaseListaVM(
            rs.getInt("id_registro"),
            (Integer) rs.getObject("id_materia"),
            rs.getString("nombre_materia"),
            rs.getDate("fecha_clase").toLocalDate(),
            rs.getString("tema_clase"),
            rs.getString("docente"),
            rs.getString("nombre_laboratorio"),
            List.of()
        ), idRegistro);
  }

  public List<StudentAttendanceRow> cargarFilas(Integer idRegistro,
                                                Integer idDocente,
                                                Integer idMateria) {
    String sql = """
      SELECT e.id_estudiante, pe.apellidos, pe.nombres,
             COALESCE(da.asistencia, false) AS presente
      FROM public.estudiante e
      JOIN public.persona pe        ON pe.id_persona = e.id_persona
      JOIN public.materia m         ON m.id_carrera  = e.id_carrera
      JOIN public.docentemateria dm ON dm.id_materia = m.id_materia
      LEFT JOIN public.detalleasistencia da
           ON da.id_registro = ? AND da.id_estudiante = e.id_estudiante
      WHERE dm.id_docente = ? AND m.id_materia = ?
      ORDER BY pe.apellidos, pe.nombres
    """;
    return jdbc.query(sql, (rs, i) ->
        new StudentAttendanceRow(
            rs.getInt("id_estudiante"),
            rs.getString("apellidos"),
            rs.getString("nombres"),
            rs.getBoolean("presente")
        ), idRegistro, idDocente, idMateria);
  }

  /* ================== guardar marcas ================== */

// ...
@Transactional
public void guardarMarcas(Integer idDocente, Integer idRegistro, Map<Integer, Boolean> marcas) {
  if (idRegistro == null) throw new IllegalArgumentException("idRegistro nulo");

  // GUC/RLS para esta sesión
  if (idDocente != null) setDocenteContext(idDocente);
  else setDocenteContextByRegistro(idRegistro);

  // ==== 0) Asegurar que EXISTAN todas las filas de detalle para este registro ====
  // (inserta ausentes como FALSE una sola vez)
  jdbc.update(con -> {
    var ps = con.prepareStatement("""
      INSERT INTO public.detalleasistencia (id_registro, id_estudiante, asistencia)
      SELECT ra.id_registro, e.id_estudiante, FALSE
        FROM public.registroasistencia ra
        JOIN public.materia m         ON m.id_materia = ra.id_materia
        JOIN public.estudiante e      ON e.id_carrera = m.id_carrera
       WHERE ra.id_registro = ?
         AND NOT EXISTS (
               SELECT 1
                 FROM public.detalleasistencia d
                WHERE d.id_registro = ra.id_registro
                  AND d.id_estudiante = e.id_estudiante
             )
    """);
    ps.setInt(1, idRegistro);
    return ps;
  });

  // ==== 1) Lista de presentes (IDs chequeados) ====
  final List<Integer> idsPresentes = (marcas == null) ? List.of()
      : marcas.entrySet().stream()
              .filter(Map.Entry::getValue)   // solo los true
              .map(Map.Entry::getKey)
              .sorted()
              .toList();

  // ==== 2) Un ÚNICO UPDATE: TRUE si está en el array; FALSE si no ====
  jdbc.update(con -> {
    var ps = con.prepareStatement("""
      UPDATE public.detalleasistencia d
         SET asistencia = (d.id_estudiante = ANY (?))
       WHERE d.id_registro = ?
    """);
    // Si la lista está vacía, enviamos {} para que todos queden FALSE
    java.sql.Array arr = con.createArrayOf("int4", idsPresentes.toArray());
    ps.setArray(1, arr);
    ps.setInt(2, idRegistro);
    return ps;
  });
}

  /* ================== PDF ================== */

  public List<PdfRow> pdfRows(Integer idRegistro) {
    String sql = """
      SELECT id_registro, fecha_clase, tema_clase,
             nombre_materia, nombre_laboratorio,
             docente, estudiante, presente
      FROM app.v_asistencia_pdf
      WHERE id_registro = ?
      ORDER BY estudiante
    """;
    return jdbc.query(sql, (rs, i) -> new PdfRow(
        rs.getInt("id_registro"),
        rs.getDate("fecha_clase").toLocalDate(),
        rs.getString("tema_clase"),
        rs.getString("nombre_materia"),
        rs.getString("nombre_laboratorio"),
        rs.getString("docente"),
        rs.getString("estudiante"),
        rs.getBoolean("presente")
    ), idRegistro);
  }

  public record PdfRow(Integer idRegistro, java.time.LocalDate fecha,
                       String tema, String materia, String laboratorio,
                       String docente, String estudiante, Boolean presente) {}
}
