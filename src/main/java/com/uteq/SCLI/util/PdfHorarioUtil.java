// src/main/java/com/uteq/SCLI/util/PdfHorarioUtil.java
package com.uteq.SCLI.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.uteq.SCLI.service.HorarioDocenteService;
import org.springframework.stereotype.Component;

import java.awt.Color; // <- SOLO Color, nada de java.awt.*
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfHorarioUtil {

    // Usa el nombre totalmente calificado para evitar ambigüedad
    private static final com.lowagie.text.Font FONT_TITLE  =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
    private static final com.lowagie.text.Font FONT_HEADER =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, Color.BLACK);
    private static final com.lowagie.text.Font FONT_CELL   =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, Color.BLACK);

    private static final Color BG_HEADER = new Color(243, 244, 246);
    private static final Color BG_FILLED = new Color(240, 255, 244);
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public void writeHorarioPdf(OutputStream os,
                                HorarioDocenteService.Tabla tabla,
                                List<String> dias,
                                String titulo) throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(doc, os);
        doc.open();

        Paragraph p = new Paragraph(titulo != null ? titulo : "Horario Docente", FONT_TITLE);
        p.setSpacingAfter(10f);
        doc.add(p);

        PdfPTable t = new PdfPTable(dias.size() + 1);
        t.setWidthPercentage(100f);
        float[] widths = new float[dias.size() + 1];
        widths[0] = 2.2f;              // columna de franja
        for (int i = 1; i < widths.length; i++) widths[i] = 3.2f;
        t.setWidths(widths);

        // Cabeceras
        addHeaderCell(t, "Franja");
        for (String d : dias) addHeaderCell(t, d);

        // Filas
        for (var fila : tabla.filas()) {
            String franja = fila.inicio().format(HHMM) + " – " + fila.fin().format(HHMM);
            addCell(t, franja, null, false);

            for (String d : dias) {
                var c = fila.celdas().get(d);
                if (c != null && c.ocupado()) {
                    StringBuilder sb = new StringBuilder();
                    if (c.texto() != null && !c.texto().isBlank()) sb.append(c.texto()).append("\n");
                    if (c.laboratorio() != null && !c.laboratorio().isBlank()) sb.append(c.laboratorio()).append("\n");
                    if (c.jornada() != null && !c.jornada().isBlank()) sb.append(c.jornada());
                    addCell(t, sb.toString().trim(), BG_FILLED, true);
                } else {
                    addCell(t, "", null, false);
                }
            }
        }

        doc.add(t);
        doc.close();
    }

    private void addHeaderCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_HEADER));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(BG_HEADER);
        c.setPadding(6f);
        t.addCell(c);
    }

    private void addCell(PdfPTable t, String text, Color bg, boolean boldTopLine) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, FONT_CELL));
        c.setPadding(6f);
        c.setUseAscender(true);
        c.setUseDescender(true);
        c.setVerticalAlignment(Element.ALIGN_TOP);
        if (bg != null) c.setBackgroundColor(bg);
        if (boldTopLine) c.setBorderWidthTop(1.2f);
        t.addCell(c);
    }
}
