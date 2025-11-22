package com.example.informationsecurity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RandomGeneratorRequest {
    @NotNull(message = "Modulus cannot be null")
    @Min(value = 1, message = "Modulus must be greater than 0")
    private Long modulus;

    @NotNull(message = "Multiplier cannot be null")
    @Min(value = 0, message = "Multiplier cannot be negative")
    private Long multiplier;

    @NotNull(message = "Increment cannot be null")
    @Min(value = 0, message = "Increment cannot be negative")
    private Long increment;

    @NotNull(message = "Seed cannot be null")
    @Min(value = 0, message = "Seed cannot be negative")
    private Long seed;

    @NotNull(message = "Number of values cannot be null")
    @Min(value = 1, message = "Number of values must be greater than 0")
    private Integer count;
}