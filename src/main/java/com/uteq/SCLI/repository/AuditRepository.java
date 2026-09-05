package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Usuario, Integer> {

// agrega a tu AuditRepository
interface CambioRow {
  Instant getFechaHora();
  String  getEsquema();
  String  getTabla();
  String  getAccion();
  String  getUsername();
  String  getNombreRol();
  String  getDbRole();
  String  getIp();
  String  getUserAgent();
  Integer getIdUsuario();
  Integer getIdPersona();
  String  getNombreCompleto();
  String  getOldRow();
  String  getNewRow();
}

@Query(value = """
  SELECT c.fecha_hora AS fechaHora, c.esquema, c.tabla, c.accion,
         c.username, c.nombre_rol AS nombreRol, c.db_role AS dbRole,
         CAST(c.ip AS TEXT) AS ip, c.user_agent AS userAgent,
         c.id_usuario AS idUsuario, c.id_persona AS idPersona,
         c.nombre_completo AS nombreCompleto,
         c.old_row::text AS oldRow, c.new_row::text AS newRow
  FROM audit.vw_cambios c
  WHERE (:user IS NULL OR c.username ILIKE CONCAT('%',:user,'%'))
    AND (:desde IS NULL OR c.fecha_hora >= CAST(:desde AS timestamptz))
    AND (:hasta IS NULL OR c.fecha_hora <= CAST(:hasta AS timestamptz))
  ORDER BY c.fecha_hora DESC
  LIMIT :limit
""", nativeQuery = true)
List<CambioRow> findCambios(@Param("user") String user,
                            @Param("desde") String desde,
                            @Param("hasta") String hasta,
                            @Param("limit") Integer limit);

interface SesionRow {
  String  getSessionId();
  Instant getStartAt();
  Instant getLastSeen();
  Instant getEndAt();
  Boolean getActiva();
  Long    getDuracionSeg();
  String  getUsername();
  String  getNombreRol();
  String  getDbRole();
  String  getIp();
  String  getUserAgent();
}

@Query(value = """
  SELECT session_id AS sessionId, start_at AS startAt, last_seen AS lastSeen, end_at AS endAt,
         activa, duracion_seg AS duracionSeg, username, nombre_rol AS nombreRol, db_role AS dbRole,
         CAST(ip AS TEXT) AS ip, user_agent AS userAgent
  FROM audit.vw_sesiones
  WHERE (:user IS NULL OR username ILIKE CONCAT('%',:user,'%'))
    AND (:desde IS NULL OR start_at >= CAST(:desde AS timestamptz))
    AND (:hasta IS NULL OR COALESCE(end_at, now()) <= CAST(:hasta AS timestamptz))
  ORDER BY start_at DESC
  LIMIT :limit
""", nativeQuery = true)
List<SesionRow> findSesiones(@Param("user") String user,
                             @Param("desde") String desde,
                             @Param("hasta") String hasta,
                             @Param("limit") Integer limit);


    // Proyección (nota: nombres en camelCase)
    interface LoginEventRow {
        Instant getFechaHora();
        String  getUsername();
        Boolean getOk();
        String  getNombreRol();
        String  getDbRole();
        String  getIp();
        String  getUserAgent();
        Integer getIdUsuario();
        Integer getIdPersona();
        String  getNombreCompleto();
    }

    @Query(value = """
        SELECT
          fecha_hora        AS fechaHora,
          username          AS username,
          ok                AS ok,
          nombre_rol        AS nombreRol,
          db_role           AS dbRole,
          CAST(ip AS TEXT)  AS ip,
          user_agent        AS userAgent,
          id_usuario        AS idUsuario,
          id_persona        AS idPersona,
          nombre_completo   AS nombreCompleto
        FROM audit.vw_logins
        WHERE (:user  IS NULL OR username ILIKE CONCAT('%', :user, '%'))
          AND (:ok    IS NULL OR ok = :ok)
          AND (:desde IS NULL OR fecha_hora >= CAST(:desde AS timestamptz))
          AND (:hasta IS NULL OR fecha_hora <= CAST(:hasta AS timestamptz))
        ORDER BY fecha_hora DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<LoginEventRow> findLogins(@Param("user") String user,
                                   @Param("ok") Boolean ok,
                                   @Param("desde") String desde,
                                   @Param("hasta") String hasta,
                                   @Param("limit") Integer limit);
}