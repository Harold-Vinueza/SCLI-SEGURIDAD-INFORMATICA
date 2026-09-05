package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.MateriaCardDTO;
import com.uteq.SCLI.dto.PaseListaVM;
import com.uteq.SCLI.dto.StudentAttendanceRow;
import com.uteq.SCLI.repository.AsistenciaJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AsistenciaService {

  private final AsistenciaJdbcRepository repo;

  public AsistenciaService(AsistenciaJdbcRepository repo) {
    this.repo = repo;
  }

  public Integer idDocentePorUsername(String username) { return repo.idDocentePorUsername(username); }
  public Integer idDocentePorIdPersona(Integer idPersona) { return repo.idDocentePorIdPersona(idPersona); }
  public List<MateriaCardDTO> materiasDeDocente(Integer idDocente) { return repo.materiasDeDocente(idDocente); }

  @Transactional
  public PaseListaVM abrirPase(Integer idDocente, Integer idMateria,
                               LocalDate fecha, Integer idLab, String tema) {
    Integer idReg = repo.getOrCreateRegistro(idDocente, idMateria, fecha, idLab, tema);
    PaseListaVM cab = repo.cargarCabecera(idReg);
    List<StudentAttendanceRow> filas = repo.cargarFilas(idReg, idDocente, idMateria);
    return new PaseListaVM(
        cab.idRegistro(), idMateria, cab.nombreMateria(),
        cab.fechaClase(), cab.temaClase(), cab.nombreDocente(),
        cab.nombreLaboratorio(), filas
    );
  }

// AsistenciaService.java
public void guardar(Integer idDocente, Integer idRegistro, Map<Integer, Boolean> marcas) {
  repo.guardarMarcas(idDocente, idRegistro, marcas);
}

// overload de respaldo (si algún código viejo lo llama)
public void guardar(Integer idRegistro, Map<Integer, Boolean> marcas) {
  repo.guardarMarcas(null, idRegistro, marcas);
}

  

  public List<AsistenciaJdbcRepository.PdfRow> pdf(Integer idRegistro) { return repo.pdfRows(idRegistro); }
}
