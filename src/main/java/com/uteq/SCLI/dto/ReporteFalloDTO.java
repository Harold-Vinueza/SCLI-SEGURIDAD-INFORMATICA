// com/uteq/SCLI/dto/ReporteFalloDTO.java
package com.uteq.SCLI.dto;

import com.uteq.SCLI.model.ReporteFallo;
import java.time.LocalDate;

public record ReporteFalloDTO(
        Integer id,
        String descripcionFallo,
        LocalDate fechaReporte,
        String estadoReporte
) {
    public static ReporteFalloDTO from(ReporteFallo r){
    return new ReporteFalloDTO(
        r.getIdReporteFallo(),          // <- ahora sí existe
        r.getDescripcionFallo(),
        r.getFechaReporte(),
        r.getEstadoReporte()
    );
}

    private static Integer tryGetId(ReporteFallo r){
        try { return (Integer) r.getClass().getMethod("getIdReporteFallo").invoke(r); } catch (Exception ignore) {}
        try { return (Integer) r.getClass().getMethod("getIdReporte").invoke(r); }      catch (Exception ignore) {}
        try { return (Integer) r.getClass().getMethod("getId").invoke(r); }             catch (Exception ignore) {}
        return null;
    }
}
