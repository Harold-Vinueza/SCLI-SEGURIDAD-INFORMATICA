// com.uteq.SCLI.service.ProfileService.java
package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.ProfileDTO;
import com.uteq.SCLI.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ProfileService {
  private final ProfileRepository repo;
  public ProfileService(ProfileRepository repo){ this.repo = repo; }

  public ProfileDTO get(int idPersona){ return repo.findByIdPersona(idPersona); }

  public void save(ProfileDTO dto, MultipartFile foto) throws Exception{
    if(foto!=null && !foto.isEmpty()){
      String base = System.getProperty("user.dir") + "/uploads/perfiles/";
      Files.createDirectories(Path.of(base));
      String name = dto.getIdPersona() + ".jpg";
      foto.transferTo(new File(base + name));
      dto.setFotoUrl("/uploads/perfiles/" + name);
    }
    repo.updatePersona(dto);
    repo.updateDocente(dto);
  }

  public ProfileRepository.Result cambiarClave(int idUsuario, String actual, String nueva){
    return repo.cambiarClave(idUsuario, actual, nueva);
  }
}
