package com.uteq.SCLI.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiemExportService {

    private static final Logger log = LoggerFactory.getLogger(SiemExportService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path CARPETA_SIEM = Path.of("logs", "siem");

    private final JdbcTemplate jdbc;

    // Marcas de tiempo del último evento exportado por fuente (en memoria; se reinicia al reiniciar la app)
    private Instant ultimoLogin = Instant.now().minusSeconds(60);
    private Instant ultimoAccesoDenegado = Instant.now().minusSeconds(60);
    private Instant ultimaAlerta = Instant.now().minusSeconds(60);

    public SiemExportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedRate = 60000)
    public void exportarEventosSiem() {
        try {
            Files.createDirectories(CARPETA_SIEM);
        } catch (IOException e) {
            log.error("No se pudo crear la carpeta de exportación SIEM: {}", e.getMessage());
            return;
        }

               try { exportarLogins(); } catch (Exception e) { log.error("Error exportando logins: {}", e.getMessage()); }
        try { exportarAccesosDenegados(); } catch (Exception e) { log.error("Error exportando accesos denegados: {}", e.getMessage()); }
        try { exportarAlertas(); } catch (Exception e) { log.error("Error exportando alertas: {}", e.getMessage()); }
    }

    private void exportarLogins() {
        List<Map<String, Object>> filas = jdbc.queryForList(
            "SELECT fecha_hora, username, ok, nombre_rol, host(ip) AS ip, user_agent " +
            "FROM audit.vw_logins WHERE fecha_hora > ? ORDER BY fecha_hora ASC",
            java.sql.Timestamp.from(ultimoLogin)
        );
        for (Map<String, Object> fila : filas) {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("event_type", "login");
            evento.put("timestamp", fila.get("fecha_hora"));
            evento.put("username", fila.get("username"));
            evento.put("ok", fila.get("ok"));
            evento.put("nombre_rol", fila.get("nombre_rol"));
            evento.put("ip", fila.get("ip"));
            evento.put("user_agent", fila.get("user_agent"));
            escribirEvento(evento);
            actualizarSiEsMasReciente(fila, "login");
        }
    }

    private void exportarAccesosDenegados() {
        List<Map<String, Object>> filas = jdbc.queryForList(
            "SELECT fecha_hora, username, nombre_rol, ruta, ip, motivo " +
            "FROM app.evento_acceso_denegado WHERE fecha_hora > ? ORDER BY fecha_hora ASC",
            java.sql.Timestamp.from(ultimoAccesoDenegado)
        );
        for (Map<String, Object> fila : filas) {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("event_type", "acceso_denegado");
            evento.put("timestamp", fila.get("fecha_hora"));
            evento.put("username", fila.get("username"));
            evento.put("nombre_rol", fila.get("nombre_rol"));
            evento.put("ruta", fila.get("ruta"));
            evento.put("ip", fila.get("ip"));
            evento.put("motivo", fila.get("motivo"));
            escribirEvento(evento);
            actualizarSiEsMasReciente(fila, "acceso_denegado");
        }
    }

    private void exportarAlertas() {
        List<Map<String, Object>> filas = jdbc.queryForList(
            "SELECT fecha_hora, tipo, descripcion, ip, username, severidad " +
            "FROM app.alerta_seguridad WHERE fecha_hora > ? ORDER BY fecha_hora ASC",
            java.sql.Timestamp.from(ultimaAlerta)
        );
        for (Map<String, Object> fila : filas) {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("event_type", "alerta_seguridad");
            evento.put("timestamp", fila.get("fecha_hora"));
            evento.put("tipo", fila.get("tipo"));
            evento.put("descripcion", fila.get("descripcion"));
            evento.put("ip", fila.get("ip"));
            evento.put("username", fila.get("username"));
            evento.put("severidad", fila.get("severidad"));
            escribirEvento(evento);
            actualizarSiEsMasReciente(fila, "alerta_seguridad");
        }
    }

    private void actualizarSiEsMasReciente(Map<String, Object> fila, String fuente) {
        Object ts = fila.get("fecha_hora");
        if (!(ts instanceof java.sql.Timestamp sqlTs)) return;
        Instant instante = sqlTs.toInstant();
        switch (fuente) {
            case "login" -> { if (instante.isAfter(ultimoLogin)) ultimoLogin = instante; }
            case "acceso_denegado" -> { if (instante.isAfter(ultimoAccesoDenegado)) ultimoAccesoDenegado = instante; }
            case "alerta_seguridad" -> { if (instante.isAfter(ultimaAlerta)) ultimaAlerta = instante; }
        }
    }

    private void escribirEvento(Map<String, Object> evento) {
        try {
            String linea = MAPPER.writeValueAsString(evento) + System.lineSeparator();
            String nombreArchivo = LocalDate.now(ZoneId.systemDefault()) + ".jsonl";
            Path archivo = CARPETA_SIEM.resolve(nombreArchivo);
            Files.writeString(archivo, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("No se pudo escribir evento SIEM: {}", e.getMessage());
        }
    }
}