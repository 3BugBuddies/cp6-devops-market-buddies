package br.com.fiap.marketbuddies.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryProgressJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${marketbuddies.delivery.check-ms:5000}",
            initialDelayString = "${marketbuddies.delivery.check-ms:5000}")
    public void advanceDeliveries() {
        orderService.advanceDeliveries();
    }
}
