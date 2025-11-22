package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonMetrics {
    private Double lehmerErrorPercentage;
    private Double systemErrorPercentage;
    private String betterGenerator;
    private Double errorDifference;
}