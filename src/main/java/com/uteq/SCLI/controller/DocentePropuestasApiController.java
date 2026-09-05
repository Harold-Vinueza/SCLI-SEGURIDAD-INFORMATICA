package com.uteq.SCLI.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/docentes/solicitudes")
@RequiredArgsConstructor
public class DocentePropuestasApiController {

    @PersistenceContext
    private final EntityManager em;

    // === Ver detalle de propuesta de UNA solicitud (lab + horario + mensaje)
    @GetMapping("/{idSolicitud}/propuesta")
    public ResponseEntity<?> verPropuesta(@PathVariable("idSolicitud") Long idSolicitud){
        String q = """
           SELECT sp.id_laboratorio_propuesto, l.nombre_laboratorio,
                  sp.id_horario_propuesto, h.dia_semana, h.hora_inicio, h.hora_fin,
                  COALESCE(sp.mensaje_admin,'')
             FROM public.solicitud_propuesta sp
             JOIN public.laboratorio l ON l.id_laboratorio = sp.id_laboratorio_propuesto
             JOIN public.horario     h ON h.id_horario     = sp.id_horario_propuesto
            WHERE sp.id_solicitud = :id
        """;
        var row = (Object[]) em.createNativeQuery(q)
                .setParameter("id", idSolicitud)
                .getSingleResult();

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("idLaboratorio", ((Number)row[0]).intValue());
        out.put("laboratorio", String.valueOf(row[1]));
        out.put("idHorario", ((Number)row[2]).intValue());
        out.put("diaSemana", String.valueOf(row[3]));
        out.put("horaInicio", row[4]);
        out.put("horaFin", row[5]);
        out.put("mensaje", String.valueOf(row[6]));
        return ResponseEntity.ok(out);
    }

    // === Aceptar la propuesta: volver a 'Pendiente' para que el admin apruebe
    @PostMapping("/{idSolicitud}/aceptar-propuesta")
    @Transactional
    public ResponseEntity<?> aceptar(@PathVariable("idSolicitud") Long idSolicitud){
        // Mantén la propuesta guardada; el admin la aprobará en bloque
        em.createNativeQuery("""
            UPDATE public.solicitudasignacion
               SET estado = 'Pendiente'
             WHERE id_solicitud = :id
        """).setParameter("id", idSolicitud).executeUpdate();
        return ResponseEntity.ok().build();
    }

    // === Rechazar la propuesta: marcar la solicitud como 'Rechazada' y eliminar la propuesta
    @PostMapping("/{idSolicitud}/rechazar-propuesta")
    @Transactional
    public ResponseEntity<?> rechazar(@PathVariable("idSolicitud") Long idSolicitud){
        em.createNativeQuery("""
            UPDATE public.solicitudasignacion
               SET estado = 'Rechazada'
             WHERE id_solicitud = :id
        """).setParameter("id", idSolicitud).executeUpdate();

        em.createNativeQuery("""
            DELETE FROM public.solicitud_propuesta
             WHERE id_solicitud = :id
        """).setParameter("id", idSolicitud).executeUpdate();

        return ResponseEntity.ok().build();
    }
}