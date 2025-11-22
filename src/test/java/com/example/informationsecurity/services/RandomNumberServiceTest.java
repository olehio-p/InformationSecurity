package com.example.informationsecurity.services;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.CesaroTestResponse;
import com.example.informationsecurity.dto.ComparisonMetrics;
import com.example.informationsecurity.dto.ComparisonTestRequest;
import com.example.informationsecurity.dto.ComparisonTestResponse;
import com.example.informationsecurity.dto.RandomGeneratorRequest;
import com.example.informationsecurity.dto.RandomGeneratorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RandomNumberServiceTest {

    @Mock
    private RandomNumberGenerator randomNumberGenerator;

    @Mock
    private CesaroTestProcessor cesaroTestProcessor;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private RandomNumberService randomNumberService;

    

    @Test
    void generateNumbers_success() {
        
        RandomGeneratorRequest request = new RandomGeneratorRequest(1L, 10L, 1L, 100L, 5);
        List<Long> mockNumbers = Arrays.asList(10L, 20L, 30L, 40L, 50L);
        String expectedFilePath = "/tmp/numbers.txt";

        when(randomNumberGenerator.generateLehmerNumbers(
                request.getSeed(), request.getMultiplier(), request.getIncrement(),
                request.getModulus(), request.getCount()))
                .thenReturn(mockNumbers);

        when(fileStorageService.saveNumbersToFile(mockNumbers))
                .thenReturn(expectedFilePath);

        
        RandomGeneratorResponse response = randomNumberService.generateNumbers(request);

        
        assertNotNull(response);
        assertEquals(mockNumbers, response.getNumbers());
        assertEquals(expectedFilePath, response.getFilePath());
        assertEquals(mockNumbers.size(), response.getGeneratedCount());
        verify(randomNumberGenerator, times(1)).generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
        verify(fileStorageService, times(1)).saveNumbersToFile(mockNumbers);
    }

    @Test
    void generateNumbers_invalidCount_throwsException() {
        
        RandomGeneratorRequest request = new RandomGeneratorRequest(1L, 10L, 1L, 100L, 0);

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.generateNumbers(request));
        assertEquals("Count must be positive", exception.getMessage());
        verifyNoInteractions(randomNumberGenerator, fileStorageService);
    }

    @Test
    void generateNumbers_invalidMultiplier_throwsException() {
        
        RandomGeneratorRequest request = new RandomGeneratorRequest(1L, 0L, 1L, 100L, 5);

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.generateNumbers(request));
        assertEquals("Multiplier must be positive", exception.getMessage());
        verifyNoInteractions(randomNumberGenerator, fileStorageService);
    }

    

    @Test
    void performCesaroTest_lehmer_success() {
        
        CesaroTestRequest request = CesaroTestRequest.builder()
                .pairsCount(100)
                .useSystemRandom(false)
                .modulus(100L)
                .multiplier(10L)
                .build();
        CesaroTestResponse expectedResponse = CesaroTestResponse.builder().errorPercentage(5.5).build();

        when(cesaroTestProcessor.performCesaroTest(request)).thenReturn(expectedResponse);

        
        CesaroTestResponse response = randomNumberService.performCesaroTest(request);

        
        assertNotNull(response);
        assertEquals(expectedResponse.getErrorPercentage(), response.getErrorPercentage());
        verify(cesaroTestProcessor, times(1)).performCesaroTest(request);
    }

    @Test
    void performCesaroTest_systemRandom_success() {
        
        CesaroTestRequest request = CesaroTestRequest.builder()
                .pairsCount(100)
                .useSystemRandom(true)
                .build();
        CesaroTestResponse expectedResponse = CesaroTestResponse.builder().errorPercentage(2.1).build();

        when(cesaroTestProcessor.performCesaroTest(request)).thenReturn(expectedResponse);

        
        CesaroTestResponse response = randomNumberService.performCesaroTest(request);

        
        assertNotNull(response);
        assertEquals(expectedResponse.getErrorPercentage(), response.getErrorPercentage());
        verify(cesaroTestProcessor, times(1)).performCesaroTest(request);
    }

    @Test
    void performCesaroTest_invalidPairsCount_throwsException() {
        
        CesaroTestRequest request = CesaroTestRequest.builder()
                .pairsCount(0)
                .useSystemRandom(false)
                .modulus(100L)
                .multiplier(10L)
                .build();

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.performCesaroTest(request));
        assertEquals("Pairs count must be positive", exception.getMessage());
        verifyNoInteractions(cesaroTestProcessor);
    }

    @Test
    void performCesaroTest_lehmerInvalidModulus_throwsException() {
        
        CesaroTestRequest request = CesaroTestRequest.builder()
                .pairsCount(100)
                .useSystemRandom(false)
                .modulus(0L)
                .multiplier(10L)
                .build();

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.performCesaroTest(request));
        assertEquals("Modulus must be positive", exception.getMessage());
        verifyNoInteractions(cesaroTestProcessor);
    }

    @Test
    void performCesaroTest_lehmerInvalidMultiplier_throwsException() {
        
        CesaroTestRequest request = CesaroTestRequest.builder()
                .pairsCount(100)
                .useSystemRandom(false)
                .modulus(100L)
                .multiplier(0L)
                .build();

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.performCesaroTest(request));
        assertEquals("Multiplier must be positive", exception.getMessage());
        verifyNoInteractions(cesaroTestProcessor);
    }

    

    @Test
    void compareGenerators_lehmerIsBetter_success() {
        
        ComparisonTestRequest request = new ComparisonTestRequest(1L, 10L, 1L, 100L, 1000);

        
        CesaroTestResponse lehmerTestResult = CesaroTestResponse.builder().errorPercentage(2.5).build();
        CesaroTestResponse systemTestResult = CesaroTestResponse.builder().errorPercentage(4.0).build();

        
        when(cesaroTestProcessor.performCesaroTest(any(CesaroTestRequest.class)))
                .thenReturn(lehmerTestResult) 
                .thenReturn(systemTestResult); 

        
        ComparisonTestResponse response = randomNumberService.compareGenerators(request);

        
        assertNotNull(response);
        ComparisonMetrics metrics = response.getComparison();
        assertEquals(2.5, metrics.getLehmerErrorPercentage(), 0.01);
        assertEquals(4.0, metrics.getSystemErrorPercentage(), 0.01);
        assertEquals("Lehmer Algorithm", metrics.getBetterGenerator());
        assertEquals(1.5, metrics.getErrorDifference(), 0.01);

        
        verify(cesaroTestProcessor, times(2)).performCesaroTest(any(CesaroTestRequest.class));
    }

    @Test
    void compareGenerators_systemRandomIsBetter_success() {
        
        ComparisonTestRequest request = new ComparisonTestRequest(1L, 10L, 1L, 100L, 1000);

        
        CesaroTestResponse lehmerTestResult = CesaroTestResponse.builder().errorPercentage(5.5).build();
        CesaroTestResponse systemTestResult = CesaroTestResponse.builder().errorPercentage(1.2).build();

        
        when(cesaroTestProcessor.performCesaroTest(any(CesaroTestRequest.class)))
                .thenReturn(lehmerTestResult) 
                .thenReturn(systemTestResult); 

        
        ComparisonTestResponse response = randomNumberService.compareGenerators(request);

        
        assertNotNull(response);
        ComparisonMetrics metrics = response.getComparison();
        assertEquals(5.5, metrics.getLehmerErrorPercentage(), 0.01);
        assertEquals(1.2, metrics.getSystemErrorPercentage(), 0.01);
        assertEquals("System Random", metrics.getBetterGenerator());
        assertEquals(4.3, metrics.getErrorDifference(), 0.01);
    }

    @Test
    void compareGenerators_errorsAreEqual_systemRandomIsChosen() {
        
        ComparisonTestRequest request = new ComparisonTestRequest(1L, 10L, 1L, 100L, 1000);

        
        CesaroTestResponse lehmerTestResult = CesaroTestResponse.builder().errorPercentage(3.0).build();
        CesaroTestResponse systemTestResult = CesaroTestResponse.builder().errorPercentage(3.0).build();

        
        when(cesaroTestProcessor.performCesaroTest(any(CesaroTestRequest.class)))
                .thenReturn(lehmerTestResult) 
                .thenReturn(systemTestResult); 

        
        ComparisonTestResponse response = randomNumberService.compareGenerators(request);

        
        assertNotNull(response);
        ComparisonMetrics metrics = response.getComparison();
        
        
        assertEquals("System Random", metrics.getBetterGenerator());
        assertEquals(0.0, metrics.getErrorDifference(), 0.01);
    }

    @Test
    void compareGenerators_invalidPairsCount_throwsException() {
        
        ComparisonTestRequest request = new ComparisonTestRequest(1L, 10L, 1L, 100L, 0);

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.compareGenerators(request));
        assertEquals("Pairs count must be positive", exception.getMessage());
        verifyNoInteractions(cesaroTestProcessor);
    }

    @Test
    void compareGenerators_invalidMultiplier_throwsException() {
        
        ComparisonTestRequest request = new ComparisonTestRequest(1L, 0L, 1L, 100L, 1000);

        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> randomNumberService.compareGenerators(request));
        assertEquals("Multiplier must be positive", exception.getMessage());
        verifyNoInteractions(cesaroTestProcessor);
    }

}