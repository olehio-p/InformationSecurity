package com.example.informationsecurity.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComparisonResult {
    private long rc5TimeMs;
    private long rsaTimeMs;
    private long fileSizeBytes;
    private String winner;
    private double rc5SpeedBps;
    private double rsaSpeedBps;
}