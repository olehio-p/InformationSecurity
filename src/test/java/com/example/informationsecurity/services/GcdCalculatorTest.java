package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GcdCalculatorTest {

    @InjectMocks
    private GcdCalculator calculator;

    @BeforeEach
    void setUp() {
        
    }

    

    @Test
    void testCalculateGCD_standardCase() {
        
        assertEquals(6, calculator.calculateGCD(48, 18), "GCD of 48 and 18 should be 6.");
        
        assertEquals(6, calculator.calculateGCD(18, 48), "GCD of 18 and 48 should be 6.");
    }

    @Test
    void testCalculateGCD_oneIsMultipleOfTheOther() {
        
        assertEquals(5, calculator.calculateGCD(10, 5), "GCD of 10 and 5 should be 5.");
        
        assertEquals(5, calculator.calculateGCD(5, 10), "GCD of 5 and 10 should be 5.");
    }

    @Test
    void testCalculateGCD_coprimeNumbers() {
        
        assertEquals(1, calculator.calculateGCD(7, 5), "GCD of coprime numbers (7 and 5) should be 1.");
    }

    

    @Test
    void testCalculateGCD_oneInputIsZero() {
        
        assertEquals(5, calculator.calculateGCD(5, 0), "GCD of a number and 0 should be the number itself.");
        
        assertEquals(5, calculator.calculateGCD(0, 5), "GCD of 0 and a number should be the number itself.");
    }

    @Test
    void testCalculateGCD_bothInputsAreZero() {
        
        assertEquals(0, calculator.calculateGCD(0, 0), "GCD of 0 and 0 should be 0 based on implementation.");
    }

    

    @Test
    void testCalculateGCD_oneInputIsNegative() {
        
        assertEquals(6, calculator.calculateGCD(-48, 18), "GCD with one negative input should be 6.");
        
        assertEquals(6, calculator.calculateGCD(18, -48), "GCD with one negative input should be 6.");
    }

    @Test
    void testCalculateGCD_bothInputsAreNegative() {
        
        assertEquals(6, calculator.calculateGCD(-48, -18), "GCD with both negative inputs should be 6.");
    }

    @Test
    void testCalculateGCD_largeCommonFactor() {
        long factor = 1000000000L;
        long a = 17 * factor;
        long b = 23 * factor;

        
        assertEquals(factor, calculator.calculateGCD(a, b), "GCD of large numbers with a large common factor.");
    }

    @Test
    void testCalculateGCD_inputsCloseToLongMax() {
        long a = Long.MAX_VALUE; 
        long b = a - 1; 
        
        assertEquals(1, calculator.calculateGCD(a, b), "GCD of consecutive large numbers should be 1.");
    }
}