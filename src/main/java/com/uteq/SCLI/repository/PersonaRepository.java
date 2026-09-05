package com.uteq.SCLI.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PersonaRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Busca personas por nombres, apellidos o id (texto).
     * Retorna filas como Object[]: [ id_persona(Integer), nombres(String), apellidos(String) ]
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public List<Object[]> buscar(String q, int limit, int offset) {
        final String like = "%" + (q == null ? "" : q.trim()) + "%";

        String sql = """
            SELECT
                p.id_persona,
                p.nombres,
                p.apellidos
            FROM
                persona p  -- importante: sin 'app.' para usar search_path
            WHERE
                p.nombres   ILIKE ?1
             OR p.apellidos ILIKE ?2
             OR CAST(p.id_persona AS TEXT) ILIKE ?3
            ORDER BY p.id_persona
            LIMIT ?4 OFFSET ?5
        """;

        return em.createNativeQuery(sql)
                .setParameter(1, like)
                .setParameter(2, like)
                .setParameter(3, like)
                .setParameter(4, Math.max(1, limit))
                .setParameter(5, Math.max(0, offset))
                .getResultList();
    }

    /**
     * Conteo total (útil si luego quieres paginar en frontend)
     */
    @Transactional
    public long contar(String q) {
        final String like = "%" + (q == null ? "" : q.trim()) + "%";

        String sql = """
            SELECT COUNT(1)
            FROM persona p
            WHERE
                p.nombres   ILIKE ?1
             OR p.apellidos ILIKE ?2
             OR CAST(p.id_persona AS TEXT) ILIKE ?3
        """;

        Object n = em.createNativeQuery(sql)
                .setParameter(1, like)
                .setParameter(2, like)
                .setParameter(3, like)
                .getSingleResult();

        // Hibernate puede devolver BigInteger/Long según driver
        if (n instanceof Number num) return num.longValue();
        return Long.parseLong(n.toString());
    }
}
