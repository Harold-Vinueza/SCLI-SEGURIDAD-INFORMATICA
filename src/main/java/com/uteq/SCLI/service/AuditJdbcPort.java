package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.UserSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditJdbcPort {
    private final JdbcTemplate jdbc;
    public AuditJdbcPort(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    public record LoginResult(boolean ok, Integer idUsuario, Integer idPersona,
                              String nombreRol, String dbRole, String sessionId) {}

    public LoginResult loginAudit(String user, String pass, String ip, String ua){
        return jdbc.queryForObject(
                "select ok, id_usuario, id_persona, nombre_rol, db_role, session_id " +
                        "from app.fn_login_audit(?, ?, CAST(? as inet), ?)",
                (rs,i)-> new LoginResult(
                        rs.getBoolean("ok"),
                        (Integer) rs.getObject("id_usuario"),
                        (Integer) rs.getObject("id_persona"),
                        rs.getString("nombre_rol"),
                        rs.getString("db_role"),
                        rs.getString("session_id")
                ),
                user, pass, ip, ua
        );
    }

    /** Compatibilidad: heartbeat básico (solo ip/ua) */
    public void heartbeat(UserSession us, String ip, String ua){
        if (us==null || us.getSessionId()==null) return;
        heartbeatDetailed(us, ip, null, ua, null,null,null,null,null,null,null, null);
    }

    /** ✅ Heartbeat completo con todos los campos de la función audit.heartbeat(17) */
    public void heartbeatDetailed(UserSession us,
                                  String ipPublic, String ipLocal, String uaFull,
                                  String browser, String browserVer,
                                  String platform, String platformVer,
                                  String arch, String deviceModel, String deviceType,
                                  String uaHintsJson) {
        if (us==null || us.getSessionId()==null) return;

        // Si viene vacío, manda NULL al jsonb
        Object hintsParam = (uaHintsJson==null || uaHintsJson.isBlank()) ? null : uaHintsJson;

        jdbc.queryForList(
                "select audit.heartbeat(" +
                        "CAST(? as uuid), ?, ?, ?, ?, ?, CAST(? as inet), ?," +      // 1..8
                        "CAST(? as inet), ?, ?, ?, ?, ?, ?, ?, CAST(? as jsonb)" +   // 9..17
                        ")",
                us.getSessionId(), us.getIdUsuario(), us.getIdPersona(), us.getUsername(),
                us.getNombreRol(), us.getDbRole(), ipPublic, uaFull,
                ipLocal, browser, browserVer, platform, platformVer, arch, deviceModel, deviceType, hintsParam
        );

        // Contexto para triggers/vistas (compatibilidad)
        jdbc.queryForList(
                "select app.set_audit_context(?, ?, ?, ?, ?, CAST(? as inet), ?)",
                us.getIdUsuario(), us.getUsername(), us.getNombreRol(), us.getDbRole(),
                us.getIdPersona(), ipPublic, uaFull
        );
    }

    public void endSession(String sessionId, String how){
        jdbc.queryForList("select audit.end_session(CAST(? as uuid), ?)", sessionId, how);
    }
}