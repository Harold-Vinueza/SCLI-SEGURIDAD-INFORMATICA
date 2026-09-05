// src/main/java/com/uteq/SCLI/service/AuditService.java
package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.LoginAuditDTO;
import com.uteq.SCLI.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditRepository repo;

    public AuditService(AuditRepository repo) { this.repo = repo; }

    public List<LoginAuditDTO> listar(String user, Boolean ok, String desde, String hasta, Integer limit) {
        return repo.findLogins(user, ok, desde, hasta, limit)
                   .stream().map(LoginAuditDTO::from).toList();
    }

    public List<AuditRepository.CambioRow> listarCambios(String user, String desde, String hasta, Integer limit){
  return repo.findCambios(user, desde, hasta, limit);
}
public List<AuditRepository.SesionRow> listarSesiones(String user, String desde, String hasta, Integer limit){
  return repo.findSesiones(user, desde, hasta, limit);
}



}
