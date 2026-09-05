// src/main/java/com/uteq/SCLI/repository/CoordinadorRepository.java
package com.uteq.SCLI.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.uteq.SCLI.model.SolicitudCoordinacion;
import com.uteq.SCLI.repository.CoordinadorRepository.LabDisp;


@Repository
public interface CoordinadorRepository extends JpaRepository<SolicitudCoordinacion, Integer> {

  // ======= Catálogos para el modal =======

  // Materias por carrera (UI: combo Materia)
  @Query(value = """
      SELECT m.id_materia AS id, m.cod_materia AS cod, m.nombre_materia AS nombre
      FROM Materia m
      WHERE (:idCarrera IS NULL OR m.id_carrera = :idCarrera)
      ORDER BY m.nombre_materia
      """, nativeQuery = true)
  List<Object[]> materiasPorCarrera(@Param("idCarrera") Integer idCarrera);

  // Docentes que dictan una materia (UI: combo Docente)
  @Query(value = """
      SELECT d.id_docente AS id,
             (p.nombres || ' ' || p.apellidos) AS label
      FROM DocenteMateria dm
      JOIN Docente d   ON d.id_docente = dm.id_docente
      JOIN Persona p   ON p.id_persona = d.id_persona
      WHERE dm.id_materia = :idMateria
      ORDER BY label
      """, nativeQuery = true)
  List<Object[]> docentesPorMateria(@Param("idMateria") Integer idMateria);


  // ====== Listar solicitudes del coordinador (filtrable por estado) ======
  @Query(value = """
      SELECT s.id_solicitud,
             s.fecha_solicitud,
             s.estado_solicitud,
             c.nombre_carrera,
             (SELECT COUNT(*) FROM detallesolicitudcoordinacion d WHERE d.id_solicitud = s.id_solicitud) AS items,
             COALESCE(s.observaciones,'')
      FROM solicitudcoordinacion s
      JOIN carrera c ON c.id_carrera = s.id_carrera
      WHERE c.id_persona_coordinador = :idPersona
        AND (:estado IS NULL OR s.estado_solicitud ILIKE :estado)
      ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC
      """, nativeQuery = true)
  List<Object[]> listarSolicitudes(@Param("idPersona") Integer idPersona,
                                   @Param("estado") String estado);

  @Query(value = """
      SELECT EXISTS (
        SELECT 1
        FROM solicitudcoordinacion s
        JOIN carrera c ON c.id_carrera = s.id_carrera
        WHERE s.id_solicitud = :idSolicitud
          AND c.id_persona_coordinador = :idPersona
      )
      """, nativeQuery = true)
  boolean esPropia(@Param("idSolicitud") Integer idSolicitud,
                   @Param("idPersona") Integer idPersona);


      // ====== Detalles de una solicitud (slots del horario) ======
  @Query(value = """
      SELECT d.id_detalle,
             d.id_horario,
             d.materia,
             h.jornada,
             h.dia_semana,
             TO_CHAR(h.hora_inicio,'HH24:MI') AS hi,
             TO_CHAR(h.hora_fin,'HH24:MI')    AS hf
      FROM detallesolicitudcoordinacion d
      JOIN horario h ON h.id_horario = d.id_horario
      WHERE d.id_solicitud = :idSolicitud
      ORDER BY h.dia_semana, h.hora_inicio
      """, nativeQuery = true)
  List<Object[]> detallesSolicitud(@Param("idSolicitud") Integer idSolicitud);

  // ====== Anular (solo si sigue Pendiente) ======
  @Modifying
  @Query(value = """
      UPDATE solicitudcoordinacion
      SET estado_solicitud = 'Anulada'
      WHERE id_solicitud = :idSolicitud
        AND estado_solicitud = 'Pendiente'
      """, nativeQuery = true)
  int anularSolicitud(@Param("idSolicitud") Integer idSolicitud);
  // ======= Escritura de solicitud =======

