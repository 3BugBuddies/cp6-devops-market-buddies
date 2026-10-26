package br.com.fiap.marketbuddies.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Conta do cliente ({@code PUT /api/users/me} e tela "Minha conta"); o admin envia só nome e e-mail. */
public record UserFormDto(
        @NotBlank(message = "Informe seu nome completo")
        @Size(max = 120, message = "O nome pode ter até 120 caracteres")
        String fullName,

        @NotBlank(message = "Informe seu e-mail")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Informe um e-mail válido, como nome@dominio.com")
        @Size(max = 160, message = "O e-mail pode ter até 160 caracteres")
        String email,

        String currentPassword,

        /** Opcional: vazio mantém a senha atual (o form envia "" quando o campo fica em branco). */
        @Pattern(regexp = "^(.{6,72})?$", message = "A senha precisa ter de 6 a 72 caracteres")
        String newPassword,

        String confirmPassword) {
}
