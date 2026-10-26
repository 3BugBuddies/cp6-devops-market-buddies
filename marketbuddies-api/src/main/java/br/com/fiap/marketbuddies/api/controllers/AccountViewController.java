package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.UserFormDto;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.security.JwtAuthFilter;
import br.com.fiap.marketbuddies.api.security.JwtUtil;
import br.com.fiap.marketbuddies.api.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountViewController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public String form(@AuthenticationPrincipal AuthUser user, Model model) {
        var profile = userService.findProfile(user.getId());
        model.addAttribute("accountForm", new UserFormDto(profile.fullName(), profile.email(), null, null, null));
        return "account/account";
    }

    /** Regrava o cookie com o token novo (mesmo mecanismo do login), para a cliente continuar logada. */
    @PostMapping
    public String update(@Valid @ModelAttribute("accountForm") UserFormDto form, BindingResult result,
                         @AuthenticationPrincipal AuthUser user, HttpServletRequest request,
                         HttpServletResponse response, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "account/account";
        }
        UserEntity updated;
        try {
            updated = userService.updateAccount(user.getId(), form);
        } catch (FieldValidationException ex) {
            result.rejectValue(ex.getField(), "account." + ex.getField(), ex.getMessage());
            return "account/account";
        }
        String token = jwtUtil.generateToken(new AuthUser(updated));
        response.addHeader(HttpHeaders.SET_COOKIE,
                JwtAuthFilter.tokenCookie(token, jwtUtil.getExpirationMs(), request.isSecure()).toString());
        redirect.addFlashAttribute("success", "Conta atualizada");
        return "redirect:/account";
    }
}
