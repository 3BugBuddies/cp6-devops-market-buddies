package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.UserDto;
import br.com.fiap.marketbuddies.api.dtos.UserFormDto;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.security.JwtUtil;
import br.com.fiap.marketbuddies.api.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<UserDto> list() {
        return userService.listUsers();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.findProfile(id);
    }

    /** O JWT usa o e-mail como subject: a resposta traz um token novo para o e-mail atualizado. */
    @PutMapping("/me")
    public Map<String, Object> updateMe(@Valid @RequestBody UserFormDto request, @AuthenticationPrincipal AuthUser user) {
        UserEntity updated = userService.updateAccount(user.getId(), request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", UserDto.from(updated));
        body.put("accessToken", jwtUtil.generateToken(new AuthUser(updated)));
        body.put("tokenType", "Bearer");
        body.put("expiresIn", jwtUtil.getExpirationMs());
        return body;
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserFormDto request) {
        return userService.updateByAdmin(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        userService.deleteUser(id, admin.getId());
        return ResponseEntity.noContent().build();
    }
}
