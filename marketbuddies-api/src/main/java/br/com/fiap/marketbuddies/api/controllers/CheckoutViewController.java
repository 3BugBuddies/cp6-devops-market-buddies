package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.AddressDto;
import br.com.fiap.marketbuddies.api.dtos.CartDto;
import br.com.fiap.marketbuddies.api.dtos.OrderDto;
import br.com.fiap.marketbuddies.api.enums.OrderOrigin;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.exceptions.OutOfStockException;
import br.com.fiap.marketbuddies.api.facades.PaymentFacade;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.services.AddressService;
import br.com.fiap.marketbuddies.api.services.CartService;
import br.com.fiap.marketbuddies.api.services.ShippingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutViewController {

    private final CartService cartService;
    private final PaymentFacade paymentFacade;
    private final AddressService addressService;
    private final ShippingCalculator shippingCalculator;

    /** Sem {@code addressId}, marca o endereço mais recente; fora de zona, mostra o aviso e não deixa pagar. */
    @GetMapping
    public String checkout(@AuthenticationPrincipal AuthUser user,
                           @RequestParam(required = false) Long addressId,
                           Model model, RedirectAttributes redirect) {
        CartDto cart = cartService.view();
        if (cart.isEmpty()) {
            redirect.addFlashAttribute("notices", cart.notices());
            return "redirect:/cart";
        }

        List<AddressDto> addresses = addressService.list(user.getId());
        AddressDto selected = addresses.stream()
                .filter(a -> a.id().equals(addressId))
                .findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.getFirst());

        ShippingCalculator.Quote quote = null;
        if (selected != null) {
            try {
                quote = shippingCalculator.quote(selected.cep(), cart.total());
            } catch (BusinessException ex) {
                model.addAttribute("shippingError", ex.getMessage());
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("customerEmail", user.getUsername());
        model.addAttribute("addresses", addresses);
        model.addAttribute("selectedAddressId", selected == null ? null : selected.id());
        model.addAttribute("quote", quote);
        model.addAttribute("orderTotal", quote == null ? null : cart.total().add(quote.fee()));
        model.addAttribute("freeShippingFrom", ShippingCalculator.FREE_SHIPPING_FROM);
        return "checkout/checkout";
    }

    @PostMapping
    public String pay(@AuthenticationPrincipal AuthUser user,
                      @RequestParam(required = false) Long addressId,
                      @RequestParam(required = false) BigDecimal expectedTotal,
                      RedirectAttributes redirect) {
        CartDto cart = cartService.view();
        if (cart.isEmpty()) {
            redirect.addFlashAttribute("notices", cart.notices());
            return "redirect:/cart";
        }
        if (!cart.notices().isEmpty()) {
            redirect.addFlashAttribute("notices", cart.notices());
            return "redirect:/checkout";
        }
        if (addressId == null) {
            redirect.addFlashAttribute("error", "Escolha o endereço de entrega.");
            return "redirect:/checkout";
        }
        String back = "redirect:/checkout?addressId=" + addressId;

        OrderDto order;
        try {
            AddressDto address = addressService.get(addressId, user.getId());
            BigDecimal total = cart.total().add(shippingCalculator.quote(address.cep(), cart.total()).fee());
            if (expectedTotal == null || expectedTotal.compareTo(total) != 0) {
                redirect.addFlashAttribute("error", "Os preços mudaram, confira o total antes de pagar.");
                return back;
            }
            order = paymentFacade.requestPayment(user.getId(), addressId, cartService.toOrderItems(cart), OrderOrigin.WEB);
        } catch (OutOfStockException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return back;
        }

        cartService.clear();
        return "redirect:/orders/" + order.id();
    }
}
