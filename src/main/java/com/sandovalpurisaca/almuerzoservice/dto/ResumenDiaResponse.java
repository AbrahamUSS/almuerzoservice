package com.sandovalpurisaca.almuerzoservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenDiaResponse(
        LocalDate fecha,
        long cantidadAlmuerzos,
        BigDecimal montoTotal
) {
}
