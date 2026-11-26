package com.sandovalpurisaca.almuerzoservice.security;

import com.sandovalpurisaca.almuerzoservice.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;
    private final int longitudFirma;

    public JwtService(@Value("${security.jwt.secret}") String secreto,
                      @Value("${security.jwt.expiration-ms}") long expiracionMs) {
        byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracionMs = expiracionMs;
        // Base64url sin padding: un HS256 mide 43 caracteres, HS384 64, HS512 86.
        this.longitudFirma = (clave.getEncoded().length * 8 + 5) / 6;
    }

    public String generarToken(Usuario usuario) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expiracionMs);
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol())
                .claim("nombre", usuario.getNombre())
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(clave)
                .compact();
    }

    public String extraerEmail(String token) {
        return parsear(token).getSubject();
    }

    public boolean esValido(String token, UserDetails userDetails) {
        Claims claims = parsear(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    public long expiracionMillis(String token) {
        return parsear(token).getExpiration().getTime();
    }

    private Claims parsear(String token) {
        // Formato estricto: tres segmentos base64url y firma de longitud exacta.
        // Así se rechazan tokens con caracteres añadidos/recortados que algunos
        // decodificadores base64 ignoran silenciosamente.
        String[] partes = token == null ? new String[0] : token.split("\\.", -1);
        if (partes.length != 3
                || partes[0].isEmpty()
                || partes[1].isEmpty()
                || partes[2].length() != longitudFirma
                || !partes[0].matches("[A-Za-z0-9_-]+")
                || !partes[1].matches("[A-Za-z0-9_-]+")
                || !partes[2].matches("[A-Za-z0-9_-]+")) {
            throw new MalformedJwtException("Token JWT con formato inválido");
        }
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
