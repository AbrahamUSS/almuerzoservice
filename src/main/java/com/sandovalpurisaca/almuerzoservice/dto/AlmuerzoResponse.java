package com.sandovalpurisaca.almuerzoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlmuerzoResponse(
        Long id,
        String cliente,
        String plato,
        Integer cantidad,
        BigDecimal precio,
        BigDecimal total,
        LocalDate fechaPedido,
        Boolean entregado
) {
}
