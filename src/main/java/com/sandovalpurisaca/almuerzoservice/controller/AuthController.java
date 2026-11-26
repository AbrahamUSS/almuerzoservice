package com.sandovalpurisaca.almuerzoservice.controller;

import com.sandovalpurisaca.almuerzoservice.dto.AuthResponse;
import com.sandovalpurisaca.almuerzoservice.dto.LoginRequest;
import com.sandovalpurisaca.almuerzoservice.dto.RegistroRequest;
import com.sandovalpurisaca.almuerzoservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest peticion) {
        return ResponseEntity.ok(authService.registrar(peticion));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> iniciarSesion(@Valid @RequestBody LoginRequest peticion) {
        return ResponseEntity.ok(authService.iniciarSesion(peticion));
    }
}
