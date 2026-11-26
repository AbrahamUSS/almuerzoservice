package com.sandovalpurisaca.almuerzoservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AlmuerzoRequest(

        @NotBlank(message = "El cliente es obligatorio")
        @Size(max = 100, message = "El cliente no puede superar 100 caracteres")
        String cliente,

        @NotBlank(message = "El plato es obligatorio")
        @Size(max = 100, message = "El plato no puede superar 100 caracteres")
        String plato,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        Integer cantidad,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        @Digits(integer = 6, fraction = 2, message = "El precio debe tener máximo 6 enteros y 2 decimales")
        BigDecimal precio,

        Boolean entregado
) {
}
