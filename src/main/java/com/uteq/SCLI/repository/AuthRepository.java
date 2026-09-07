package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<Usuario, Integer> {

    interface LoginResultView {
        Boolean getOk();
        Integer getId_usuario();
        Integer getId_persona();
        String  getNombre_rol();
        String  getDb_role();
        // ✅ NUEVO: session_id para no tener que invocar dos veces fn_login_audit
        String  getSession_id();
    }

    @Query(value = "select * from app.fn_login_v2(:u, :p)", nativeQuery = true)
    LoginResultView login(@Param("u") String username, @Param("p") String password);

    @Query(value = "select * from app.fn_login_audit(:u, :p, CAST(:ip AS inet), :ua)", nativeQuery = true)
    LoginResultView loginAudit(@Param("u") String username,
                               @Param("p") String password,
                               @Param("ip") String ip,
                               @Param("ua") String userAgent);
                               
        @Query(value = "select bloqueado_hasta from app.app_usuario where username = :u", nativeQuery = true)
    java.time.Instant findBloqueadoHasta(@Param("u") String username);

    
    interface RecuperacionUsuarioView {
        Integer getId_usuario();
        String  getCorreo();
        String  getNombres();
    }

    @Query(value = "SELECT au.id_usuario as id_usuario, p.correo as correo, p.nombres as nombres " +
                   "FROM app.app_usuario au JOIN persona p ON p.id_persona = au.id_persona " +
                   "WHERE au.username = :identificador OR p.correo = :identificador " +
                   "AND au.activo = true", nativeQuery = true)
    RecuperacionUsuarioView buscarParaRecuperacion(@Param("identificador") String identificador);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT INTO app.token_recuperacion (id_usuario, token, expira_en) VALUES (:idUsuario, :token, :expiraEn)", nativeQuery = true)
    void guardarTokenRecuperacion(@Param("idUsuario") Integer idUsuario,
                                  @Param("token") String token,
                                  @Param("expiraEn") java.time.Instant expiraEn);

    interface TokenRecuperacionView {
        Integer getId_usuario();
        java.time.Instant getExpira_en();
        Boolean getUsado();
    }

    @Query(value = "SELECT id_usuario, expira_en, usado FROM app.token_recuperacion WHERE token = :token", nativeQuery = true)
    TokenRecuperacionView buscarToken(@Param("token") String token);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE app.token_recuperacion SET usado = true WHERE token = :token", nativeQuery = true)
    void marcarTokenUsado(@Param("token") String token);

    
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE app.app_usuario SET hash_password = crypt(:nueva, gen_salt('bf')), intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id_usuario = :idUsuario", nativeQuery = true)
    void actualizarClaveDirecta(@Param("idUsuario") Integer idUsuario, @Param("nueva") String nueva);
}