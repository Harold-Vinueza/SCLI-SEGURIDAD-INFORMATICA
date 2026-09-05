package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByNombreUsuarioAndClave(String nombreUsuario, String clave);
    
}
