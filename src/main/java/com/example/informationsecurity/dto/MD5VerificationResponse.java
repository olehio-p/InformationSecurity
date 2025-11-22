package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MD5VerificationResponse {
    private String fileName;
    private String expectedHash;
    private String actualHash;
    private boolean isValid;
    private long executionTimeMs;
}