// src/main/java/com/uteq/SCLI/config/DbRoleResetFilter.java
package com.uteq.SCLI.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DbRoleResetFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbc;

    public DbRoleResetFilter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
         
            try { jdbc.execute("RESET ROLE"); } catch (Exception ignored) {}
            try { jdbc.execute("SELECT set_config('app.current_docente_id', NULL, true)"); } catch (Exception ignored) {}
            try { jdbc.execute("SELECT set_config('app.current_estudiante_id', NULL, true)"); } catch (Exception ignored) {}
        }
    }
}
