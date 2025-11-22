package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RandomNumberGeneratorTest {

    @InjectMocks
    private RandomNumberGenerator generator;

    @BeforeEach
    void setUp() {
        
    }

    

    @Test
    void testGenerateLehmerNumbers_standardDeterministicSequence() {
        
        
        long seed = 1L;
        long multiplier = 5L;
        long increment = 1L;
        long modulus = 16L;
        int count = 5;

        
        List<Long> expected = Arrays.asList(6L, 15L, 12L, 13L, 2L);

        List<Long> actual = generator.generateLehmerNumbers(seed, multiplier, increment, modulus, count);

        assertEquals(count, actual.size(), "Generated list size must match the count.");
        assertEquals(expected, actual, "The LCG sequence must be deterministic and match the expected output.");
    }

    @Test
    void testGenerateLehmerNumbers_zeroCount() {
        List<Long> actual = generator.generateLehmerNumbers(1L, 5L, 1L, 16L, 0);
        assertTrue(actual.isEmpty(), "Generating zero numbers should result in an empty list.");
    }

    @Test
    void testGenerateLehmerNumbers_modulusBehavior() {
        
        long seed = 100L;
        long multiplier = 10L;
        long increment = 5L;
        long modulus = 500L;
        int count = 2;

        
        
        List<Long> expected = Arrays.asList(5L, 55L);
        List<Long> actual = generator.generateLehmerNumbers(seed, multiplier, increment, modulus, count);

        assertEquals(expected, actual, "LCG must correctly apply the modulo operation.");
    }

    @Test
    void testGenerateLehmerNumbers_ensuresPositiveOutput() {
        
        
        
        
        long seed = 9223372036854775800L; 
        long multiplier = 2L;
        long increment = 1L;
        long modulus = 9223372036854775807L; 
        int count = 1;

        List<Long> actual = generator.generateLehmerNumbers(seed, multiplier, increment, modulus, count);

        assertEquals(1, actual.size());
        assertTrue(actual.get(0) >= 0, "The resulting number must be non-negative due to Math.abs()");
    }

    

    @Test
    void testGenerateSystemRandomNumbers_correctSize() {
        int count = 100;
        List<Long> actual = generator.generateSystemRandomNumbers(count);
        assertEquals(count, actual.size(), "Generated list size must match the requested count.");
    }

    @Test
    void testGenerateSystemRandomNumbers_numbersAreWithinRange() {
        int count = 1000;
        long maxExpectedValue = (long) Integer.MAX_VALUE - 1; 

        List<Long> actual = generator.generateSystemRandomNumbers(count);

        for (Long number : actual) {
            assertTrue(number >= 1, "Generated number must be greater than or equal to 1.");
            assertTrue(number <= maxExpectedValue, "Generated number must be less than or equal to Integer.MAX_VALUE - 1.");
        }
    }

    @Test
    void testGenerateSystemRandomNumbers_sequencesAreDifferent() {
        int count = 10;

        List<Long> sequence1 = generator.generateSystemRandomNumbers(count);
        List<Long> sequence2 = generator.generateSystemRandomNumbers(count);

        assertNotEquals(sequence1, sequence2, "Two sequential calls should produce statistically different sequences.");
    }

    @Test
    void testGenerateSystemRandomNumbers_zeroCount() {
        List<Long> actual = generator.generateSystemRandomNumbers(0);
        assertTrue(actual.isEmpty(), "Generating zero numbers should result in an empty list.");
    }
}