  // Crea cabecera y retorna id
  @Query(value = """
      INSERT INTO solicitudcoordinacion (id_carrera, fecha_solicitud, estado_solicitud, observaciones)
      VALUES (:idCarrera, CURRENT_DATE, 'Pendiente', :obs)
      RETURNING id_solicitud
      """, nativeQuery = true)
  Integer crearSolicitud(@Param("idCarrera") Integer idCarrera,
                         @Param("obs") String observaciones);

 // ===== Texto de materia/docente =====
@Query(value = "SELECT (m.cod_materia || ' - ' || m.nombre_materia) FROM materia m WHERE m.id_materia = :id", nativeQuery = true)
String materiaTexto(@Param("id") Integer idMateria);

  // Recupera etiqueta de docente
  @Query(value = """
  SELECT (p.nombres || ' ' || p.apellidos)
    FROM docente d JOIN persona p ON p.id_persona = d.id_persona
   WHERE d.id_docente = :id
  """, nativeQuery = true)
String docenteTexto(@Param("id") Integer idDocente);

  // Inserta detalle
  @Modifying
@Query(value = """
  INSERT INTO detallesolicitudcoordinacion
         (id_solicitud, id_horario, materia)
  VALUES (:idSolicitud, :idHorario, :materia)
  """, nativeQuery = true)
void insertarDetalle(@Param("idSolicitud") Integer idSolicitud,
                     @Param("idHorario") Integer idHorario,
                     @Param("materia") String materia);


  // Chequeo simple de ocupación del slot (ya asignado)
  @Query(value = """
      SELECT EXISTS (
        SELECT 1
        FROM asignacion_laboratorio a
        WHERE a.id_horario = :idHorario
      )
      """, nativeQuery = true)
  boolean slotOcupado(@Param("idHorario") Integer idHorario);

  // ===== Ocupación existente por jornada (muestra en la grilla) =====
@Query(value = """
  SELECT h.id_horario,
         l.cod_laboratorio,
         a.materia
    FROM asignacion_laboratorio a
    JOIN horario h      ON h.id_horario = a.id_horario
    JOIN laboratorio l  ON l.id_laboratorio = a.id_laboratorio
   WHERE h.jornada ILIKE :jornada
     AND a.id_periodo = :idPeriodo
  """, nativeQuery = true)
List<Object[]> ocupacionPorJornada(@Param("jornada") String jornada,
                                   @Param("idPeriodo") Integer idPeriodo);



    // Carreras del coordinador (para el combo)
  @Query(value = """
      SELECT c.id_carrera, c.nombre_carrera
      FROM carrera c
      WHERE c.id_persona_coordinador = :idPersona
      ORDER BY c.nombre_carrera
      """, nativeQuery = true)
  List<Object[]> carrerasMias(@Param("idPersona") Integer idPersona);

  // Slots de horario por jornada
  @Query(value = """
      SELECT h.id_horario,
             h.jornada,
             h.dia_semana,
             TO_CHAR(h.hora_inicio,'HH24:MI') AS hi,
             TO_CHAR(h.hora_fin,'HH24:MI')    AS hf
      FROM horario h
      WHERE (:jornada IS NULL OR h.jornada ILIKE :jornada)
      ORDER BY h.hora_inicio, h.dia_semana
      """, nativeQuery = true)
  List<Object[]> horariosPorJornada(@Param("jornada") String jornada);

  /*para el administrador */

