package com.uteq.SCLI.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;

@Component
public class SesionService {

  private final JdbcTemplate jdbc;
  private final HttpSession session;

  public SesionService(JdbcTemplate jdbc, HttpSession session) {
    this.jdbc = jdbc;
    this.session = session;
  }

  /** Devuelve id_docente basado en el usuario autenticado. */
  public Integer idDocente() {
    Object cached = session.getAttribute("ID_DOCENTE");
    if (cached instanceof Integer i) return i;

    Object idUsuarioObj = session.getAttribute("ID_USUARIO"); // ajusta al atributo que ya uses
    if (idUsuarioObj == null) return null;

    Integer idUsuario = (Integer) idUsuarioObj;
    Integer idDocente = jdbc.query("""
        SELECT d.id_docente
        FROM app.app_usuario u
        JOIN public.docente d ON d.id_persona = u.id_persona
        WHERE u.id_usuario = ?
      """, rs -> rs.next() ? rs.getInt(1) : null, idUsuario);

    if (idDocente != null) session.setAttribute("ID_DOCENTE", idDocente);
    return idDocente;
  }
}
