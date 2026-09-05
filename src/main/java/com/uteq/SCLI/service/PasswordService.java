// src/main/java/com/uteq/SCLI/service/PasswordService.java
package com.uteq.SCLI.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

  @PersistenceContext
  private EntityManager em;

  @Transactional(readOnly = true)
  public boolean mustChange(Integer idUsuario) {
    Object r = em.createNativeQuery("SELECT app.fn_must_change(?)")
        .setParameter(1, idUsuario)
        .getSingleResult();
    if (r == null) return false;
    if (r instanceof Boolean b) return b;
    return Boolean.parseBoolean(r.toString());
  }

  @Transactional
  public void cambiar(Integer idUsuario, String actual, String nueva) {
    Object raw = em.createNativeQuery("SELECT * FROM app.fn_usuario_cambiar_clave(?,?,?)")
        .setParameter(1, idUsuario)
        .setParameter(2, actual)
        .setParameter(3, nueva)
        .getSingleResult();
    Object[] row = (Object[]) raw; // (ok boolean, msg text)
    boolean ok = (row[0] instanceof Boolean) ? (Boolean) row[0] : Boolean.parseBoolean(String.valueOf(row[0]));
    String msg = row[1] != null ? row[1].toString() : "No se pudo actualizar la contraseña.";
    if (!ok) throw new IllegalArgumentException(msg);
  }
}















/* 
package com.uteq.SCLI.service;

import com.uteq.SCLI.repository.PasswordRepository;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
  private final PasswordRepository repo;
  public PasswordService(PasswordRepository repo){ this.repo = repo; }

  public boolean mustChange(Integer idUsuario){
    Boolean v = repo.mustChange(idUsuario);
    return v != null && v;
  }

  public String cambiar(Integer idUsuario, String actual, String nueva){
    Object[] r = repo.cambiarClave(idUsuario, actual, nueva);
    boolean ok = r != null && Boolean.valueOf(r[0].toString());
    String msg = (r != null && r[1]!=null) ? r[1].toString() : "No se pudo actualizar";
    if(!ok) throw new IllegalArgumentException(msg);
    return msg;
  }
}*/
