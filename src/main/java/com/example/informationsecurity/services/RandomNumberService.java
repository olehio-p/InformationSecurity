package com.example.informationsecurity.services;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.CesaroTestResponse;
import com.example.informationsecurity.dto.ComparisonMetrics;
import com.example.informationsecurity.dto.ComparisonTestRequest;
import com.example.informationsecurity.dto.ComparisonTestResponse;
import com.example.informationsecurity.dto.RandomGeneratorRequest;
import com.example.informationsecurity.dto.RandomGeneratorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class RandomNumberService {

    private final RandomNumberGenerator randomNumberGenerator;
    private final CesaroTestProcessor cesaroTestProcessor;
    private final FileStorageService fileStorageService;


    public RandomGeneratorResponse generateNumbers(RandomGeneratorRequest request) {
        validateRequest(request);
        List<Long> numbers = randomNumberGenerator.generateLehmerNumbers(
                request.getSeed(), request.getMultiplier(), request.getIncrement(),
                request.getModulus(), request.getCount());

        String filePath = fileStorageService.saveNumbersToFile(numbers);
        log.info("Generated {} numbers and saved to file {}", numbers.size(), filePath);

        return RandomGeneratorResponse.builder()
                .numbers(numbers)
                .filePath(filePath)
                .generatedCount(numbers.size())
                .message(String.format("Successfully generated %d numbers and saved to file %s",
                        numbers.size(), filePath))
                .build();
    }


    public CesaroTestResponse performCesaroTest(CesaroTestRequest request) {
        validateCesaroRequest(request);
        return cesaroTestProcessor.performCesaroTest(request);
    }


    public ComparisonTestResponse compareGenerators(ComparisonTestRequest request) {
        validateComparisonRequest(request);

        CesaroTestRequest lehmerRequest = CesaroTestRequest.builder()
                .modulus(request.getModulus())
                .multiplier(request.getMultiplier())
                .increment(request.getIncrement())
                .seed(request.getSeed())
                .pairsCount(request.getPairsCount())
                .useSystemRandom(false)
                .build();
        CesaroTestResponse lehmerTest = performCesaroTest(lehmerRequest);

        // Test system random
        CesaroTestRequest systemRequest = CesaroTestRequest.builder()
                .pairsCount(request.getPairsCount())
                .useSystemRandom(true)
                .build();
        CesaroTestResponse systemTest = performCesaroTest(systemRequest);

        // Compare results
        String betterGenerator = lehmerTest.getErrorPercentage() < systemTest.getErrorPercentage()
                ? "Lehmer Algorithm"
                : "System Random";
        double errorDifference = Math.abs(lehmerTest.getErrorPercentage() - systemTest.getErrorPercentage());

        log.info("Comparison completed. Better generator: {}", betterGenerator);

        return ComparisonTestResponse.builder()
                .lehmerTest(lehmerTest)
                .systemTest(systemTest)
                .comparison(ComparisonMetrics.builder()
                        .lehmerErrorPercentage(lehmerTest.getErrorPercentage())
                        .systemErrorPercentage(systemTest.getErrorPercentage())
                        .betterGenerator(betterGenerator)
                        .errorDifference(errorDifference)
                        .build())
                .build();
    }

    private void validateRequest(RandomGeneratorRequest request) {
        if (request.getCount() <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        if (request.getModulus() <= 0) {
            throw new IllegalArgumentException("Modulus must be positive");
        }
        if (request.getMultiplier() <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive");
        }
    }

    private void validateCesaroRequest(CesaroTestRequest request) {
        if (request.getPairsCount() <= 0) {
            throw new IllegalArgumentException("Pairs count must be positive");
        }
        if (Boolean.FALSE.equals(request.getUseSystemRandom())) {
            if (request.getModulus() <= 0) {
                throw new IllegalArgumentException("Modulus must be positive");
            }
            if (request.getMultiplier() <= 0) {
                throw new IllegalArgumentException("Multiplier must be positive");
            }
        }
    }

    private void validateComparisonRequest(ComparisonTestRequest request) {
        if (request.getPairsCount() <= 0) {
            throw new IllegalArgumentException("Pairs count must be positive");
        }
        if (request.getModulus() <= 0) {
            throw new IllegalArgumentException("Modulus must be positive");
        }
        if (request.getMultiplier() <= 0) {
            throw new IllegalArgumentException("Multiplier must be positive");
        }
    }
}