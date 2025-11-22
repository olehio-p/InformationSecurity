package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MD5FileHashResponse {
    private String fileName;
    private String hash;
    private long fileSizeBytes;
    private long executionTimeMs;
}