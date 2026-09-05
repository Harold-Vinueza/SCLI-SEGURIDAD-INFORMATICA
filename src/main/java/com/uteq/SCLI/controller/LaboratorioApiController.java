package com.uteq.SCLI.controller;

import com.uteq.SCLI.dto.LaboratorioDTO;
import com.uteq.SCLI.dto.PisoDTO;
import com.uteq.SCLI.model.Laboratorio;
import com.uteq.SCLI.model.Piso;
import com.uteq.SCLI.service.LaboratorioService;
import com.uteq.SCLI.service.PisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LaboratorioApiController {

    private final LaboratorioService laboratorioService;
    private final PisoService pisoService;

    // ===== LISTADO + BÚSQUEDA (usa tu LaboratorioService.listar) =====
    @GetMapping("/dashboard/admin/laboratorios")
    public String listado(@RequestParam(name = "q", required = false) String q,
                          @PageableDefault(size = 10) Pageable pageable,
                          Model model) {

        Page<Laboratorio> page = laboratorioService.listar(
                q,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        List<Piso> pisos = pisoService.listarTodos(); // usa tu PisoService

        model.addAttribute("labsPage", page);
        model.addAttribute("pisos", pisos);
        model.addAttribute("query", q);
        return "dashboard/admin/laboratorios";
    }

    // ===== LABORATORIOS =====
    @PostMapping("/admin/laboratorios")
    public String crearLab(@ModelAttribute LaboratorioDTO dto,
                           @RequestParam(value = "piso.idPiso", required = false) Integer pisoId,
                           RedirectAttributes ra) {
        if (pisoId != null) dto.setIdPiso(pisoId); // tu service espera dto.idPiso
        laboratorioService.crear(dto);
        ra.addFlashAttribute("msg", "Laboratorio creado correctamente");
        return "redirect:/dashboard/admin/laboratorios";
    }

    @PutMapping("/admin/laboratorios/{id}")
    public String actualizarLab(@PathVariable Integer id,
                                @ModelAttribute LaboratorioDTO dto,
                                @RequestParam(value = "piso.idPiso", required = false) Integer pisoId,
                                RedirectAttributes ra) {
        // valida existencia (opcional, pero útil para 404 claros)
        try { laboratorioService.obtener(id); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Laboratorio no encontrado"); }

        if (pisoId != null) dto.setIdPiso(pisoId);
        laboratorioService.actualizar(id, dto);
        ra.addFlashAttribute("msg", "Laboratorio actualizado");
        return "redirect:/dashboard/admin/laboratorios";
    }

    @DeleteMapping("/admin/laboratorios/{id}")
    public String eliminarLab(@PathVariable Integer id, RedirectAttributes ra) {
        laboratorioService.eliminar(id);
        ra.addFlashAttribute("msg", "Laboratorio eliminado");
        return "redirect:/dashboard/admin/laboratorios";
    }

    // ===== PISOS (mantengo las firmas que mostrastes para PisoService) =====
    @PostMapping("/admin/pisos")
    public String crearPiso(@ModelAttribute PisoDTO dto, RedirectAttributes ra) {
        Piso p = new Piso();
        p.setNumeroPiso(dto.getNumeroPiso());
        p.setDescripcion(dto.getDescripcion());
        pisoService.guardar(p);
        ra.addFlashAttribute("msg", "Piso creado");
        return "redirect:/dashboard/admin/laboratorios#pisos";
    }

    @PutMapping("/admin/pisos/{id}")
    public String actualizarPiso(@PathVariable Integer id,
                                 @ModelAttribute PisoDTO dto,
                                 RedirectAttributes ra) {
        Piso p = pisoService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Piso no encontrado"));
        p.setNumeroPiso(dto.getNumeroPiso());
        p.setDescripcion(dto.getDescripcion());
        pisoService.guardar(p);
        ra.addFlashAttribute("msg", "Piso actualizado");
        return "redirect:/dashboard/admin/laboratorios#pisos";
    }

    @DeleteMapping("/admin/pisos/{id}")
    public String eliminarPiso(@PathVariable Integer id, RedirectAttributes ra) {
        pisoService.eliminar(id);
        ra.addFlashAttribute("msg", "Piso eliminado");
        return "redirect:/dashboard/admin/laboratorios#pisos";
    }
}
