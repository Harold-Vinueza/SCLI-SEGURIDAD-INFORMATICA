// src/main/java/com/uteq/SCLI/repository/PasswordRepository.java
package com.uteq.SCLI.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordRepository {

  @PersistenceContext
  private EntityManager em;

  @Transactional
  public Object[] cambiarClave(Integer idUsuario, String actual, String nueva){
    Object r = em.createNativeQuery("SELECT * FROM app.fn_usuario_cambiar_clave(?,?,?)")
        .setParameter(1, idUsuario)
        .setParameter(2, actual)
        .setParameter(3, nueva)
        .getSingleResult();
    return (Object[]) r; // [ok, msg]
  }

  @Transactional
  public Boolean mustChange(Integer idUsuario){
    Object r = em.createNativeQuery("""
      SELECT must_change_password FROM app.app_usuario WHERE id_usuario = ?
    """).setParameter(1, idUsuario).getSingleResult();
    return (r != null) ? Boolean.valueOf(r.toString()) : null;
  }
}
