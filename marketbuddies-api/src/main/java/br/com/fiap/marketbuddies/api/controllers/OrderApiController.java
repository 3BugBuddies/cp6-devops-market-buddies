package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.OrderDto;
import br.com.fiap.marketbuddies.api.dtos.OrderRequestDto;
import br.com.fiap.marketbuddies.api.enums.OrderOrigin;
import br.com.fiap.marketbuddies.api.facades.PaymentFacade;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final PaymentFacade paymentFacade;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderRequestDto request,
                                           @AuthenticationPrincipal AuthUser user) {
        OrderDto order = paymentFacade.requestPayment(user.getId(), request.addressId(), request.items(), OrderOrigin.API);
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.id()))
                .body(order);
    }

    @GetMapping
    public List<OrderDto> list(@AuthenticationPrincipal AuthUser user) {
        return orderService.listOrders(user.getId(), isAdmin(user));
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return orderService.getOrder(id, user.getId(), isAdmin(user));
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return orderService.cancel(id, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(AuthUser user) {
        return user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
