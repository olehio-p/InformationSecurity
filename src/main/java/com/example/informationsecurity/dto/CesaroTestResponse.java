package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CesaroTestResponse {
    private Double estimatedPi;
    private Double actualPi;
    private Double error;
    private Double errorPercentage;
    private Integer totalPairs;
    private Integer coprimeCount;
    private Double coprimeProbability;
    private String generatorType;
    private List<PairTestResult> samplePairs;
}
