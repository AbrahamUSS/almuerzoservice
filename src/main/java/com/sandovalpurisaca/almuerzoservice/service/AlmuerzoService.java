package com.sandovalpurisaca.almuerzoservice.service;

import com.sandovalpurisaca.almuerzoservice.dto.AlmuerzoRequest;
import com.sandovalpurisaca.almuerzoservice.dto.AlmuerzoResponse;
import com.sandovalpurisaca.almuerzoservice.dto.ResumenDiaResponse;
import com.sandovalpurisaca.almuerzoservice.entity.Almuerzo;
import com.sandovalpurisaca.almuerzoservice.entity.Usuario;
import com.sandovalpurisaca.almuerzoservice.exception.RecursoNoEncontradoException;
import com.sandovalpurisaca.almuerzoservice.repository.AlmuerzoRepository;
import com.sandovalpurisaca.almuerzoservice.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class AlmuerzoService {

    private final AlmuerzoRepository almuerzoRepository;
    private final UsuarioRepository usuarioRepository;

    public AlmuerzoService(AlmuerzoRepository almuerzoRepository, UsuarioRepository usuarioRepository) {
        this.almuerzoRepository = almuerzoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AlmuerzoResponse> listar(String email) {
        return almuerzoRepository.findAllByUsuarioIdOrderByFechaPedidoDescIdDesc(usuarioDe(email).getId())
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlmuerzoResponse obtener(String email, Long id) {
        return aRespuesta(buscarDelUsuario(email, id));
    }

    @Transactional
    public AlmuerzoResponse registrar(String email, AlmuerzoRequest peticion) {
        Almuerzo almuerzo = new Almuerzo();
        aplicarDatos(almuerzo, peticion);
        almuerzo.setFechaPedido(LocalDate.now());
        almuerzo.setEntregado(Boolean.TRUE.equals(peticion.entregado()));
        almuerzo.setUsuario(usuarioDe(email));
        return aRespuesta(almuerzoRepository.save(almuerzo));
    }

    @Transactional
    public AlmuerzoResponse actualizar(String email, Long id, AlmuerzoRequest peticion) {
        Almuerzo almuerzo = buscarDelUsuario(email, id);
        aplicarDatos(almuerzo, peticion);
        if (peticion.entregado() != null) {
            almuerzo.setEntregado(peticion.entregado());
        }
        return aRespuesta(almuerzoRepository.save(almuerzo));
    }

    @Transactional
    public void eliminar(String email, Long id) {
        almuerzoRepository.delete(buscarDelUsuario(email, id));
    }

    @Transactional(readOnly = true)
    public ResumenDiaResponse resumenDelDia(String email) {
        LocalDate hoy = LocalDate.now();
        List<Almuerzo> pedidos = almuerzoRepository.findByUsuarioIdAndFechaPedido(usuarioDe(email).getId(), hoy);
        BigDecimal monto = pedidos.stream()
                .map(this::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new ResumenDiaResponse(hoy, pedidos.size(), monto);
    }

    private void aplicarDatos(Almuerzo almuerzo, AlmuerzoRequest peticion) {
        almuerzo.setCliente(peticion.cliente());
        almuerzo.setPlato(peticion.plato());
        almuerzo.setCantidad(peticion.cantidad());
        almuerzo.setPrecio(peticion.precio());
    }

    private Almuerzo buscarDelUsuario(String email, Long id) {
        return almuerzoRepository.findByIdAndUsuarioId(id, usuarioDe(email).getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    private Usuario usuarioDe(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + email));
    }

    private BigDecimal total(Almuerzo almuerzo) {
        return almuerzo.getPrecio()
                .multiply(BigDecimal.valueOf(almuerzo.getCantidad()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private AlmuerzoResponse aRespuesta(Almuerzo almuerzo) {
        return new AlmuerzoResponse(
                almuerzo.getId(),
                almuerzo.getCliente(),
                almuerzo.getPlato(),
                almuerzo.getCantidad(),
                almuerzo.getPrecio(),
                total(almuerzo),
                almuerzo.getFechaPedido(),
                Boolean.TRUE.equals(almuerzo.getEntregado()));
    }
}
