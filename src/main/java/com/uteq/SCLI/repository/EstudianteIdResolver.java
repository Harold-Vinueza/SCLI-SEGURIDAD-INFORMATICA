package com.uteq.SCLI.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EstudianteIdResolver {
    private final JdbcTemplate jdbc;

    public EstudianteIdResolver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Devuelve id_estudiante por id_persona, o null si no existe */
    public Integer findIdEstByIdPersona(Integer idPersona) {
        if (idPersona == null) return null;
        try {
            return jdbc.query(
                    "SELECT e.id_estudiante FROM public.estudiante e WHERE e.id_persona = ? LIMIT 1",
                    ps -> ps.setInt(1, idPersona),
                    rs -> rs.next() ? rs.getInt(1) : null
            );
        } catch (Exception e) {
            return null;
        }
    }
}