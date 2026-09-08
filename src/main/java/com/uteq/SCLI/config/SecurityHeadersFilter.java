package com.uteq.SCLI.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Evita que el sitio se muestre dentro de un <iframe> de otro dominio (protege contra clickjacking)
        response.setHeader("X-Frame-Options", "DENY");

        // Evita que el navegador intente "adivinar" el tipo de contenido (protege contra MIME-sniffing)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Controla qué recursos externos puede cargar la página (protege contra XSS e inyección de scripts externos)
                response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "img-src 'self' data:; " +
                "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                "font-src 'self' https://cdnjs.cloudflare.com data:; " +
                "connect-src 'self' https://api.ipify.org https://ifconfig.me");

        // Limita cuánta información de la URL de origen se envía al navegar a otro sitio
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Desactiva el acceso a APIs sensibles del navegador que la app no necesita
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        chain.doFilter(request, response);
    }
}