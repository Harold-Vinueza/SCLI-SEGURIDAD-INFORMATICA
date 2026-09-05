package com.uteq.SCLI.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Repository
public class HorarioQueriesRepository {

  private final JdbcTemplate jdbc;

  public HorarioQueriesRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

 /** Lee un período por id. Devuelve null si no existe. */
    public Map<String, Object> getPeriodo(Integer id) {
    final String sql = """
        SELECT id_periodo AS id, nombre, activo
        FROM periodolectivo
        WHERE id_periodo = ?
      """;
    var rows = jdbc.queryForList(sql, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

    /** Abre / cierra un período (flag booleano 'activo'). */
  public int setPeriodoEstado(Integer idPeriodo, boolean activo) {
    return jdbc.update(
        "UPDATE periodolectivo SET activo = ? WHERE id_periodo = ?",
        activo, idPeriodo
    );
  }


  // === Buscar asignación exacta (celda) para poder editar ===
  // Precarga de una celda: devuelve id_asignacion, id_materia, id_docente
public Map<String,Object> asignacionPorCelda(Integer idLab, Integer idPeriodo, Integer idHorario) {
  String sql = """
      SELECT a.id_asignacion,
             a.id_materia,
             a.id_docente
      FROM asignacion_laboratorio a
      WHERE a.id_laboratorio = ?
        AND a.id_periodo     = ?
        AND a.id_horario     = ?
      """;
  var list = jdbc.queryForList(sql, idLab, idPeriodo, idHorario);
  return list.isEmpty() ? null : list.get(0);
}

// Estado del período
public Map<String,Object> periodoById(Integer idPeriodo) {
  String sql = """
      SELECT id_periodo AS id,
             nombre,
             activo
      FROM periodolectivo
      WHERE id_periodo = ?
      """;
  var list = jdbc.queryForList(sql, idPeriodo);
  return list.isEmpty() ? Map.of("id", idPeriodo, "nombre", "", "activo", false) : list.get(0);
}



  // Reemplaza el método materias(...) por este:
public List<Map<String,Object>> materias(String q) {
  if (q == null || q.isBlank()) {
    String sql = """
        SELECT id_materia AS id,
               cod_materia,
               nombre_materia
        FROM Materia
        ORDER BY nombre_materia
        """;
    return jdbc.queryForList(sql);
  } else {
    String like = "%" + q.trim() + "%";
    String sql = """
        SELECT id_materia AS id,
               cod_materia,
               nombre_materia
        FROM Materia
        WHERE nombre_materia ILIKE ? OR cod_materia ILIKE ?
        ORDER BY nombre_materia
        """;
    return jdbc.queryForList(sql, like, like);
  }
}


// Docentes que dictan una materia
public List<Map<String,Object>> docentesPorMateria(Integer idMateria) {
  String sql = """
      SELECT d.id_docente AS id,
             (p.nombres || ' ' || p.apellidos) AS label
      FROM DocenteMateria dm
      JOIN Docente d   ON d.id_docente = dm.id_docente
      JOIN Persona p   ON p.id_persona = d.id_persona
      WHERE dm.id_materia = ?
      ORDER BY label
      """;
  return jdbc.queryForList(sql, idMateria);
}



   public List<Map<String,Object>> periodosActivos() {
    String sql = """
        SELECT id_periodo AS id, nombre AS label
        FROM periodolectivo
        WHERE activo = TRUE
        ORDER BY fecha_inicio DESC NULLS LAST, id_periodo DESC
      """;
    return jdbc.queryForList(sql);
  }
  
  public List<Map<String, Object>> laboratorios() {
    String sql = """
        SELECT id_laboratorio AS id,
               COALESCE(cod_laboratorio||' - ', '') || nombre_laboratorio AS label
        FROM Laboratorio
        ORDER BY nombre_laboratorio
        """;
    return jdbc.queryForList(sql);
  }

  public List<Map<String, Object>> bloquesPorJornada(String jornada) {
    String sql = """
        SELECT id_horario, dia_semana,
               to_char(hora_inicio,'HH24:MI') AS hora_inicio,
               to_char(hora_fin,'HH24:MI')    AS hora_fin
        FROM Horario
        WHERE jornada = ?
        ORDER BY
          CASE dia_semana
            WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miércoles' THEN 3
            WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 WHEN 'Sábado' THEN 6 WHEN 'Domingo' THEN 7
          END, hora_inicio
        """;
    return jdbc.queryForList(sql, jornada);
  }

  public List<Map<String, Object>> grillaSemana(Integer lab, Integer periodo, String jornada) {
  String sql = """
    WITH bloques AS (
      SELECT h.id_horario, h.dia_semana, h.hora_inicio, h.hora_fin
      FROM Horario h
      WHERE h.jornada = ?
    ),
    asigs AS (
      SELECT a.id_asignacion,
             a.id_horario,
             COALESCE(m.cod_materia || ' - ' || m.nombre_materia, a.materia) AS nombre_materia,
             (p.nombres || ' ' || p.apellidos) AS docente,
             TO_CHAR(a.fecha_asignacion,'YYYY-MM-DD') AS f_asig
      FROM asignacion_laboratorio a
      LEFT JOIN Materia  m ON m.id_materia  = a.id_materia
      LEFT JOIN Docente  d ON d.id_docente  = a.id_docente
      LEFT JOIN Persona  p ON p.id_persona  = d.id_persona
      WHERE a.id_laboratorio = ?
        AND a.id_periodo     = ?
    )
    SELECT
      to_char(b.hora_inicio,'HH24:MI')||'-'||to_char(b.hora_fin,'HH24:MI') AS bloque,
      MAX(CASE WHEN b.dia_semana='Lunes'
               THEN COALESCE(a.nombre_materia,'') ||
                    CASE WHEN a.docente IS NOT NULL THEN ' ('||a.docente||')' ELSE '' END ||
                    CASE WHEN a.f_asig  IS NOT NULL THEN ' — '||a.f_asig ELSE '' END END) AS lunes,
      MAX(CASE WHEN b.dia_semana='Martes'
               THEN COALESCE(a.nombre_materia,'') ||
                    CASE WHEN a.docente IS NOT NULL THEN ' ('||a.docente||')' ELSE '' END ||
                    CASE WHEN a.f_asig  IS NOT NULL THEN ' — '||a.f_asig ELSE '' END END) AS martes,
      MAX(CASE WHEN b.dia_semana='Miércoles'
               THEN COALESCE(a.nombre_materia,'') ||
                    CASE WHEN a.docente IS NOT NULL THEN ' ('||a.docente||')' ELSE '' END ||
                    CASE WHEN a.f_asig  IS NOT NULL THEN ' — '||a.f_asig ELSE '' END END) AS miercoles,
      MAX(CASE WHEN b.dia_semana='Jueves'
               THEN COALESCE(a.nombre_materia,'') ||
                    CASE WHEN a.docente IS NOT NULL THEN ' ('||a.docente||')' ELSE '' END ||
                    CASE WHEN a.f_asig  IS NOT NULL THEN ' — '||a.f_asig ELSE '' END END) AS jueves,
      MAX(CASE WHEN b.dia_semana='Viernes'
               THEN COALESCE(a.nombre_materia,'') ||
                    CASE WHEN a.docente IS NOT NULL THEN ' ('||a.docente||')' ELSE '' END ||
                    CASE WHEN a.f_asig  IS NOT NULL THEN ' — '||a.f_asig ELSE '' END END) AS viernes
    FROM bloques b
    LEFT JOIN asigs a ON a.id_horario = b.id_horario
    GROUP BY b.hora_inicio, b.hora_fin
    ORDER BY b.hora_inicio
    """;
  return jdbc.queryForList(sql, jornada, lab, periodo);
}






  public Map<String, Object> contador(Integer lab, Integer periodo) {
    String sql = "SELECT * FROM horas_restantes_laboratorio(?,?)";
    return jdbc.queryForMap(sql, lab, periodo);
  }

// Lista para combos: [{id, label}] con búsqueda opcional
public List<Map<String,Object>> docentesSelect(String q) {
  String base = """
      SELECT d.id_docente AS id,
             TRIM(p.nombres || ' ' || p.apellidos) ||
             CASE WHEN COALESCE(d.titulo_academico,'') <> ''
                  THEN ' — ' || d.titulo_academico
                  ELSE '' END AS label
      FROM Docente d
      JOIN Persona p ON p.id_persona = d.id_persona
      """;

  if (q == null || q.isBlank()) {
    return jdbc.queryForList(base + " ORDER BY p.nombres, p.apellidos");
  }

  String like = "%" + q.trim() + "%";
  String sql = base + """
      WHERE p.nombres ILIKE ?
         OR p.apellidos ILIKE ?
         OR (p.nombres || ' ' || p.apellidos) ILIKE ?
         OR d.titulo_academico ILIKE ?
      ORDER BY p.nombres, p.apellidos
      """;
  return jdbc.queryForList(sql, like, like, like, like);
}

/** Materias asignadas a un docente (para el combo del modal). */
  public List<Map<String,Object>> materiasDeDocente(Integer idDocente) {
    String sql = """
        SELECT m.id_materia AS id,
               m.nombre_materia AS label
        FROM Materia m
        JOIN DocenteMateria dm ON dm.id_materia = m.id_materia
        WHERE dm.id_docente = ?
        ORDER BY m.nombre_materia
        """;
    return jdbc.queryForList(sql, idDocente);
  }

}
