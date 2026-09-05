// src/main/java/com/uteq/SCLI/dto/LoginAuditDTO.java
package com.uteq.SCLI.dto;

import com.uteq.SCLI.repository.AuditRepository.LoginEventRow;
import lombok.Getter; import lombok.Setter;

import java.time.Instant;

@Getter @Setter
public class LoginAuditDTO {
    private Instant fechaHora;
    private String  username;
    private boolean ok;
    private String  nombreRol;
    private String  dbRole;
    private String  ip;
    private String  userAgent;
    private Integer idUsuario;
    private Integer idPersona;
    private String  nombreCompleto;

    public static LoginAuditDTO from(LoginEventRow r){
        LoginAuditDTO d = new LoginAuditDTO();
        d.setFechaHora(r.getFechaHora());
        d.setUsername(r.getUsername());
        d.setOk(Boolean.TRUE.equals(r.getOk()));
        d.setNombreRol(r.getNombreRol());
        d.setDbRole(r.getDbRole());
        d.setIp(r.getIp());
        d.setUserAgent(r.getUserAgent());
        d.setIdUsuario(r.getIdUsuario());
        d.setIdPersona(r.getIdPersona());
        d.setNombreCompleto(r.getNombreCompleto());
        return d;
    }
}
