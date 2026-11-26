package com.sandovalpurisaca.almuerzoservice.dto;

public record AuthResponse(
        String token,
        String tipo,
        long expiraEn,
        String nombre,
        String email,
        String rol
) {
}
