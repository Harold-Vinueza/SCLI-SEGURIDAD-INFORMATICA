package com.uteq.SCLI.controller;

import com.uteq.SCLI.model.Equipo;
import com.uteq.SCLI.model.Mobiliario;
import com.uteq.SCLI.model.ReporteFalloForm;
import com.uteq.SCLI.service.EquipoService;
import com.uteq.SCLI.service.MobiliarioService;
import com.uteq.SCLI.service.ReporteFalloService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/admin/activos")
public class ActivosLaboratorioController {

    @Autowired private EquipoService equipoService;
    @Autowired private MobiliarioService mobiliarioService;
    @Autowired private ReporteFalloService reporteFalloService;

    // ---------- LISTA / PESTAÑAS ----------
    @GetMapping
    public String vista(@RequestParam(required = false) String tab, // "equipos" | "mobiliario"
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) Integer labId,
                        @RequestParam(required = false) String estado,
                        Model model) {

        String activeTab = (tab == null || tab.isBlank()) ? "equipos" : tab;

        model.addAttribute("labs", equipoService.laboratorios());
        model.addAttribute("tab", activeTab);

        // pestaña equipos
        model.addAttribute("equipos", equipoService.listar(q, labId, estado));
        model.addAttribute("equipoForm", new Equipo());
        model.addAttribute("q", q);
        model.addAttribute("labId", labId);
        model.addAttribute("estado", estado);

        // pestaña mobiliario
        model.addAttribute("mobiliarios", mobiliarioService.listar(null, labId, null));
        model.addAttribute("mobiliarioForm", new Mobiliario());

        // para el modal de reporte
        model.addAttribute("reporteFalloForm", new ReporteFalloForm());

        return "dashboard/activos";
    }

    // ====== EQUIPOS: abrir vista en modo edición ======
    @GetMapping("/equipos/editar-view/{id}")
    public String editarEquipoView(@PathVariable Integer id,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) Integer labId,
                                   @RequestParam(required = false) String estado,
                                   Model model, RedirectAttributes ra) {
        Equipo e = equipoService.buscarPorId(id);
        if (e == null) {
            ra.addFlashAttribute("error", "El equipo no existe.");
            return "redirect:/dashboard/admin/activos?tab=equipos";
        }

        // listas y filtros
        model.addAttribute("labs", equipoService.laboratorios());
        model.addAttribute("equipos", equipoService.listar(q, labId, estado));
        model.addAttribute("q", q);
        model.addAttribute("labId", labId);
        model.addAttribute("estado", estado);

        // modo edición
        model.addAttribute("equipoForm", e);
        model.addAttribute("editMode", true);
        model.addAttribute("editId", id);
        model.addAttribute("tab", "equipos");

        // para que la pestaña mobiliario también tenga datos/objetos
        model.addAttribute("mobiliarios", mobiliarioService.listar(null, labId, null));
        model.addAttribute("mobiliarioForm", new Mobiliario());
        model.addAttribute("reporteFalloForm", new ReporteFalloForm());

        return "dashboard/activos";
    }

    // ---------- CRUD EQUIPOS ----------
    @PostMapping("/equipos/crear")
    public String crearEquipo(@ModelAttribute("equipoForm") @Valid Equipo equipo,
                              BindingResult br,
                              RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos (equipo).");
            return "redirect:/dashboard/admin/activos?tab=equipos";
        }
        try {
            equipoService.guardarResolviendoLaboratorio(equipo);
            ra.addFlashAttribute("ok", "Equipo creado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=equipos";
    }

    @PostMapping("/equipos/editar/{id}")
    public String editarEquipo(@PathVariable Integer id,
                               @ModelAttribute("equipoForm") @Valid Equipo equipo,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos (equipo).");
            return "redirect:/dashboard/admin/activos?tab=equipos";
        }
        try {
            equipoService.editar(id, equipo);
            ra.addFlashAttribute("ok", "Equipo actualizado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=equipos";
    }

    @PostMapping("/equipos/eliminar/{id}")
    public String eliminarEquipo(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            equipoService.eliminar(id);
            ra.addFlashAttribute("ok", "Equipo eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=equipos";
    }

    // ---------- REPORTE DE FALLO (por equipo, via modal) ----------
    @PostMapping("/equipos/{idEquipo}/reportes")
    public String crearReporte(@PathVariable Integer idEquipo,
                               @ModelAttribute("reporteFalloForm") @Valid ReporteFalloForm form,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors() || form.getDescripcionFallo() == null || form.getDescripcionFallo().isBlank()) {
            ra.addFlashAttribute("error", "Describe el fallo.");
            return "redirect:/dashboard/admin/activos?tab=equipos";
        }
        try {
            reporteFalloService.crear(idEquipo, form.getDescripcionFallo(), form.getIdDocente(), form.getIdAdminPiso());
            ra.addFlashAttribute("ok", "Reporte de fallo registrado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=equipos";
    }

    // ====== MOBILIARIO: abrir vista en modo edición ======
    @GetMapping("/mobiliario/editar-view/{id}")
    public String editarMobiliarioView(@PathVariable Integer id,
                                       @RequestParam(required = false) Integer labId,
                                       Model model, RedirectAttributes ra) {
        Mobiliario m = mobiliarioService.buscarPorId(id);
        if (m == null) {
            ra.addFlashAttribute("error", "El mobiliario no existe.");
            return "redirect:/dashboard/admin/activos?tab=mobiliario";
        }

        model.addAttribute("labs", equipoService.laboratorios());
        model.addAttribute("mobiliarios", mobiliarioService.listar(null, labId, null));
        model.addAttribute("mobiliarioForm", m);
        model.addAttribute("mEditMode", true);
        model.addAttribute("mEditId", id);

        // para que la pestaña equipos no rompa
        model.addAttribute("equipos", equipoService.listar(null, labId, null));
        model.addAttribute("equipoForm", new Equipo());
        model.addAttribute("reporteFalloForm", new ReporteFalloForm());

        model.addAttribute("tab", "mobiliario");
        return "dashboard/activos";
    }

    // ---------- CRUD MOBILIARIO ----------
    @PostMapping("/mobiliario/crear")
    public String crearMobiliario(@ModelAttribute("mobiliarioForm") @Valid Mobiliario mob,
                                  BindingResult br,
                                  RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos (mobiliario).");
            return "redirect:/dashboard/admin/activos?tab=mobiliario";
        }
        try {
            mobiliarioService.guardarResolviendoLaboratorio(mob);
            ra.addFlashAttribute("ok", "Mobiliario creado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=mobiliario";
    }

    @PostMapping("/mobiliario/editar/{id}")
    public String editarMobiliario(@PathVariable Integer id,
                                   @ModelAttribute("mobiliarioForm") @Valid Mobiliario mob,
                                   BindingResult br,
                                   RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos (mobiliario).");
            return "redirect:/dashboard/admin/activos?tab=mobiliario";
        }
        try {
            mobiliarioService.editar(id, mob);
            ra.addFlashAttribute("ok", "Mobiliario actualizado.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=mobiliario";
    }

    @PostMapping("/mobiliario/eliminar/{id}")
    public String eliminarMobiliario(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            mobiliarioService.eliminar(id);
            ra.addFlashAttribute("ok", "Mobiliario eliminado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/admin/activos?tab=mobiliario";
    }

    @GetMapping("/equipos/{id}/reportes.json")
@ResponseBody
public java.util.List<com.uteq.SCLI.dto.ReporteFalloDTO> reportesEquipo(@PathVariable Integer id){
    return reporteFalloService.listarPorEquipo(id)
            .stream()
            .map(com.uteq.SCLI.dto.ReporteFalloDTO::from)
            .toList();
}


}
