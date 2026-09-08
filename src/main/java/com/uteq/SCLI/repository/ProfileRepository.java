// com.uteq.SCLI.repository.ProfileRepository.java
package com.uteq.SCLI.repository;

import com.uteq.SCLI.dto.ProfileDTO;
import com.uteq.SCLI.service.CryptoService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
  private final JdbcTemplate jdbc;
  private final CryptoService crypto;
  public ProfileRepository(JdbcTemplate jdbc, CryptoService crypto){
    this.jdbc = jdbc;
    this.crypto = crypto;
  }

  public ProfileDTO findByIdPersona(int idPersona){
    String sql = """
      SELECT p.id_persona, p.nombres, p.apellidos, p.correo, p.telefono, p.foto_url,
             d.titulo_academico, d.departamento
        FROM persona p
   LEFT JOIN docente d ON d.id_persona = p.id_persona
       WHERE p.id_persona = ?
      """;
    return jdbc.query(sql, rs -> {
      if(!rs.next()) return null;
      ProfileDTO x = new ProfileDTO();
      x.setIdPersona(rs.getInt("id_persona"));
      x.setNombres(rs.getString("nombres"));
      x.setApellidos(rs.getString("apellidos"));
      x.setCorreo(rs.getString("correo"));
      x.setTelefono(crypto.desencriptar(rs.getString("telefono"))); // Desencriptar antes de devolver
      x.setFotoUrl(rs.getString("foto_url"));
      x.setTituloAcademico(rs.getString("titulo_academico"));
      x.setDepartamento(rs.getString("departamento"));
      return x;
    }, idPersona);
  }

  public void updatePersona(ProfileDTO d){
    String sql = """
      UPDATE persona
         SET nombres=?, apellidos=?, correo=?, telefono=?, foto_url=COALESCE(?, foto_url)
       WHERE id_persona=?
      """;
    jdbc.update(sql, d.getNombres(), d.getApellidos(), d.getCorreo(),
                crypto.encriptar(d.getTelefono()), d.getFotoUrl(), d.getIdPersona());
  }

  public void updateDocente(ProfileDTO d){
    String sql = """
      UPDATE docente
         SET titulo_academico=?, departamento=?
       WHERE id_persona=?
      """;
    jdbc.update(sql, d.getTituloAcademico(), d.getDepartamento(), d.getIdPersona());
  }

  public Result cambiarClave(int idUsuario, String actual, String nueva){
    String sql = "SELECT ok, msg FROM app.fn_usuario_cambiar_clave(?, ?, ?)";
    return jdbc.query(sql, rs -> { rs.next(); return new Result(rs.getBoolean("ok"), rs.getString("msg")); },
                      idUsuario, actual, nueva);
  }

  public record Result(boolean ok, String msg) {}
}
