package com.sandovalpurisaca.almuerzoservice.repository;

import com.sandovalpurisaca.almuerzoservice.entity.Almuerzo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlmuerzoRepository extends JpaRepository<Almuerzo, Long> {

    List<Almuerzo> findAllByUsuarioIdOrderByFechaPedidoDescIdDesc(Long usuarioId);

    Optional<Almuerzo> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Almuerzo> findByUsuarioIdAndFechaPedido(Long usuarioId, LocalDate fechaPedido);
}
