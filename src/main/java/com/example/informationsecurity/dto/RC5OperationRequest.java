// src/main/java/com/example/informationsecurity/dto/RC5OperationRequest.java

package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RC5OperationRequest {
    @Min(value = 64, message = "Word size (w) must be 64 bits")
    @Max(value = 64, message = "Word size (w) must be 64 bits")
    private int w;

    @Min(value = 1, message = "Rounds (r) must be at least 1")
    private int r;

    @Min(value = 8, message = "Key length (b) must be at least 8 bytes")
    @Max(value = 32, message = "Key length (b) must not exceed 32 bytes (256 bits)")
    private int b;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    private String saveName;
    private String saveDirectory;
}