    // ==== ADMIN: listar todas las solicitudes (opcionalmente por estado) ====
  // ==== ADMIN: listar todas las solicitudes (con nombre de coordinador) ====
// CoordinadorRepository.java

// Reemplaza el listarSolicitudesAdmin(...) por este (misma firma)
// CoordinadorRepository.java
@Query(value = """
  SELECT
     s.id_solicitud,                                 -- [0]
     s.fecha_solicitud,                              -- [1]
     s.estado_solicitud,                             -- [2]
     c.nombre_carrera,                               -- [3]
     (p.nombres || ' ' || p.apellidos) AS coordinador, -- [4]
     (SELECT COUNT(*)
        FROM detallesolicitudcoordinacion d
       WHERE d.id_solicitud = s.id_solicitud) AS items, -- [5]
     COALESCE(s.observaciones,'') AS obs,            -- [6]
     ( /* fecha mínima de asignación en período activo */
       SELECT TO_CHAR(MIN(a.fecha_asignacion),'YYYY-MM-DD')
         FROM detallesolicitudcoordinacion d
         JOIN asignacion_laboratorio a
           ON a.id_horario = d.id_horario
          AND a.id_periodo = (SELECT id_periodo
                                FROM periodolectivo
                               WHERE activo = TRUE
                               LIMIT 1)
        WHERE d.id_solicitud = s.id_solicitud
     ) AS f_asig                                     -- [7]
  FROM solicitudcoordinacion s
  JOIN carrera  c ON c.id_carrera = s.id_carrera
  JOIN persona  p ON p.id_persona = c.id_persona_coordinador
  WHERE (:estado IS NULL OR s.estado_solicitud ILIKE :estado)
  ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC
  """, nativeQuery = true)
List<Object[]> listarSolicitudesAdmin(@Param("estado") String estado);


  // ==== ADMIN: update estado + observaciones ====
 @Modifying(clearAutomatically = true)
@Query(value = """
    UPDATE solicitudcoordinacion
       SET estado_solicitud = :estado,
           observaciones = CASE
               WHEN COALESCE(:obs,'') = '' THEN observaciones
               WHEN COALESCE(observaciones,'') = '' THEN :obs
               ELSE observaciones || chr(10) || :obs
           END
     WHERE id_solicitud = :id
    """, nativeQuery = true)
int actualizarEstado(@Param("id") Integer idSolicitud,
                     @Param("estado") String estado,
                     @Param("obs") String observaciones);
  // Limpiar / rearmar detalles (para "Propuesta")
  @Modifying
  @Query(value = "DELETE FROM detallesolicitudcoordinacion WHERE id_solicitud = :id", nativeQuery = true)
  int limpiarDetalles(@Param("id") Integer idSolicitud);

  // Insertar detalle (se reutiliza el mismo formato que usa el coordinador)
  @Modifying
  @Query(value = """
      INSERT INTO detallesolicitudcoordinacion (id_solicitud, id_horario, materia)
      VALUES (:idSolicitud, :idHorario, :materia)
      """, nativeQuery = true)
  void insertarDetalleAdmin(@Param("idSolicitud") Integer idSolicitud,
                            @Param("idHorario") Integer idHorario,
                            @Param("materia") String materia);

  // Aprobar -> crear asignación de laboratorio
@Modifying
@Query(value = """
    INSERT INTO asignacion_laboratorio
           (id_horario, materia, id_periodo, fecha_asignacion)
    VALUES (:idHorario, :materia, :idPeriodo, CURRENT_DATE)
    """, nativeQuery = true)
int insertarAsignacion(@Param("idHorario") Integer idHorario,
                       @Param("materia")   String  materia,
                       @Param("idPeriodo") Integer idPeriodo);

   @Query(value = "SELECT id_periodo FROM periodolectivo WHERE activo = TRUE LIMIT 1", nativeQuery = true)
Integer periodoActivoId();


 @Query(value = "SELECT id_carrera FROM solicitudcoordinacion WHERE id_solicitud=:id", nativeQuery = true)
  Integer carreraDeSolicitud(@Param("id") Integer idSolicitud);


