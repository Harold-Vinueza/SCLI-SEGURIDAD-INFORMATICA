package com.uteq.SCLI.repository;

import com.uteq.SCLI.model.Mobiliario;
import com.uteq.SCLI.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MobiliarioRepository extends JpaRepository<Mobiliario, Integer> {
    List<Mobiliario> findByLaboratorio(Laboratorio lab);
    List<Mobiliario> findByEstadoIgnoreCase(String estado);
    List<Mobiliario> findByTipoMobiliarioContainingIgnoreCase(String q);
}