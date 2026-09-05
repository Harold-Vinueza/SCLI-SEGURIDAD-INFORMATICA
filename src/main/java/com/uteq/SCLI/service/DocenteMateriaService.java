package com.uteq.SCLI.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocenteMateriaService {

    private final JdbcTemplate jdbc;

  @Transactional(readOnly = true)
  public Integer findDocenteByMateria(Integer idMateria) {
    return jdbc.query("""
        SELECT id_docente
        FROM DocenteMateria
        WHERE id_materia = ?
        ORDER BY id_docente
        LIMIT 1
        """, ps -> ps.setInt(1, idMateria),
        rs -> rs.next() ? rs.getInt(1) : null
    );
  }

  @Transactional
  public void asignarDocente(Integer idMateria, Integer idDocente) {
    jdbc.update("DELETE FROM DocenteMateria WHERE id_materia = ?", idMateria);
    jdbc.update("INSERT INTO DocenteMateria (id_docente, id_materia) VALUES (?, ?)",
        idDocente, idMateria);
  }
}
