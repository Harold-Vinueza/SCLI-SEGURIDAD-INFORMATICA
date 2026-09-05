package com.uteq.SCLI.service;

import com.uteq.SCLI.dto.AprobacionDTO;
import com.uteq.SCLI.dto.AprobacionItem;
import com.uteq.SCLI.repository.CoordinadorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCoordService {

  private final CoordinadorRepository repo;

  // No la marcamos como final, ni la usamos en el @RequiredArgsConstructor
  @PersistenceContext
  private EntityManager em;

  // =================== Listado principal ===================
  public List<Map<String, Object>> listar(String estado) {
  // Normaliza el filtro: "" -> null (para que pase el WHERE)
  String filtro = (estado == null || estado.trim().isEmpty()) ? null : estado.trim();

  var rows = repo.listarSolicitudesAdmin(filtro);
  var out  = new ArrayList<Map<String, Object>>();

  for (Object[] r : rows) {
    Map<String, Object> m = new HashMap<>();
    m.put("id",     ((Number) r[0]).intValue());       // id_solicitud
    m.put("fecha",  String.valueOf(r[1]));             // fecha_solicitud
    m.put("estado", (String) r[2]);                    // estado_solicitud
    m.put("carrera",(String) r[3]);                    // nombre_carrera
    m.put("coord",  r[4] == null ? "" : String.valueOf(r[4])); // coordinador
    m.put("items",  ((Number) r[5]).intValue());       // items
    m.put("obs",    (String) r[6]);                    // observaciones
    m.put("fAsig",  r[7] == null ? null : String.valueOf(r[7])); // fecha asignación
    out.add(m);
  }
  return out;
}

  // =========== Labs disponibles para un horario ============
  @Transactional(readOnly = true)
public List<Map<String, Object>> labsDisponibles(Integer idSolicitud, Integer idHorario) {
  Integer idCarrera = repo.carreraDeSolicitud(idSolicitud);
  Integer idPeriodo = repo.periodoActivoId();

  // ⚠️ Importante: NO bloquear el horario completo si ya hay alguna asignación.
  // Queremos ver qué laboratorios siguen libres para este mismo horario.

  List<CoordinadorRepository.LabDisp> rows =
      repo.labsDisponibles(idCarrera, idHorario, idPeriodo);

  List<Map<String, Object>> out = new ArrayList<>();
  for (CoordinadorRepository.LabDisp r : rows) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", r.getId());
    m.put("codigo", r.getCodigo());
    m.put("nombre", r.getNombre());
    m.put("preferido", Boolean.TRUE.equals(r.getPreferido()));
    out.add(m);
  }
  return out;
}

  // ================== Detalle de la solicitud ==================
  @Transactional(readOnly = true)
  public List<Map<String, Object>> detalles(Integer idSolicitud) {
    var rows = repo.detallesSolicitud(idSolicitud);
    var out = new ArrayList<Map<String, Object>>();
    for (Object[] r : rows) {
      Map<String, Object> m = new HashMap<>();
      m.put("idDetalle", ((Number) r[0]).intValue());
      m.put("idHorario", ((Number) r[1]).intValue());
      m.put("materia", (String) r[2]);
      m.put("jornada", (String) r[3]);
      m.put("dia", (String) r[4]);
      m.put("hi", (String) r[5]);
      m.put("hf", (String) r[6]);
      out.add(m);
    }
    return out;
  }

  // ===================== Aprobar solicitud =====================
