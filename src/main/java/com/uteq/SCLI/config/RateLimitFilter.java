package com.uteq.SCLI.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mitigación básica contra DDoS/fuerza bruta a nivel de aplicación:
 * limita cuántas peticiones puede hacer una misma IP en una ventana de tiempo corta.
 * No reemplaza una protección de red real (firewall, CDN/WAF), pero evita que
 * un solo origen sature el servidor de la aplicación.
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_PETICIONES = 60;      // máximo por IP
    private static final long VENTANA_MS = 10_000;         // por cada 10 segundos

    private static class Contador {
        AtomicInteger cantidad = new AtomicInteger(0);
        volatile long inicioVentana = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Contador> contadoresPorIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // No limitar recursos estáticos: una sola carga de página pide varios (CSS, JS, imágenes)
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/")
                || path.startsWith("/images/") || path.startsWith("/webjars/") || path.startsWith("/static/")
                || path.equals("/favicon.ico") || path.equals("/LABU.png")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = obtenerIp(request);
        Contador contador = contadoresPorIp.computeIfAbsent(ip, k -> new Contador());

        long ahora = System.currentTimeMillis();
        synchronized (contador) {
            if (ahora - contador.inicioVentana > VENTANA_MS) {
                // La ventana de tiempo expiró: reiniciar el conteo
                contador.inicioVentana = ahora;
                contador.cantidad.set(0);
            }
        }

        int cantidadActual = contador.cantidad.incrementAndGet();

        if (cantidadActual > LIMITE_PETICIONES) {
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"mensaje\":\"Demasiadas solicitudes. Intenta de nuevo en unos segundos.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String obtenerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xr = request.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) return xr.trim();
        return request.getRemoteAddr();
    }
}