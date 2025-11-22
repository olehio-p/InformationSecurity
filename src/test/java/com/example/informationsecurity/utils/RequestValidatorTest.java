package com.example.informationsecurity.utils;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.ComparisonTestRequest;
import com.example.informationsecurity.dto.RandomGeneratorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RequestValidatorTest {

    @InjectMocks
    private RequestValidator validator;

    
    private static final long MODULUS = 100L;
    private static final long VALID_VALUE = 50L;
    private static final long INVALID_VALUE_EQ_MOD = 100L;
    private static final long INVALID_VALUE_GT_MOD = 101L;
    private static final long INVALID_VALUE_NEGATIVE = -1L;
    private static final long VALID_ZERO = 0L;

    @BeforeEach
    void setUp() {
        
    }

    @Test
    void testValidateRandomGeneratorRequest_Valid() {
        RandomGeneratorRequest request = RandomGeneratorRequest.builder()
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(VALID_VALUE)
                .seed(VALID_VALUE)
                .build();
        assertDoesNotThrow(() -> validator.validateRandomGeneratorRequest(request));
    }

    @Test
    void testValidateRandomGeneratorRequest_MultiplierEqualsModulus() {
        RandomGeneratorRequest request = RandomGeneratorRequest.builder()
                .modulus(MODULUS)
                .multiplier(INVALID_VALUE_EQ_MOD)
                .increment(VALID_VALUE)
                .seed(VALID_VALUE)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateRandomGeneratorRequest(request),
                "Multiplier must be less than modulus");
    }

    @Test
    void testValidateRandomGeneratorRequest_IncrementGreaterThanModulus() {
        RandomGeneratorRequest request = RandomGeneratorRequest.builder()
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(INVALID_VALUE_GT_MOD)
                .seed(VALID_VALUE)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateRandomGeneratorRequest(request),
                "Increment must be less than modulus");
    }

    @Test
    void testValidateRandomGeneratorRequest_SeedGreaterThanModulus() {
        RandomGeneratorRequest request = RandomGeneratorRequest.builder()
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(VALID_VALUE)
                .seed(INVALID_VALUE_GT_MOD)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateRandomGeneratorRequest(request),
                "Seed must be less than modulus");
    }

    @Test
    void testValidateCesaroTestRequest_SystemRandom_NoValidation() {
        
        CesaroTestRequest request = CesaroTestRequest.builder()
                .useSystemRandom(true)
                .modulus(null) 
                .build();
        assertDoesNotThrow(() -> validator.validateCesaroTestRequest(request));
    }

    @Test
    void testValidateCesaroTestRequest_LCG_Valid() {
        CesaroTestRequest request = CesaroTestRequest.builder()
                .useSystemRandom(false)
                .pairsCount(100)
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(VALID_VALUE)
                .seed(VALID_VALUE)
                .build();
        assertDoesNotThrow(() -> validator.validateCesaroTestRequest(request));
    }

    @Test
    void testValidateCesaroTestRequest_LCG_ModulusZero() {
        CesaroTestRequest request = CesaroTestRequest.builder()
                .useSystemRandom(false)
                .modulus(VALID_ZERO)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateCesaroTestRequest(request),
                "Modulus must be positive");
    }

    @Test
    void testValidateCesaroTestRequest_LCG_MultiplierNegative() {
        CesaroTestRequest request = CesaroTestRequest.builder()
                .useSystemRandom(false)
                .modulus(MODULUS)
                .multiplier(INVALID_VALUE_NEGATIVE)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateCesaroTestRequest(request),
                "Multiplier must be non-negative");
    }

    @Test
    void testValidateCesaroTestRequest_LCG_IncrementEqualsModulus() {
        CesaroTestRequest request = CesaroTestRequest.builder()
                .useSystemRandom(false)
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(INVALID_VALUE_EQ_MOD)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateCesaroTestRequest(request),
                "Increment must be in range [0, modulus)");
    }

    @Test
    void testValidateCesaroTestRequest_LCG_SeedGreaterThanModulus() {
        CesaroTestRequest request = CesaroTestRequest.builder()
                .useSystemRandom(false)
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(VALID_VALUE)
                .seed(INVALID_VALUE_GT_MOD)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateCesaroTestRequest(request),
                "Seed must be in range [0, modulus)");
    }

    @Test
    void testValidateComparisonTestRequest_Valid() {
        ComparisonTestRequest request = ComparisonTestRequest.builder()
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(VALID_VALUE)
                .seed(VALID_VALUE)
                .build();
        assertDoesNotThrow(() -> validator.validateComparisonTestRequest(request));
    }

    @Test
    void testValidateComparisonTestRequest_MultiplierEqualsModulus() {
        ComparisonTestRequest request = ComparisonTestRequest.builder()
                .modulus(MODULUS)
                .multiplier(INVALID_VALUE_EQ_MOD)
                .increment(VALID_VALUE)
                .seed(VALID_VALUE)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateComparisonTestRequest(request),
                "Multiplier must be less than modulus");
    }

    @Test
    void testValidateComparisonTestRequest_SeedGreaterThanModulus() {
        ComparisonTestRequest request = ComparisonTestRequest.builder()
                .modulus(MODULUS)
                .multiplier(VALID_VALUE)
                .increment(VALID_VALUE)
                .seed(INVALID_VALUE_GT_MOD)
                .build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateComparisonTestRequest(request),
                "Seed must be less than modulus");
    }
}