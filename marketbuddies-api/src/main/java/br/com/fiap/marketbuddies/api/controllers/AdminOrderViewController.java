package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.OrderDto;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderViewController {

    private final OrderService orderService;

    /** Filtra por status os 50 pedidos mais recentes; status desconhecido mostra todos. */
    @GetMapping
    public String orders(@RequestParam(required = false) String status, Model model) {
        OrderStatus filter = Arrays.stream(OrderStatus.values())
                .filter(s -> s.name().equals(status))
                .findFirst()
                .orElse(null);
        List<OrderDto> orders = orderService.listOrders(null, true).stream()
                .filter(o -> filter == null || o.status() == filter)
                .toList();
        model.addAttribute("orders", orders);
        model.addAttribute("statusFilter", filter);
        return "admin/orders";
    }

    @GetMapping("/{id}")
    public String order(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrder(id, null, true));
        model.addAttribute("adminView", true);
        return "orders/order";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            orderService.deleteOrder(id);
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/orders/" + id;
        }
        redirect.addFlashAttribute("success", "Pedido #" + id + " excluído");
        return "redirect:/admin/orders";
    }
}
