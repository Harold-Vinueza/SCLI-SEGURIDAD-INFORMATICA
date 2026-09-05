package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.exception.CredencialesInvalidasException;
import com.uteq.SCLI.repository.AuthRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    @PersistenceContext private final EntityManager em;
    private final UserSession userSession;

    /** Overload que acepta ip/ua y obtiene session_id en una sola llamada auditada */
    @Transactional
    public UserSession autenticar(String username, String password, String ip, String ua) {
        em.createNativeQuery("SELECT set_config('search_path', 'public,app', false)").getSingleResult();
        em.createNativeQuery("RESET ROLE").executeUpdate();
        em.createNativeQuery("SELECT set_config('app.current_docente_id', '', true)").getSingleResult();
        em.createNativeQuery("SELECT set_config('app.current_estudiante_id', '', true)").getSingleResult();

        // ✅ UNA sola invocación a fn_login_audit (ya crea evento_login y devuelve session_id)
        AuthRepository.LoginResultView r = authRepository.loginAudit(username, password, ip, ua);
        if (r == null || r.getOk() == null || !r.getOk()) {
            throw new CredencialesInvalidasException("Usuario o contraseña incorrectos");
        }

        try {
            if (r.getDb_role() != null && !r.getDb_role().isBlank()) {
                em.createNativeQuery("SET ROLE " + r.getDb_role()).executeUpdate();

                if ("docente".equalsIgnoreCase(r.getNombre_rol())) {
                    em.createNativeQuery("SELECT set_config('app.current_docente_id', :v, true)")
                            .setParameter("v", String.valueOf(r.getId_persona()))
                            .getSingleResult();
                } else if ("estudiante".equalsIgnoreCase(r.getNombre_rol())) {
                    em.createNativeQuery("SELECT set_config('app.current_estudiante_id', :v, true)")
                            .setParameter("v", String.valueOf(r.getId_persona()))
                            .getSingleResult();
                } else if ("coordinador".equalsIgnoreCase(r.getNombre_rol())) {
                    em.createNativeQuery("SELECT set_config('app.current_coordinador_id', :v, true)")
                            .setParameter("v", String.valueOf(r.getId_persona()))
                            .getSingleResult();
                }
            }
        } catch (Exception e) {
            throw new CredencialesInvalidasException(
                    "No se pudieron aplicar permisos de BD para el rol: " + r.getDb_role(), e);
        }

        // ✅ Rellenar bean de sesión (incluyendo sessionId)
        userSession.setIdUsuario(r.getId_usuario());
        userSession.setIdPersona(r.getId_persona());
        userSession.setUsername(username);
        userSession.setNombreRol(r.getNombre_rol() == null ? "" : r.getNombre_rol().trim().toLowerCase());
        userSession.setDbRole(r.getDb_role());
        userSession.setSessionId(r.getSession_id());  // <- clave para heartbeat/update

        return userSession;
    }

    @Transactional
    public UserSession autenticar(String username, String password) {
        return autenticar(username, password, null, null);
    }
}