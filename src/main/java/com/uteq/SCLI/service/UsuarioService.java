package com.uteq.SCLI.service;

import com.uteq.SCLI.exception.CredencialesInvalidasException;
import com.uteq.SCLI.model.Rol;
import com.uteq.SCLI.model.Usuario;
import com.uteq.SCLI.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Autenticación por nombre_usuario y clave (texto plano)
    public Usuario autenticar(String nombreUsuario, String clave) {
        return usuarioRepository.findByNombreUsuarioAndClave(nombreUsuario, clave)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contraseña incorrectos"));
    }

    // Devuelve el primer rol en minúsculas
    public String obtenerRolPrincipal(Usuario usuario) {
        return usuario.getRoles().stream()
                .findFirst()
                .map(Rol::getNombreRol)
                .map(String::toLowerCase)
                .orElse("");
    }
}
