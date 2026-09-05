package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Equipo;

import com.uteq.SCLI.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipoRepository extends JpaRepository<Equipo, Integer> {

    
    boolean existsByCodigoEquipoIgnoreCase(String codigoEquipo);
    Equipo findByCodigoEquipoIgnoreCase(String codigoEquipo);



    List<Equipo> findByLaboratorio(Laboratorio laboratorio);
    List<Equipo> findByEstadoIgnoreCase(String estado);
    List<Equipo> findByCodigoEquipoContainingIgnoreCaseOrTipoEquipoContainingIgnoreCaseOrMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(
        String codigo, String tipo, String marca, String modelo
    );
}

