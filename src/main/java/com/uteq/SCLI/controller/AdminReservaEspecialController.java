package com.uteq.SCLI.controller;

import com.uteq.SCLI.model.ReservaEspecial;
import com.uteq.SCLI.service.ReservaEspecialService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/reservas-especiales")
public class AdminReservaEspecialController {

    private final ReservaEspecialService service;

    public AdminReservaEspecialController(ReservaEspecialService service) {
        this.service = service;
    }

    @GetMapping
    public String vista(Model model) {
        model.addAttribute("nueva", new ReservaEspecial());
        model.addAttribute("reservas", service.listar());
        return "admin/reservas-especiales";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("nueva") ReservaEspecial r) {
        service.guardar(r);
        return "redirect:/admin/reservas-especiales";
    }

    @PostMapping("/editar")
    public String editar(@ModelAttribute ReservaEspecial r) {
        service.guardar(r); // trae id -> hace update
        return "redirect:/admin/reservas-especiales";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return "redirect:/admin/reservas-especiales";
    }

    @PostMapping("/publicar/{id}")
    public String publicar(@PathVariable Integer id) {
        service.togglePublicar(id);
        return "redirect:/admin/reservas-especiales";
    }
}
