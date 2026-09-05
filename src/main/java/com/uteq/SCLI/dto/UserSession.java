package com.uteq.SCLI.dto;

import java.io.Serializable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class UserSession implements Serializable {
    private Integer idUsuario;
    private Integer idPersona;
    private String  username;
    private String  nombreRol; // “admin_master”, “admin_piso”, “docente”, “estudiante”
    private String  dbRole;    // ej. “app_docente”, “app_estudiante”

    private String sessionId;
    public String getSessionId(){ return sessionId; }
    public void setSessionId(String s){ this.sessionId = s; }

    // getters/setters
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public Integer getIdPersona() { return idPersona; }
    public void setIdPersona(Integer idPersona) { this.idPersona = idPersona; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String nombreRol) { this.nombreRol = nombreRol; }

    public String getDbRole() { return dbRole; }
    public void setDbRole(String dbRole) { this.dbRole = dbRole; }
}

/*package com.uteq.SCLI.dto;

import java.io.Serializable;

public class UserSession implements Serializable {
    private Integer idUsuario;
    private Integer idPersona;
    private String  username;
    private String  nombreRol; // “admin_master”, “admin_piso”, “docente”, “Estudiante” (según BD)
    private String  dbRole;    // ej. “app_docente”, “app_estudiante”

    // getters/setters
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdPersona() { return idPersona; }
    public void setIdPersona(Integer idPersona) { this.idPersona = idPersona; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String nombreRol) { this.nombreRol = nombreRol; }
    public String getDbRole() { return dbRole; }
    public void setDbRole(String dbRole) { this.dbRole = dbRole; }
}*/
