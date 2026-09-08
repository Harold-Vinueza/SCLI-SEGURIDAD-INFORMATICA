package com.uteq.SCLI.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AlertaSeguridadService {

    private static final Logger log = LoggerFactory.getLogger(AlertaSeguridadService.class);

    private final JdbcTemplate jdbc;

    public AlertaSeguridadService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Corre cada 60 segundos: revisa intentos fallidos recientes agrupados por IP */
    @Scheduled(fixedRate = 60000)
    public void detectarLoginsFallidosSospechosos() {
        try {
            List<Map<String, Object>> resultado = jdbc.queryForList(
                "SELECT ip, COUNT(*) AS intentos " +
                "FROM audit.vw_logins " +
                "WHERE ok = false AND fecha_hora >= now() - INTERVAL '10 minutes' " +
                "GROUP BY ip HAVING COUNT(*) >= 5"
            );

            for (Map<String, Object> fila : resultado) {
                String ip = String.valueOf(fila.get("ip"));
                Object intentosObj = fila.get("intentos");
                long intentos = intentosObj == null ? 0 : ((Number) intentosObj).longValue();

                boolean yaExiste = existeAlertaReciente(ip, "LOGIN_BRUTE_FORCE");
                if (!yaExiste) {
                    String descripcion = intentos + " intentos de login fallidos en los últimos 10 minutos desde " + ip;
                    jdbc.update(
                        "INSERT INTO app.alerta_seguridad (tipo, descripcion, ip, severidad) VALUES (?, ?, ?, ?)",
                        "LOGIN_BRUTE_FORCE", descripcion, ip, "alta"
                    );
                    log.warn("ALERTA DE SEGURIDAD: {}", descripcion);
                }
            }
        } catch (Exception e) {
            log.error("Error detectando logins fallidos sospechosos: {}", e.getMessage());
        }
    }

    /** Corre cada 60 segundos: revisa accesos denegados repetidos por el mismo usuario */
    @Scheduled(fixedRate = 60000)
    public void detectarAccesosDenegadosSospechosos() {
        try {
            List<Map<String, Object>> resultado = jdbc.queryForList(
                "SELECT username, COUNT(*) AS intentos " +
                "FROM app.evento_acceso_denegado " +
                "WHERE fecha_hora >= now() - INTERVAL '10 minutes' " +
                "GROUP BY username HAVING COUNT(*) >= 3"
            );

            for (Map<String, Object> fila : resultado) {
                String username = String.valueOf(fila.get("username"));
                Object intentosObj = fila.get("intentos");
                long intentos = intentosObj == null ? 0 : ((Number) intentosObj).longValue();

                boolean yaExiste = existeAlertaRecientePorUsuario(username, "ACCESO_NO_AUTORIZADO_REPETIDO");
                if (!yaExiste) {
                    String descripcion = intentos + " intentos de acceso a secciones no autorizadas en los últimos 10 minutos por " + username;
                    jdbc.update(
                        "INSERT INTO app.alerta_seguridad (tipo, descripcion, username, severidad) VALUES (?, ?, ?, ?)",
                        "ACCESO_NO_AUTORIZADO_REPETIDO", descripcion, username, "media"
                    );
                    log.warn("ALERTA DE SEGURIDAD: {}", descripcion);
                }
            }
        } catch (Exception e) {
            log.error("Error detectando accesos denegados sospechosos: {}", e.getMessage());
        }
    }

    private boolean existeAlertaReciente(String ip, String tipo) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app.alerta_seguridad WHERE ip = ? AND tipo = ? AND fecha_hora >= now() - INTERVAL '10 minutes'",
            Integer.class, ip, tipo
        );
        return count != null && count > 0;
    }

    private boolean existeAlertaRecientePorUsuario(String username, String tipo) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app.alerta_seguridad WHERE username = ? AND tipo = ? AND fecha_hora >= now() - INTERVAL '10 minutes'",
            Integer.class, username, tipo
        );
        return count != null && count > 0;
    }
}