@Transactional
public Map<String, Object> aprobar(Integer idSolicitud, AprobacionDTO dto) {
  Integer periodoId = repo.periodoActivoId();
  if (periodoId == null) throw new IllegalStateException("No hay período activo.");

  // 1) prioridad por horario (si viene)
  Map<Integer, Integer> labPorHorario = Optional.ofNullable(dto.getLabs())
      .orElse(List.of())
      .stream()
      .filter(it -> it.getIdHorario() != null && it.getIdLaboratorio() != null)
      .collect(Collectors.toMap(
          AprobacionItem::getIdHorario,
          AprobacionItem::getIdLaboratorio,
          (a, b) -> a
      ));

  // 2) prioridad por materia (desde la barra superior)
  Map<String, Integer> labPorMateria = Optional.ofNullable(dto.getLabsMateria())
      .orElseGet(HashMap::new);

  var det      = repo.detallesSolicitud(idSolicitud); // [idDet, idHor, materia, jornada, dia, hi, hf]
  var sinLab   = new ArrayList<Integer>();            // idHorario sin asignación
  var idCarrera= repo.carreraDeSolicitud(idSolicitud);

  // Para construir el detalle legible al coordinador
  List<String> aprobadas    = new ArrayList<>();
  List<String> noAsignadas  = new ArrayList<>();

  for (Object[] d : det) {
    Integer idHor  = ((Number) d[1]).intValue();
    String  materia= (String) d[2];
    String  dia    = (String) d[4];
    String  hi     = (String) d[5];
    String  hf     = (String) d[6];

    Integer idDoc = null; // (opcional, hoy no lo usamos aquí)

    // Laboratorios libres para ESTE horario en el período activo
    var disponibles = repo.labsDisponibles(idCarrera, idHor, periodoId);

    Integer elegido = labPorHorario.get(idHor); // prioridad 1

    // Si vino elegido por horario, validar que esté libre para este slot
    if (elegido != null) {
      boolean ok = false;
      for (var ld : disponibles) {
        if (Objects.equals(ld.getId(), elegido)) { ok = true; break; }
      }
      if (!ok) elegido = null;
    }

    // prioridad 2: por materia (si todos los bloques de esa materia usan el mismo lab)
    if (elegido == null && labPorMateria.containsKey(materia)) {
      Integer candidato = labPorMateria.get(materia);
      boolean ok = false;
      for (var ld : disponibles) {
        if (Objects.equals(ld.getId(), candidato)) { ok = true; break; }
      }
      if (ok) elegido = candidato;
    }

    // prioridad 3: cualquier disponible (respetando preferidos primero según la query)
    if (elegido == null) {
      elegido = disponibles.isEmpty() ? null : disponibles.get(0).getId();
    }

    if (elegido == null) {
      // No se pudo asignar este bloque
      sinLab.add(idHor);
      noAsignadas.add(String.format("- [%s %s-%s] %s — sin laboratorio (ocupado en todos)", dia, hi, hf, materia));
      continue;
    }

    // Insertar asignación y registrar la línea aprobada con nombre del laboratorio
    repo.insertarAsignacionConLaboratorio(elegido, idHor, materia, idDoc, periodoId);
    String labTxt = repo.labTexto(elegido); // "LAB01 - Laboratorio X"
    aprobadas.add(String.format("- [%s %s-%s] %s — Lab: %s", dia, hi, hf, materia, labTxt));
  }

  // === Construir observación explicativa para el coordinador ===
  StringBuilder detalle = new StringBuilder();
  if (!aprobadas.isEmpty()) {
    detalle.append("Horas aprobadas (con laboratorio):\n");
    aprobadas.forEach(l -> detalle.append(l).append('\n'));
  }
  if (!noAsignadas.isEmpty()) {
    if (!aprobadas.isEmpty()) detalle.append('\n');
    detalle.append("Horas no asignadas (choque/ocupado):\n");
    noAsignadas.forEach(l -> detalle.append(l).append('\n'));
  }

  // Extra anterior (contador simple), lo mantenemos como cierre
  String extra = sinLab.isEmpty()
      ? null
      : "No se pudo asignar " + sinLab.size() + " horario(s) porque ya están ocupados en todos los laboratorios.";

  // Observaciones finales = (obs ingresada) + detalle por bloques + (resumen extra)
  List<String> partes = new ArrayList<>();
  if (dto.getObservaciones() != null && !dto.getObservaciones().isBlank()) partes.add(dto.getObservaciones().trim());
  if (detalle.length() > 0) partes.add(detalle.toString().trim());
  if (extra != null) partes.add(extra);

  String obsFinal = String.join("\n\n", partes);

  // Estado final: Aprobada si todo asignado, caso contrario Propuesta (para que el coord. responda)
  repo.actualizarEstado(idSolicitud, sinLab.isEmpty() ? "Aprobada" : "Propuesta", obsFinal);

  Map<String, Object> resp = new HashMap<>();
  resp.put("ok", true);
  resp.put("parcial", !sinLab.isEmpty());
  resp.put("sinLaboratorio", sinLab);
  return resp;
}


  // ======================= Rechazar =======================
  @Transactional
  public void rechazar(Integer idSolicitud, String motivo) {
    repo.actualizarEstado(idSolicitud, "Rechazada", motivo == null ? "" : motivo.trim());
  }

  // ======================= Proponer =======================
  @Transactional
  public Map<String, Object> proponer(Integer idSolicitud, String obs, List<Map<String, Object>> celdas) {
    repo.limpiarDetalles(idSolicitud);

    for (Map<String, Object> c : celdas) {
      Integer idHorario = Integer.parseInt(String.valueOf(c.get("idHorario")));
      Integer idMateria = Integer.parseInt(String.valueOf(c.get("idMateria")));
      Integer idDocente = (c.get("idDocente") == null || String.valueOf(c.get("idDocente")).isBlank())
          ? null
          : Integer.parseInt(String.valueOf(c.get("idDocente")));

      String mat = repo.materiaTexto(idMateria);
      String doc = (idDocente == null ? null : repo.docenteTexto(idDocente));
      String label = (doc == null ? mat : mat + " (" + doc + ")");
      repo.insertarDetalleAdmin(idSolicitud, idHorario, label);
    }

    repo.actualizarEstado(idSolicitud, "Propuesta", obs == null ? "" : obs.trim());
    return Map.of("ok", true, "idSolicitud", idSolicitud, "items", celdas.size());
  }
}
