package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public
class ComparisonTestResponse {
    private CesaroTestResponse lehmerTest;
    private CesaroTestResponse systemTest;
    private ComparisonMetrics comparison;
}
