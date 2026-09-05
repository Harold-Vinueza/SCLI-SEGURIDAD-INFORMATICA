package com.uteq.SCLI.controller;

import com.uteq.SCLI.model.Equipo;
import com.uteq.SCLI.model.Laboratorio;
import com.uteq.SCLI.repository.LaboratorioRepository;
import com.uteq.SCLI.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/admin/equipos")
public class EquipoController {

   @Autowired private EquipoService equipoService;
    @Autowired private LaboratorioRepository laboratorioRepository;

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Integer labId,
                         @RequestParam(required = false) String estado,
                         Model model) {

        model.addAttribute("equipos", equipoService.listar(q, labId, estado));
        model.addAttribute("labs",   equipoService.laboratorios());
        model.addAttribute("q", q);
        model.addAttribute("labId", labId);
        model.addAttribute("estado", estado);

        model.addAttribute("equipoForm", new Equipo());
        return "dashboard/equipos";
    }

    @PostMapping("/crear")
    public String crear(@ModelAttribute("equipoForm") @Valid Equipo equipo,
                        BindingResult br,
                        RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos. Revisa los campos.");
            return "redirect:/dashboard/admin/equipos";
        }

        // Resolver laboratorio por ID
        if (equipo.getLaboratorio() != null && equipo.getLaboratorio().getIdLaboratorio() != null) {
            Laboratorio lab = laboratorioRepository
                    .findById(equipo.getLaboratorio().getIdLaboratorio())
                    .orElse(null);
            equipo.setLaboratorio(lab);
        }

        try {
            equipoService.guardar(equipo);
            ra.addFlashAttribute("ok", "Equipo creado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/equipos";
    }

    @PostMapping("/editar/{id}")
    public String editar(@PathVariable Integer id,
                         @ModelAttribute("equipoForm") @Valid Equipo equipo,
                         BindingResult br,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos. Revisa los campos.");
            return "redirect:/dashboard/admin/equipos";
        }
        Equipo existente = equipoService.buscarPorId(id);
        if (existente == null) {
            ra.addFlashAttribute("error", "El equipo no existe.");
            return "redirect:/dashboard/admin/equipos";
        }

        // Resolver laboratorio por ID
        Laboratorio lab = null;
        if (equipo.getLaboratorio() != null && equipo.getLaboratorio().getIdLaboratorio() != null) {
            lab = laboratorioRepository.findById(equipo.getLaboratorio().getIdLaboratorio()).orElse(null);
        }

        existente.setCodigoEquipo(equipo.getCodigoEquipo());
        existente.setTipoEquipo(equipo.getTipoEquipo());
        existente.setMarca(equipo.getMarca());
        existente.setModelo(equipo.getModelo());
        existente.setEstado(equipo.getEstado());
        existente.setLaboratorio(lab);

        try {
            equipoService.guardar(existente);
            ra.addFlashAttribute("ok", "Equipo actualizado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/equipos";
    }

   @PostMapping("/eliminar/{id}")
public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
    try {
        equipoService.eliminar(id);
        ra.addFlashAttribute("ok", "Equipo eliminado.");
    } catch (IllegalStateException e) {
        ra.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
        ra.addFlashAttribute("error", "No se pudo eliminar el equipo.");
    }
    return "redirect:/dashboard/admin/equipos";
}


    @GetMapping("/editar/{id}")
public String editarVista(@PathVariable Integer id,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) Integer labId,
                          @RequestParam(required = false) String estado,
                          Model model,
                          RedirectAttributes ra) {
    var equipo = equipoService.buscarPorId(id);
    if (equipo == null) {
        ra.addFlashAttribute("error", "El equipo no existe.");
        return "redirect:/dashboard/admin/equipos";
    }

    // Listas y filtros (para que la vista tenga todo)
    model.addAttribute("equipos", equipoService.listar(q, labId, estado));
    model.addAttribute("labs",   equipoService.laboratorios());
    model.addAttribute("q", q);
    model.addAttribute("labId", labId);
    model.addAttribute("estado", estado);

    // Modo edición
    model.addAttribute("equipoForm", equipo);
    model.addAttribute("editMode", true);
    model.addAttribute("editId", id);

    return "dashboard/equipos";
}

    @GetMapping("/vue")
    public String vistaVue() {
        return "dashboard/equipos-vue";
    }

}