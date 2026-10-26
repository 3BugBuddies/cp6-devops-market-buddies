package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.AddressFormDto;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.services.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressViewController {

    private final AddressService addressService;

    @GetMapping
    public String list(@AuthenticationPrincipal AuthUser user, Model model) {
        model.addAttribute("addresses", addressService.list(user.getId()));
        return "addresses/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) String next, Model model) {
        return form(model, AddressFormDto.empty(), null, next);
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, @RequestParam(required = false) String next,
                           @AuthenticationPrincipal AuthUser user, Model model) {
        return form(model, addressService.findForm(id, user.getId()), id, next);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("addressForm") AddressFormDto form, BindingResult result,
                         @RequestParam(required = false) String next, @AuthenticationPrincipal AuthUser user,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return form(model, form, null, next);
        }
        try {
            addressService.create(user.getId(), form);
        } catch (FieldValidationException ex) {
            result.rejectValue(ex.getField(), "address." + ex.getField(), ex.getMessage());
            return form(model, form, null, next);
        }
        return saved(redirect, next);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("addressForm") AddressFormDto form,
                         BindingResult result, @RequestParam(required = false) String next,
                         @AuthenticationPrincipal AuthUser user, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return form(model, form, id, next);
        }
        try {
            addressService.update(id, user.getId(), form);
        } catch (FieldValidationException ex) {
            result.rejectValue(ex.getField(), "address." + ex.getField(), ex.getMessage());
            return form(model, form, id, next);
        }
        return saved(redirect, next);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user, RedirectAttributes redirect) {
        addressService.delete(id, user.getId());
        redirect.addFlashAttribute("success", "Endereço excluído");
        return "redirect:/addresses";
    }

    private static String saved(RedirectAttributes redirect, String next) {
        redirect.addFlashAttribute("success", "Endereço salvo");
        return AuthViewController.isInternalPath(next) ? "redirect:" + next : "redirect:/addresses";
    }

    private static String form(Model model, AddressFormDto form, Long id, String next) {
        model.addAttribute("addressForm", form);
        model.addAttribute("addressId", id);
        model.addAttribute("next", AuthViewController.isInternalPath(next) ? next : null);
        return "addresses/form";
    }
}
