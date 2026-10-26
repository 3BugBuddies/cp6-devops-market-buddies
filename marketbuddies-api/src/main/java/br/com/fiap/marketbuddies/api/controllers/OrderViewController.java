package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.OrderDto;
import br.com.fiap.marketbuddies.api.dtos.RetryItemDto;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.services.CartService;
import br.com.fiap.marketbuddies.api.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderViewController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping
    public String orders(@AuthenticationPrincipal AuthUser user, Model model) {
        model.addAttribute("orders", orderService.listOrders(user.getId(), false));
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String order(@PathVariable Long id, @AuthenticationPrincipal AuthUser user, Model model) {
        OrderDto order = orderService.getOrder(id, user.getId(), false);
        model.addAttribute("order", order);
        return "orders/order";
    }

    @GetMapping("/{id}/status")
    @ResponseBody
    public Map<String, Object> status(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        OrderDto order = orderService.getOrder(id, user.getId(), false);
        Map<String, Object> body = new HashMap<>();
        body.put("status", order.status());
        body.put("statusEmail", order.statusEmail());
        body.put("deliveryStatus", order.delivery() == null ? null : order.delivery().status());
        return body;
    }

    @PostMapping("/{id}/retry")
    public String retry(@PathVariable Long id, @AuthenticationPrincipal AuthUser user, RedirectAttributes redirect) {
        List<RetryItemDto> items = orderService.retryItems(id, user.getId());
        List<String> warnings = cartService.refill(items);
        if (!warnings.isEmpty()) {
            redirect.addFlashAttribute("notices", warnings);
        }
        return "redirect:/checkout";
    }
}
