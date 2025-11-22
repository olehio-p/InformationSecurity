package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DSAKeyGenRequest {

    @Min(value = 1024, message = "Key size must be at least 1024 bits")
    @Max(value = 3072, message = "Key size must be at most 3072 bits")
    private int keySize = 2048;

    @NotBlank(message = "Key prefix cannot be empty")
    private String keyPrefix = "dsa_key";

    private String saveDirectory;
}