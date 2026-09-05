package com.uteq.SCLI.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AsignacionCrudRepository extends CrudRepository<com.uteq.SCLI.model.AsignacionLaboratorio, Integer> {

   @Modifying
  @Query(value = """
    INSERT INTO asignacion_laboratorio
      (id_laboratorio, id_horario, id_docente, materia, id_materia, fecha_asignacion, id_periodo)
    VALUES (:lab, :hor, :doc, :matTxt, :matId, CURRENT_DATE, :per)
  """, nativeQuery = true)
  void insertar(@Param("lab") Integer idLab,
                @Param("hor") Integer idHorario,
                @Param("doc") Integer idDocente,
                @Param("matTxt") String materiaTexto,
                @Param("matId") Integer idMateria,
                @Param("per") Integer idPeriodo);



  @Modifying
@Query(value = """
  UPDATE asignacion_laboratorio
     SET id_docente = :doc,
         id_materia = :matId,
         materia    = NULL
   WHERE id_asignacion = :id
""", nativeQuery = true)
int actualizar(@Param("id") Integer idAsign,
               @Param("doc") Integer idDocente,
               @Param("matId") Integer idMateria);


  @Modifying
  @Query(value = "DELETE FROM asignacion_laboratorio WHERE id_asignacion = :id", nativeQuery = true)
  int borrar(@Param("id") Integer id);

}