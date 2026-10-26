package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.StoreMailProperties;
import br.com.fiap.marketbuddies.api.dtos.EmailDto;
import br.com.fiap.marketbuddies.api.entities.EmailEntity;
import br.com.fiap.marketbuddies.api.entities.OrderEntity;
import br.com.fiap.marketbuddies.api.entities.OrderItemEntity;
import br.com.fiap.marketbuddies.api.enums.StatusEmail;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.EmailRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int ERROR_MAX = 500;
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR);
    private static final String TEMPLATE = "email/order-confirmation";

    private final EmailRepository emailRepository;
    private final OrderRepository orderRepository;
    private final JavaMailSender mailSender;
    private final StoreMailProperties mailProperties;
    private final TemplateEngine templateEngine;

    /** Grava SENT ou ERROR em TB_EMAILS; a falha nunca é relançada. */
    public StatusEmail sendConfirmation(Long orderId) {
        OrderEntity order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new NotFoundException("Pedido " + orderId + " não encontrado"));

        EmailEntity email = new EmailEntity();
        email.setOrder(order);
        email.setEmailFrom(mailProperties.from());
        email.setEmailTo(order.getCustomer().getEmail());
        email.setSubject("MarketBuddies · Pagamento confirmado do pedido #" + order.getId());
        email.setSendDateEmail(LocalDateTime.now());
        String orderUrl = orderUrl(order);
        String paidAt = DATE.format(order.getRespondedAt() != null ? order.getRespondedAt() : email.getSendDateEmail());
        email.setText(body(order, paidAt, orderUrl));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(email.getEmailFrom());
            helper.setTo(email.getEmailTo());
            helper.setSubject(email.getSubject());
            helper.setText(email.getText(), html(order, paidAt, orderUrl));
            mailSender.send(message);
            email.setStatusEmail(StatusEmail.SENT);
            log.info("E-mail do pedido {} enviado para {}", orderId, email.getEmailTo());
        } catch (MailException | MessagingException ex) {
            email.setStatusEmail(StatusEmail.ERROR);
            email.setErrorMessage(truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            log.warn("E-mail do pedido {} não enviado: {}", orderId, ex.getMessage(), ex);
        }

        emailRepository.save(email);
        return email.getStatusEmail();
    }

    @Transactional(readOnly = true)
    public List<EmailDto> findRecent() {
        return emailRepository.findTop50ByOrderByIdDesc().stream().map(EmailDto::from).toList();
    }

    public record EmailItem(String name, Integer quantity, String unitPrice, String subtotal) {
    }

    private String html(OrderEntity order, String paidAt, String orderUrl) {
        List<EmailItem> items = order.getItems().stream()
                .map(item -> new EmailItem(item.getProductName(), item.getQuantity(),
                        money(item.getUnitPrice()), money(subtotal(item))))
                .toList();
        Context context = new Context(PT_BR);
        context.setVariable("customerName", order.getCustomer().getFullName());
        context.setVariable("orderId", order.getId());
        context.setVariable("items", items);
        context.setVariable("subtotal", money(itemsTotal(order)));
        context.setVariable("shipping", shippingText(order));
        context.setVariable("total", money(order.getTotalAmount()));
        context.setVariable("authorizationCode", order.getAuthorizationCode());
        context.setVariable("paidAt", paidAt);
        context.setVariable("orderUrl", orderUrl);
        return templateEngine.process(TEMPLATE, context);
    }

    private String orderUrl(OrderEntity order) {
        String baseUrl = mailProperties.baseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/orders/" + order.getId();
    }

    private static BigDecimal itemsTotal(OrderEntity order) {
        return order.getItems().stream().map(EmailService::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** "grátis", o valor, ou "—" em pedido anterior à entrega. */
    private static String shippingText(OrderEntity order) {
        BigDecimal fee = order.getShippingFee();
        if (fee == null) {
            return "—";
        }
        return fee.signum() == 0 ? "grátis" : money(fee);
    }

    private static BigDecimal subtotal(OrderItemEntity item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private static String body(OrderEntity order, String paidAt, String orderUrl) {
        StringBuilder text = new StringBuilder()
                .append("Olá, ").append(order.getCustomer().getFullName()).append("!\n\n")
                .append("O Tranquilo Bank confirmou o pagamento do seu pedido #").append(order.getId()).append(".\n\n")
                .append("Itens:\n");
        for (OrderItemEntity item : order.getItems()) {
            BigDecimal subtotal = subtotal(item);
            text.append("  ").append(item.getQuantity()).append(" × ").append(item.getProductName())
                    .append(" (").append(money(item.getUnitPrice())).append(" cada) = ").append(money(subtotal))
                    .append('\n');
        }
        return text.append("\nSubtotal: ").append(money(itemsTotal(order))).append('\n')
                .append("Frete: ").append(shippingText(order)).append('\n')
                .append("Total: ").append(money(order.getTotalAmount())).append('\n')
                .append("Código de autorização: ").append(order.getAuthorizationCode()).append('\n')
                .append("Pago em: ").append(paidAt).append("\n\n")
                .append("Ver meu pedido: ").append(orderUrl).append("\n\n")
                .append("Obrigado por comprar no MarketBuddies!\n")
                .toString();
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    private static String truncate(String message) {
        return message.length() <= ERROR_MAX ? message : message.substring(0, ERROR_MAX - 1) + "…";
    }
}
