package com.example.informationsecurity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CesaroTestRequest {

    private Long modulus;
    private Long multiplier;
    private Long increment;
    private Long seed;

    @NotNull(message = "Number of pairs cannot be null")
    @Min(value = 1, message = "Number of pairs must be greater than 0")
    private Integer pairsCount;

    private Boolean useSystemRandom = false;
}