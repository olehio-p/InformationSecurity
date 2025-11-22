package com.example.informationsecurity.utils;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.ComparisonTestRequest;
import com.example.informationsecurity.dto.RandomGeneratorRequest;
import org.springframework.stereotype.Component;


@Component
public class RequestValidator {

    public void validateRandomGeneratorRequest(RandomGeneratorRequest request) {
        if (request.getMultiplier() >= request.getModulus()) {
            throw new IllegalArgumentException("Multiplier must be less than modulus");
        }
        if (request.getIncrement() >= request.getModulus()) {
            throw new IllegalArgumentException("Increment must be less than modulus");
        }
        if (request.getSeed() >= request.getModulus()) {
            throw new IllegalArgumentException("Seed must be less than modulus");
        }
    }


    public void validateCesaroTestRequest(CesaroTestRequest request) {
        if (!request.getUseSystemRandom()) {
            if (request.getModulus() == null || request.getModulus() <= 0) {
                throw new IllegalArgumentException("Modulus must be positive");
            }
            if (request.getMultiplier() == null || request.getMultiplier() < 0 ||
                    request.getMultiplier() >= request.getModulus()) {
                throw new IllegalArgumentException("Multiplier must be in range [0, modulus)");
            }
            if (request.getIncrement() == null || request.getIncrement() < 0 ||
                    request.getIncrement() >= request.getModulus()) {
                throw new IllegalArgumentException("Increment must be in range [0, modulus)");
            }
            if (request.getSeed() == null || request.getSeed() < 0 ||
                    request.getSeed() >= request.getModulus()) {
                throw new IllegalArgumentException("Seed must be in range [0, modulus)");
            }
        }
    }


    public void validateComparisonTestRequest(ComparisonTestRequest request) {
        if (request.getMultiplier() >= request.getModulus()) {
            throw new IllegalArgumentException("Multiplier must be less than modulus");
        }
        if (request.getIncrement() >= request.getModulus()) {
            throw new IllegalArgumentException("Increment must be less than modulus");
        }
        if (request.getSeed() >= request.getModulus()) {
            throw new IllegalArgumentException("Seed must be less than modulus");
        }
    }
}
