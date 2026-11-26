package com.sandovalpurisaca.almuerzoservice.service;

import com.sandovalpurisaca.almuerzoservice.dto.AuthResponse;
import com.sandovalpurisaca.almuerzoservice.dto.LoginRequest;
import com.sandovalpurisaca.almuerzoservice.dto.RegistroRequest;
import com.sandovalpurisaca.almuerzoservice.entity.Usuario;
import com.sandovalpurisaca.almuerzoservice.exception.EmailDuplicadoException;
import com.sandovalpurisaca.almuerzoservice.exception.NoAutorizadoException;
import com.sandovalpurisaca.almuerzoservice.exception.RecursoNoEncontradoException;
import com.sandovalpurisaca.almuerzoservice.repository.UsuarioRepository;
import com.sandovalpurisaca.almuerzoservice.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest peticion) {
        if (usuarioRepository.existsByEmail(peticion.email())) {
            throw new EmailDuplicadoException("Ya existe una cuenta con el email " + peticion.email());
        }
        Usuario usuario = new Usuario(
                peticion.nombre(),
                peticion.email(),
                passwordEncoder.encode(peticion.password()),
                "USER");
        return respuestaConToken(usuarioRepository.save(usuario));
    }

    public AuthResponse iniciarSesion(LoginRequest peticion) {
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(peticion.email(), peticion.password()));
        } catch (AuthenticationException ex) {
            throw new NoAutorizadoException("Email o contraseña incorrectos");
        }
        Usuario usuario = usuarioRepository.findByEmail(autenticacion.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return respuestaConToken(usuario);
    }

    private AuthResponse respuestaConToken(Usuario usuario) {
        String token = jwtService.generarToken(usuario);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.expiracionMillis(token),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol());
    }
}
