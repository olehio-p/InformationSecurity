package com.example.informationsecurity.services;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.CesaroTestResponse;
import com.example.informationsecurity.dto.PairTestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
public class CesaroTestProcessor {

    private final RandomNumberGenerator numberGenerator;
    private final GcdCalculator gcdCalculator;

    public CesaroTestResponse performCesaroTest(CesaroTestRequest request) {
        List<Long> numbers = Boolean.TRUE.equals(request.getUseSystemRandom())
                ? numberGenerator.generateSystemRandomNumbers(request.getPairsCount() * 2)
                : numberGenerator.generateLehmerNumbers(
                request.getSeed(), request.getMultiplier(), request.getIncrement(),
                request.getModulus(), request.getPairsCount() * 2);

        int coprimeCount = 0;
        List<PairTestResult> samplePairs = new ArrayList<>();
        boolean collectSamples = request.getPairsCount() <= 100;

        // Check pairs for coprimality
        for (int i = 0; i < request.getPairsCount(); i++) {
            long num1 = numbers.get(i * 2);
            long num2 = numbers.get(i * 2 + 1);
            long gcd = gcdCalculator.calculateGCD(num1, num2);
            boolean areCoprime = gcd == 1;

            if (areCoprime) {
                coprimeCount++;
            }

            if (collectSamples && i < 10) {
                samplePairs.add(PairTestResult.builder()
                        .number1(num1)
                        .number2(num2)
                        .gcd(gcd)
                        .areCoprime(areCoprime)
                        .build());
            }
        }

        double coprimeProbability = (double) coprimeCount / request.getPairsCount();
        double estimatedPi = coprimeProbability > 0 ? Math.sqrt(6.0 / coprimeProbability) : 0;
        double actualPi = Math.PI;
        double error = Math.abs(estimatedPi - actualPi);
        double errorPercentage = (error / actualPi) * 100;

        return CesaroTestResponse.builder()
                .estimatedPi(estimatedPi)
                .actualPi(actualPi)
                .error(error)
                .errorPercentage(errorPercentage)
                .totalPairs(request.getPairsCount())
                .coprimeCount(coprimeCount)
                .coprimeProbability(coprimeProbability)
                .generatorType(Boolean.TRUE.equals(request.getUseSystemRandom()) ? "System Random" : "Lehmer Algorithm")
                .samplePairs(samplePairs)
                .build();
    }
}