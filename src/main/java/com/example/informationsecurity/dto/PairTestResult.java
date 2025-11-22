package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairTestResult {
    private Long number1;
    private Long number2;
    private Long gcd;
    private Boolean areCoprime;
}
