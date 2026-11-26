package com.sandovalpurisaca.almuerzoservice.controller;

import com.sandovalpurisaca.almuerzoservice.dto.AlmuerzoRequest;
import com.sandovalpurisaca.almuerzoservice.dto.AlmuerzoResponse;
import com.sandovalpurisaca.almuerzoservice.dto.ResumenDiaResponse;
import com.sandovalpurisaca.almuerzoservice.service.AlmuerzoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/almuerzos")
public class AlmuerzoController {

    private final AlmuerzoService almuerzoService;

    public AlmuerzoController(AlmuerzoService almuerzoService) {
        this.almuerzoService = almuerzoService;
    }

    @GetMapping
    public List<AlmuerzoResponse> listar(@AuthenticationPrincipal UserDetails usuario) {
        return almuerzoService.listar(usuario.getUsername());
    }

    @GetMapping("/resumen")
    public ResumenDiaResponse resumen(@AuthenticationPrincipal UserDetails usuario) {
        return almuerzoService.resumenDelDia(usuario.getUsername());
    }

    @GetMapping("/{id}")
    public AlmuerzoResponse obtener(@AuthenticationPrincipal UserDetails usuario,
                                    @PathVariable Long id) {
        return almuerzoService.obtener(usuario.getUsername(), id);
    }

    @PostMapping
    public ResponseEntity<AlmuerzoResponse> registrar(@AuthenticationPrincipal UserDetails usuario,
                                                      @Valid @RequestBody AlmuerzoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(almuerzoService.registrar(usuario.getUsername(), peticion));
    }

    @PutMapping("/{id}")
    public AlmuerzoResponse actualizar(@AuthenticationPrincipal UserDetails usuario,
                                       @PathVariable Long id,
                                       @Valid @RequestBody AlmuerzoRequest peticion) {
        return almuerzoService.actualizar(usuario.getUsername(), id, peticion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UserDetails usuario,
                                         @PathVariable Long id) {
        almuerzoService.eliminar(usuario.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
