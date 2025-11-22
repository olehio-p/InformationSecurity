package com.example.informationsecurity.services;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.CesaroTestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CesaroTestProcessorTest {

    @InjectMocks
    private CesaroTestProcessor processor;

    @Mock
    private RandomNumberGenerator numberGenerator;

    @Mock
    private GcdCalculator gcdCalculator;

    
    private static final long SEED = 1L;
    private static final long MULTIPLIER = 1L;
    private static final long INCREMENT = 1L;
    private static final long MODULUS = 1L;
    private static final int PAIRS_COUNT_SMALL = 5;
    private static final int PAIRS_COUNT_LARGE = 200;

    private CesaroTestRequest createMockRequest(int pairsCount, boolean useSystem) {
        return CesaroTestRequest.builder()
                .pairsCount(pairsCount)
                .useSystemRandom(useSystem)
                .seed(SEED)
                .multiplier(MULTIPLIER)
                .increment(INCREMENT)
                .modulus(MODULUS)
                .build();
    }

    private void setupMockGcds(List<Long> gcds) {
        if (gcds.isEmpty()) return;

        Long[] gcdArray = gcds.toArray(new Long[0]);

        
        when(gcdCalculator.calculateGCD(anyLong(), anyLong()))
                .thenReturn(gcdArray[0], Arrays.copyOfRange(gcdArray, 1, gcdArray.length));
    }

    @Test
    void testPerformCesaroTest_LCG_PerfectCoprimes() {
        CesaroTestRequest request = createMockRequest(PAIRS_COUNT_SMALL, false); 

        
        when(numberGenerator.generateLehmerNumbers(SEED, MULTIPLIER, INCREMENT, MODULUS, PAIRS_COUNT_SMALL * 2))
                .thenReturn(LongStream.range(1, 11).boxed().collect(Collectors.toList()));

        
        setupMockGcds(Arrays.asList(1L, 1L, 1L, 1L, 1L));

        CesaroTestResponse response = processor.performCesaroTest(request);

        
        assertEquals(PAIRS_COUNT_SMALL, response.getTotalPairs());
        assertEquals(PAIRS_COUNT_SMALL, response.getCoprimeCount(), "All pairs should be coprime.");
        assertEquals(1.0, response.getCoprimeProbability(), 0.0001, "Probability should be 1.0.");

        
        assertEquals(Math.sqrt(6.0), response.getEstimatedPi(), 0.0001, "Estimated Pi should be sqrt(6).");

        
        assertEquals("Lehmer Algorithm", response.getGeneratorType());

        
        assertEquals(5, response.getSamplePairs().size());
        assertEquals(1, response.getSamplePairs().get(0).getGcd());
        assertTrue(response.getSamplePairs().get(0).getAreCoprime());

        verify(numberGenerator).generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), eq(10));
        verify(numberGenerator, never()).generateSystemRandomNumbers(anyInt());
    }

    @Test
    void testPerformCesaroTest_SystemRandom_NoCoprimes() {
        CesaroTestRequest request = createMockRequest(PAIRS_COUNT_SMALL, true); 

        
        when(numberGenerator.generateSystemRandomNumbers(PAIRS_COUNT_SMALL * 2))
                .thenReturn(LongStream.range(100, 110).boxed().collect(Collectors.toList()));

        
        setupMockGcds(Arrays.asList(2L, 2L, 2L, 2L, 2L));

        CesaroTestResponse response = processor.performCesaroTest(request);

        
        assertEquals(PAIRS_COUNT_SMALL, response.getTotalPairs());
        assertEquals(0, response.getCoprimeCount(), "No pairs should be coprime.");
        assertEquals(0.0, response.getCoprimeProbability(), 0.0001, "Probability should be 0.0.");

        
        assertEquals(0.0, response.getEstimatedPi(), 0.0001, "Estimated Pi should be 0.");

        
        assertEquals("System Random", response.getGeneratorType());

        
        assertEquals(5, response.getSamplePairs().size());
        assertEquals(2, response.getSamplePairs().get(0).getGcd());
        assertFalse(response.getSamplePairs().get(0).getAreCoprime());

        verify(numberGenerator).generateSystemRandomNumbers(eq(10));
        verify(numberGenerator, never()).generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testPerformCesaroTest_MixedCoprimes_CheckPiEstimation() {
        CesaroTestRequest request = createMockRequest(10, true); 

        when(numberGenerator.generateSystemRandomNumbers(20))
                .thenReturn(LongStream.range(1, 21).boxed().collect(Collectors.toList()));

        
        
        setupMockGcds(Arrays.asList(1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 1L));

        CesaroTestResponse response = processor.performCesaroTest(request);

        int expectedCoprimeCount = 6;
        double expectedProbability = 6.0 / 10.0; 
        double expectedEstimatedPi = Math.sqrt(6.0 / expectedProbability); 

        
        assertEquals(10, response.getTotalPairs());
        assertEquals(expectedCoprimeCount, response.getCoprimeCount());
        assertEquals(expectedProbability, response.getCoprimeProbability(), 0.0001);
        assertEquals(expectedEstimatedPi, response.getEstimatedPi(), 0.0001);
        assertEquals(Math.PI, response.getActualPi(), 0.0001);

        
        double expectedError = Math.abs(expectedEstimatedPi - Math.PI);
        assertEquals(expectedError, response.getError(), 0.0001);
        assertEquals((expectedError / Math.PI) * 100, response.getErrorPercentage(), 0.0001);

        
        assertEquals(10, response.getSamplePairs().size());
        assertEquals(1L, response.getSamplePairs().get(0).getGcd(), "First pair GCD should be 1.");
        assertEquals(2L, response.getSamplePairs().get(1).getGcd(), "Second pair GCD should be 2.");
    }

    @Test
    void testPerformCesaroTest_LargePairCount_NoSampleCollection() {
        CesaroTestRequest request = createMockRequest(PAIRS_COUNT_LARGE, false); 

        
        when(numberGenerator.generateLehmerNumbers(SEED, MULTIPLIER, INCREMENT, MODULUS, PAIRS_COUNT_LARGE * 2))
                .thenReturn(LongStream.range(1, 401).boxed().collect(Collectors.toList()));

        
        List<Long> gcds = LongStream.range(0, PAIRS_COUNT_LARGE)
                .mapToObj(i -> (i % 2 == 0) ? 1L : 2L)
                .collect(Collectors.toList());
        setupMockGcds(gcds);

        CesaroTestResponse response = processor.performCesaroTest(request);

        
        assertEquals(PAIRS_COUNT_LARGE, response.getTotalPairs());
        assertEquals(100, response.getCoprimeCount(), "Expected 100 coprimes (50%).");

        
        assertTrue(response.getSamplePairs().isEmpty(), "Sample pairs should be empty for large pair counts.");

        
        verify(gcdCalculator, times(PAIRS_COUNT_LARGE)).calculateGCD(anyLong(), anyLong());
    }

    @Test
    void testSampleCollection_OnlyFirstTenPairsAreCollectedWhenPairsCountIsLessThanOrEqualTo100() {
        CesaroTestRequest request = createMockRequest(50, true); 

        
        when(numberGenerator.generateSystemRandomNumbers(100))
                .thenReturn(LongStream.range(1, 101).boxed().collect(Collectors.toList()));

        
        
        
        List<Long> gcds = LongStream.range(0, 50)
                .mapToObj(i -> (i < 10) ? 1L : 2L)
                .collect(Collectors.toList());
        setupMockGcds(gcds);

        CesaroTestResponse response = processor.performCesaroTest(request);

        
        assertEquals(50, response.getTotalPairs());
        assertEquals(10, response.getCoprimeCount(), "Only the first 10 pairs should be coprime.");

        
        assertEquals(10, response.getSamplePairs().size());

        
        assertEquals(1L, response.getSamplePairs().get(9).getGcd(), "The 10th sample (index 9) must have GCD=1.");
    }
}