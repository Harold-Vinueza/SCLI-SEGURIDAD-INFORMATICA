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
}