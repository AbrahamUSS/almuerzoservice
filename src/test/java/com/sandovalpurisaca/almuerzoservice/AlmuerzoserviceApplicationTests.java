package com.sandovalpurisaca.almuerzoservice;

import com.sandovalpurisaca.almuerzoservice.entity.Usuario;
import com.sandovalpurisaca.almuerzoservice.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmuerzoserviceApplicationTests {

    private final JwtService jwtService = new JwtService(
            "dev-secret-cambiar-en-produccion-almuerzoya-2026", 3_600_000L);

    @Test
    void tokenContieneEmailRolYExpiracion() {
        Usuario usuario = new Usuario("Ana", "ana@correo.com", "hash", "USER");
        String token = jwtService.generarToken(usuario);

        assertEquals("ana@correo.com", jwtService.extraerEmail(token));
        assertTrue(jwtService.expiracionMillis(token) > System.currentTimeMillis());
    }

    @Test
    void tokenAlteradoNoEsValido() {
        Usuario usuario = new Usuario("Ana", "ana@correo.com", "hash", "USER");
        String token = jwtService.generarToken(usuario) + "x";

        assertThrows(JwtException.class, () -> jwtService.extraerEmail(token));
    }

    @Test
    void passwordSeCifraConBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("almuerzo123");

        assertNotEquals("almuerzo123", hash);
        assertTrue(encoder.matches("almuerzo123", hash));
    }
}
