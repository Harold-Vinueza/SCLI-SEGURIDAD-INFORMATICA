package com.uteq.SCLI.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DocenteSelectRepository {

  private final JdbcTemplate jdbc;

  public DocenteSelectRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String,Object>> select(String q) {
    String base = """
      SELECT d.id_docente AS id,
             TRIM(p.nombres || ' ' || p.apellidos) ||
             CASE WHEN COALESCE(d.titulo_academico,'') <> ''
                  THEN ' — ' || d.titulo_academico
                  ELSE '' END                           AS label
      FROM Docente d
      JOIN Persona p ON p.id_persona = d.id_persona
    """;

    if (q == null || q.isBlank()) {
      return jdbc.queryForList(base + " ORDER BY p.nombres, p.apellidos");
    }

    String like = "%" + q.trim() + "%";
    String sql = base + """
      WHERE p.nombres ILIKE ? OR p.apellidos ILIKE ? OR d.titulo_academico ILIKE ?
      ORDER BY p.nombres, p.apellidos
    """;
    return jdbc.queryForList(sql, like, like, like);
  }
}
