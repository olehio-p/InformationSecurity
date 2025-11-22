package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MD5HashResponse {
    private String input;
    private String hash;
    private long executionTimeMs;
}