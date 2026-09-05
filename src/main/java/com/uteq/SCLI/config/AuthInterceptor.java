package com.uteq.SCLI.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor; // <-- IMPORTANTE

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor { // <-- implements

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        HttpSession session = req.getSession(false);
        String path = req.getRequestURI();

        // Permitir login y recursos estáticos
        if (path.startsWith("/login")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                
                || path.equals("/LABU.png")) {
            return true;
        }

        // Proteger /dashboard/**
        if (path.startsWith("/dashboard")) {
            if (session == null || session.getAttribute("usuario") == null) {
                res.sendRedirect("/login");
                return false;
            }
        }

        return true;
    }
}
