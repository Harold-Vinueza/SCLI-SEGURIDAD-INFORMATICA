package com.uteq.SCLI.repository;

import com.uteq.SCLI.dto.StudentProfileDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudentProfileRepository {

    private final JdbcTemplate jdbc;
    public StudentProfileRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Obtiene perfil de estudiante por id_persona (join persona + estudiante + carrera). */
    public StudentProfileDTO findByIdPersona(int idPersona){
        String sql = """
      SELECT
          p.id_persona,
          p.nombres,
          p.apellidos,
          p.correo,
          p.telefono,
          p.foto_url,
          e.id_estudiante,
          e.matricula,
          e.nivel,
          e.id_carrera,
          c.nombre_carrera
      FROM persona p
      LEFT JOIN estudiante e ON e.id_persona = p.id_persona
      LEFT JOIN carrera    c ON c.id_carrera = e.id_carrera
      WHERE p.id_persona = ?
      """;
        return jdbc.query(sql, rs -> {
            if (!rs.next()) return null;
            StudentProfileDTO d = new StudentProfileDTO();
            d.setIdPersona(rs.getInt("id_persona"));
            d.setNombres(rs.getString("nombres"));
            d.setApellidos(rs.getString("apellidos"));
            d.setCorreo(rs.getString("correo"));
            d.setTelefono(rs.getString("telefono"));
            d.setFotoUrl(rs.getString("foto_url"));
            Object idEst = rs.getObject("id_estudiante");
            d.setIdEstudiante(idEst != null ? ((Number)idEst).intValue() : null);
            d.setMatricula(rs.getString("matricula"));
            d.setNivel(rs.getString("nivel"));
            Object idCar = rs.getObject("id_carrera");
            d.setIdCarrera(idCar != null ? ((Number)idCar).intValue() : null);
            d.setCarreraNombre(rs.getString("nombre_carrera"));
            return d;
        }, idPersona);
    }

    /** Actualiza datos básicos en persona. Mantiene foto_url si es null. */
    public void updatePersona(StudentProfileDTO d){
        String sql = """
      UPDATE persona
         SET nombres = ?,
             apellidos = ?,
             correo = ?,
             telefono = ?,
             foto_url = COALESCE(?, foto_url)
       WHERE id_persona = ?
      """;
        jdbc.update(sql,
                d.getNombres(), d.getApellidos(), d.getCorreo(), d.getTelefono(), d.getFotoUrl(), d.getIdPersona());
    }

    /** Inserta/actualiza en estudiante usando UPSERT por (id_persona) UNIQUE. */
    public void upsertEstudiante(StudentProfileDTO d){
        String sql = """
      INSERT INTO estudiante (id_persona, matricula, nivel, id_carrera)
      VALUES (?, ?, ?, ?)
      ON CONFLICT (id_persona) DO UPDATE
        SET matricula = EXCLUDED.matricula,
            nivel     = EXCLUDED.nivel,
            id_carrera= EXCLUDED.id_carrera
      """;
        jdbc.update(sql, d.getIdPersona(), d.getMatricula(), d.getNivel(), d.getIdCarrera());
    }

    /** Busca id_carrera por nombre (case-insensitive). Devuelve null si no existe. */
    public Integer findCarreraIdByNombre(String nombre){
        if (nombre == null || nombre.isBlank()) return null;
        String sql = "SELECT id_carrera FROM carrera WHERE lower(nombre_carrera) = lower(?)";
        return jdbc.query(sql, rs -> rs.next()? (Integer) rs.getObject(1) : null, nombre.trim());
    }

    /** Cambio de clave (si usas función app.fn_usuario_cambiar_clave). Ajusta si tu esquema difiere. */
    public Result cambiarClave(int idUsuario, String actual, String nueva){
        String sql = "SELECT ok, msg FROM app.fn_usuario_cambiar_clave(?, ?, ?)";
        return jdbc.query(sql, rs -> { rs.next(); return new Result(rs.getBoolean("ok"), rs.getString("msg")); },
                idUsuario, actual, nueva);
    }

    public record Result(boolean ok, String msg) {}
}