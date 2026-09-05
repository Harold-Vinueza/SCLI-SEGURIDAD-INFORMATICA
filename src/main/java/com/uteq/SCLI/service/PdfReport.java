package com.uteq.SCLI.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.uteq.SCLI.repository.AsistenciaJdbcRepository;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfReport {

  public byte[] render(List<AsistenciaJdbcRepository.PdfRow> rows) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document doc = new Document();
    try {
      PdfWriter.getInstance(doc, out);
      doc.open();

      if (rows == null || rows.isEmpty()) {
        // PDF mínimo válido cuando no hay datos (evita error del visor)
        doc.add(new Paragraph("Registro de Asistencia\n\n"));
        doc.add(new Paragraph("No hay datos para mostrar.")); // mensaje amigable
        doc.add(new Paragraph("Fecha: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE)));
        return out.toByteArray();
      }

      var first = rows.get(0);

      String header = String.format(
          "Registro de Asistencia\n\nMateria: %s\nFecha: %s\nDocente: %s\nLaboratorio: %s\n\n",
          first.materia(),
          first.fecha().format(DateTimeFormatter.ISO_DATE),
          first.docente(),
          first.laboratorio() == null ? "-" : first.laboratorio()
      );
      doc.add(new Paragraph(header));

      PdfPTable table = new PdfPTable(3);
      table.setWidths(new float[]{8f, 72f, 20f});
      table.addCell("#");
      table.addCell("Estudiante");
      table.addCell("Presente");

      int i = 1;
      for (var r : rows) {
        table.addCell(Integer.toString(i++));
        table.addCell(r.estudiante());
        table.addCell(Boolean.TRUE.equals(r.presente()) ? "Sí" : "No");
      }
      doc.add(table);

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      doc.close();
    }
    return out.toByteArray();
  }
}