package com.uteq.SCLI.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Repository
public class ReporteRepositoryFreddy {

    @PersistenceContext
    private EntityManager entityManager;

    // helper para mapear filas -> Map<String,Object>
    private List<Map<String, Object>> runToMap(String sql) {
        return entityManager.createNativeQuery(sql)
                .unwrap(NativeQuery.class)
                .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
                .getResultList();
    }

    // 1) Asistencia por materia
    public List<Map<String, Object>> asistenciaPorMateria() {
        String sql = "SELECT * FROM rep.vw_asistencia_por_materia";
        return runToMap(sql);
    }

    // 2) Uso de laboratorios
    public List<Map<String, Object>> usoLaboratorios() {
        String sql = "SELECT * FROM rep.vw_uso_laboratorios";
        return runToMap(sql);
    }

    // 3) Carga de docentes
    public List<Map<String, Object>> cargaDocentes() {
        String sql = "SELECT * FROM rep.vw_carga_docentes";
        return runToMap(sql);
    }

    // 4) Top asistencia estudiantes
    public List<Map<String, Object>> topAsistenciaEstudiantes() {
        String sql = "SELECT * FROM rep.vw_top_asistencia_estudiantes";
        return runToMap(sql);
    }

    public List<Map<String,Object>> reporteFallos(Integer labId, LocalDate desde, LocalDate hasta, String estado){
        String sql = "SELECT * FROM rep.fn_reporte_fallos(:labId, :desde, :hasta, :estado)";
        var q = entityManager.createNativeQuery(sql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter("labId", labId)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("estado", estado)
                .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
        return q.getResultList();
    }

    public List<Map<String,Object>> tendenciaMensualFallos(LocalDate desde, LocalDate hasta, Integer labId, String estado){
        String sql = "SELECT * FROM rep.fn_resumen_fallos(:desde, :hasta, :labId, :estado)";
        var q = entityManager.createNativeQuery(sql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .setParameter("desde",  desde)
                .setParameter("hasta",  hasta)
                .setParameter("labId",  labId)
                .setParameter("estado", estado)
                .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
        return q.getResultList();
    }


    // 7) Ocupación / comparativo de uso de laboratorios
    public List<Map<String, Object>> ocupacionLaboratorios() {
        String sql = "SELECT * FROM rep.v_ocupacion_labs";
        return runToMap(sql);
    }
    // 8) Resumen del sistema
    public List<Map<String, Object>> resumenSistema() {
        String sql = "SELECT * FROM rep.vw_resumen_sistema";
        return runToMap(sql);
    }
}