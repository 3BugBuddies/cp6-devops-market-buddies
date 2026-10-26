package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneFormDto;
import br.com.fiap.marketbuddies.api.exceptions.ConflictException;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.services.DeliveryZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/delivery-zones")
@RequiredArgsConstructor
public class AdminDeliveryZoneViewController {

    private static final String FORM = "zoneForm";

    private final DeliveryZoneService deliveryZoneService;

    @GetMapping
    public String list(Model model) {
        return page(model, DeliveryZoneFormDto.empty(), false);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute(FORM) DeliveryZoneFormDto form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        return save(null, form, result, model, redirect);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute(FORM) DeliveryZoneFormDto form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        return save(id, form, result, model, redirect);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        String name = deliveryZoneService.delete(id);
        redirect.addFlashAttribute("success", "Zona excluída: " + name);
        return "redirect:/admin/delivery-zones";
    }

    private String save(Long id, DeliveryZoneFormDto form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                if (id == null) {
                    deliveryZoneService.create(form);
                } else {
                    deliveryZoneService.update(id, form);
                }
                redirect.addFlashAttribute("success", "Zona salva");
                return "redirect:/admin/delivery-zones";
            } catch (FieldValidationException ex) {
                result.rejectValue(ex.getField(), "zone." + ex.getField(), ex.getMessage());
            } catch (ConflictException ex) {
                if (ex.getField() != null) {
                    result.rejectValue(ex.getField(), "zone.conflict", ex.getMessage());
                } else {
                    result.reject("zone.conflict", ex.getMessage());
                }
            }
        }
        return page(model, form.withId(id), result);
    }

    private String page(Model model, DeliveryZoneFormDto form, BindingResult result) {
        String page = page(model, form, true);
        BindingResult copy = new BeanPropertyBindingResult(form, FORM);
        copy.addAllErrors(result);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + FORM, copy);
        return page;
    }

    private String page(Model model, DeliveryZoneFormDto form, boolean modalOpen) {
        model.addAttribute("zones", deliveryZoneService.list());
        model.addAttribute(FORM, form);
        model.addAttribute("modalOpen", modalOpen);
        return "admin/delivery-zones";
    }
}