 interface LabDisp {
  Integer getId(); String getCodigo(); String getNombre(); Boolean getPreferido();
}
  @Query(value = """
  WITH pref AS (
    SELECT cl.id_laboratorio
      FROM carreralaboratorio cl
     WHERE cl.id_carrera = :idCarrera
  ),
  ocp AS (
    SELECT a.id_laboratorio
      FROM asignacion_laboratorio a
     WHERE a.id_horario = :idHorario
       AND a.id_periodo = :idPeriodo
  )
  SELECT l.id_laboratorio  AS id,
         l.cod_laboratorio AS codigo,
         l.nombre_laboratorio AS nombre,
         (l.id_laboratorio IN (SELECT * FROM pref)) AS preferido
    FROM laboratorio l
    LEFT JOIN ocp ON ocp.id_laboratorio = l.id_laboratorio
   WHERE ocp.id_laboratorio IS NULL
   ORDER BY preferido DESC, l.cod_laboratorio
  """, nativeQuery = true)
List<LabDisp> labsDisponibles(@Param("idCarrera") Integer idCarrera,
                              @Param("idHorario") Integer idHorario,
                              @Param("idPeriodo") Integer idPeriodo);

// 4) insertar asignación CON laboratorio, docente opcional y período
// Antes (sin fecha)
// INSERT INTO asignacion_laboratorio
//        (id_laboratorio, id_horario, materia, id_docente, id_periodo)
// VALUES (:idLab,        :idHorario, :materia, :idDocente, :idPeriodo)

@Modifying
@Query(value = """
  INSERT INTO asignacion_laboratorio
         (id_laboratorio, id_horario, materia, id_docente, id_periodo, fecha_asignacion)
  VALUES (:idLab,        :idHorario, :materia, :idDocente, :idPeriodo, CURRENT_DATE)
""", nativeQuery = true)
int insertarAsignacionConLaboratorio(@Param("idLab") Integer idLab,
                                     @Param("idHorario") Integer idHorario,
                                     @Param("materia") String materia,
                                     @Param("idDocente") Integer idDocente,
                                     @Param("idPeriodo") Integer idPeriodo);


// ===== Chequeo de slot ocupado en el período =====
@Query(value = """
  SELECT EXISTS(
    SELECT 1 FROM asignacion_laboratorio
     WHERE id_horario = :idHorario AND id_periodo = :idPeriodo
  )
  """, nativeQuery = true)
boolean slotOcupadoEnPeriodo(@Param("idHorario") Integer idHorario,
                             @Param("idPeriodo") Integer idPeriodo);




// === LABS por carrera (para combo inicial) ===
@Query(value = """
    SELECT 
      l.id_laboratorio    AS id,
      l.cod_laboratorio   AS codigo,
      l.nombre_laboratorio AS nombre,
      (l.id_laboratorio IN (
         SELECT cl.id_laboratorio FROM carreralaboratorio cl WHERE cl.id_carrera = :idCarrera
       )) AS preferido
    FROM laboratorio l
    ORDER BY preferido DESC, l.cod_laboratorio
    """, nativeQuery = true)
List<Object[]> labsPorCarrera(@Param("idCarrera") Integer idCarrera);

// === OCUPACIÓN por laboratorio + jornada (solo ids de horario) ===
@Query(value = """
    SELECT a.id_horario
      FROM asignacion_laboratorio a
      JOIN horario h ON h.id_horario = a.id_horario
     WHERE a.id_laboratorio = :idLab
       AND a.id_periodo    = :idPeriodo
       AND (:jornada IS NULL OR h.jornada ILIKE :jornada)
    """, nativeQuery = true)
List<Integer> ocupadosPorLaboratorio(@Param("idLab") Integer idLaboratorio,
                                     @Param("idPeriodo") Integer idPeriodo,
                                     @Param("jornada") String jornada);


                                     // Etiqueta de laboratorio "COD - Nombre"
@Query(value = """
  SELECT (l.cod_laboratorio || ' - ' || l.nombre_laboratorio)
    FROM laboratorio l
   WHERE l.id_laboratorio = :id
  """, nativeQuery = true)
String labTexto(@Param("id") Integer idLaboratorio);
                                   
}

