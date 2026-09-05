// src/main/java/com/uteq/SCLI/service/CoordinadorService.java
package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.UserSession;
import com.uteq.SCLI.repository.CoordinadorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.uteq.SCLI.dto.coordinador.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoordinadorService {

  private final CoordinadorRepository repo;
  private final UserSession userSession;
  @PersistenceContext private final EntityManager em;

  // Catálogos
  public List<Object[]> materiasPorCarrera(Integer idCarrera) {
    return repo.materiasPorCarrera(idCarrera);
  }

  public List<Object[]> docentesPorMateria(Integer idMateria) {
    return repo.docentesPorMateria(idMateria);
  }

  // ===== LISTAR =====
  public List<SolicitudItemDTO> listarSolicitudes(String estado) {
    ensureCoordinador();
    String filtro = (estado == null || estado.isBlank()) ? null : estado.trim();
    var rows = repo.listarSolicitudes(userSession.getIdPersona(), filtro);

    List<SolicitudItemDTO> out = new ArrayList<>();
    for (Object[] r : rows) {
      var x = new SolicitudItemDTO();
      x.idSolicitud = ((Number) r[0]).intValue();
      x.fecha       = ((java.sql.Date) r[1]).toLocalDate();
      x.estado      = (String) r[2];
      x.carrera     = (String) r[3];
      x.items       = ((Number) r[4]).intValue();
      x.observaciones = (String) r[5];
      out.add(x);
    }
    return out;
  }

  public List<Object[]> carrerasDelCoordinador(Integer idPersona) {
    return repo.carrerasMias(idPersona);
  }

  public List<Object[]> horariosPorJornada(String jornada) {
    return repo.horariosPorJornada(jornada);
  }

  // ===== DETALLES =====
  public List<SolicitudDetalleDTO> detallesSolicitud(Integer idSolicitud) {
    ensureCoordinador();
    if (!repo.esPropia(idSolicitud, userSession.getIdPersona()))
      throw new IllegalStateException("Solicitud no pertenece a este coordinador");

    var rows = repo.detallesSolicitud(idSolicitud);
    List<SolicitudDetalleDTO> out = new ArrayList<>();
    for (Object[] r : rows) {
      var d = new SolicitudDetalleDTO();
      d.idDetalle  = ((Number) r[0]).intValue();
      d.idHorario  = ((Number) r[1]).intValue();
      d.materia    = (String) r[2];
      d.jornada    = (String) r[3];
      d.diaSemana  = (String) r[4];
      d.horaInicio = (String) r[5];
      d.horaFin    = (String) r[6];
      out.add(d);
    }
    return out;
  }

  // ===== ANULAR =====
  @Transactional
  public boolean anular(Integer idSolicitud) {
    ensureCoordinador();
    if (!repo.esPropia(idSolicitud, userSession.getIdPersona()))
      throw new IllegalStateException("No autorizado para anular esta solicitud");

    int n = repo.anularSolicitud(idSolicitud);
    return n == 1;
  }

  // ===== Helper =====
  private void ensureCoordinador() {
    if (userSession == null || userSession.getNombreRol() == null ||
        !userSession.getNombreRol().equalsIgnoreCase("coordinador")) {
      throw new IllegalStateException("Rol no autorizado");
    }
  }

  // ===== CREAR SOLICITUD (con validación por período activo) =====
// ===== CREAR SOLICITUD (ocupación solo informativa; tope 6 h se mantiene) =====
@Transactional
public SolicitudCreadaDTO crearSolicitud(NuevaSolicitudDTO in) {
  // Seguridad básica
  if (userSession == null || userSession.getNombreRol() == null ||
      !userSession.getNombreRol().equalsIgnoreCase("coordinador")) {
    throw new IllegalStateException("Rol no autorizado para crear solicitudes de coordinación");
  }
  if (in == null || in.idCarrera == null || in.jornada == null) {
    throw new IllegalArgumentException("Faltan datos obligatorios");
  }

  final Integer periodoId = repo.periodoActivoId(); // puede ser null
  final Integer idSolicitud = repo.crearSolicitud(in.idCarrera, in.observaciones);

  var out = new SolicitudCreadaDTO();
  out.idSolicitud = idSolicitud;
  out.estado = "Pendiente";
  out.items = new ArrayList<>();

  if (in.celdas == null || in.celdas.isEmpty()) return out;

  // Control de tope 6h por materia (servidor)
  Map<Integer, Integer> horasPorMateria = new HashMap<>();

  for (var c : in.celdas) {
    var item = new SolicitudCreadaDTO.ItemResultado();
    item.idHorario = c.idHorario;

    // Texto visible de materia (+docente si viene)
    String materiaTxt = repo.materiaTexto(c.idMateria);
    if (c.idDocente != null) {
      String docente = repo.docenteTexto(c.idDocente);
      if (docente != null && !docente.isBlank()) materiaTxt += " (" + docente + ")";
    }
    item.materiaTexto = materiaTxt;

    // 1) Tope 6 horas por materia (bloqueante)
    int usadas = horasPorMateria.getOrDefault(c.idMateria, 0);
    if (usadas >= 6) {
      item.conflictivo = true;
      item.motivo = "Excede las 6 horas por materia";
      out.items.add(item);
      continue; // NO insertamos
    }

    // 2) Ocupación del slot en el período activo (solo informativo)
    boolean ocupado = (periodoId != null) && repo.slotOcupadoEnPeriodo(c.idHorario, periodoId);
    if (ocupado) {
      item.conflictivo = true;
      item.motivo = "El horario está ocupado en el período activo (se envía igual para revisión)";
      // IMPORTANTE: igual insertamos el detalle
    } else {
      item.conflictivo = false;
      item.motivo = "OK";
    }

    // Insertar detalle SIEMPRE (salvo que haya excedido las 6h por materia)
    repo.insertarDetalle(idSolicitud, c.idHorario, materiaTxt);
    horasPorMateria.put(c.idMateria, usadas + 1);
    out.items.add(item);
  }

  return out;
}


public List<int[]> ocupacionJornada(String jornada) {
  Integer p = repo.periodoActivoId();
  if (p == null) return java.util.Collections.emptyList();
  var rows = repo.ocupacionPorJornada(jornada, p);
  List<int[]> out = new ArrayList<>();
  for (Object[] r : rows) {
    out.add(new int[]{ ((Number)r[0]).intValue() }); // solo id_horario; la UI marca como ocupado
  }
  return out;
}

// ==== NUEVO: Laboratorios disponibles mapeados para el controller ====
public List<Map<String, Object>> labsDisponiblesMap(Integer idCarrera, Integer idHorario) {
  // Si no hay período activo igual devolvemos disponibles (sin cruce por período)
  Integer periodo = repo.periodoActivoId();
  if (periodo == null) periodo = -1;

  // Proyección de la query (interface LabDisp del repository)
  List<com.uteq.SCLI.repository.CoordinadorRepository.LabDisp> rows =
      repo.labsDisponibles(idCarrera, idHorario, periodo);

  List<Map<String, Object>> out = new ArrayList<>();
  for (com.uteq.SCLI.repository.CoordinadorRepository.LabDisp r : rows) {
    Map<String, Object> m = new HashMap<>();
    m.put("id",        r.getId());
    m.put("codigo",    r.getCodigo());
    m.put("nombre",    r.getNombre());
    m.put("preferido", r.getPreferido());
    out.add(m);
  }
  return out;
}

// === Labs por carrera mapeados a List<Map<..>> para el combo ===
public List<Map<String, Object>> labsPorCarreraMap(Integer idCarrera) {
  var rows = repo.labsPorCarrera(idCarrera);
  List<Map<String, Object>> out = new ArrayList<>();
  for (Object[] r : rows) {
    Map<String, Object> m = new HashMap<>();
    m.put("id",        ((Number) r[0]).intValue());
    m.put("codigo",     r[1]);          // String
    m.put("nombre",     r[2]);          // String
    m.put("preferido",  r[3]);          // Boolean (puede venir como Short/Integer -> lo dejamos tal cual)
    out.add(m);
  }
  return out;
}

// === Ocupación por laboratorio + jornada: devuelve [[idHorario], ...] ===
public List<int[]> ocupacionPorLaboratorio(String jornada, Integer idLaboratorio) {
  // normalizamos jornada; si viene vacío, la dejamos null para no filtrar
  String j = (jornada == null || jornada.isBlank()) ? null : jornada.trim();
  Integer periodoId = repo.periodoActivoId();
  if (periodoId == null) {
    // sin período activo no marcamos ocupación
    return List.of();
  }
  var ids = repo.ocupadosPorLaboratorio(idLaboratorio, periodoId, j);
  List<int[]> out = new ArrayList<>();
  for (Integer id : ids) {
    if (id != null) out.add(new int[]{ id });
  }
  return out;
}


}
