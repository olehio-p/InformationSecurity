package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LCGServiceTest {

    @InjectMocks
    private LCGService lcgService;

    @Mock
    private RandomNumberGenerator randomNumberGenerator;

    
    private static final long MODULUS = 2147483647L; 
    private static final long MULTIPLIER = 16807L;
    private static final long INCREMENT = 0L;
    private static final int EXPECTED_IV_SIZE = 16;
    private static final int IV_NUMBERS_COUNT = 2; 

    @BeforeEach
    void setUp() {
        
        ReflectionTestUtils.setField(lcgService, "modulus", MODULUS);
        ReflectionTestUtils.setField(lcgService, "multiplier", MULTIPLIER);
        ReflectionTestUtils.setField(lcgService, "increment", INCREMENT);
    }

    

    @Test
    void testGenerateIV_invalidSizeThrowsException() {
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> lcgService.generateIV(8));

        assertTrue(thrown.getMessage().contains("IV size must be 16 bytes for RC5 w=64."));
    }

    @Test
    void testGenerateIV_nominalExecutionFlow() {
        
        when(randomNumberGenerator.generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), eq(IV_NUMBERS_COUNT)))
                .thenReturn(Arrays.asList(1L, 2L)); 

        
        assertDoesNotThrow(() -> lcgService.generateIV(EXPECTED_IV_SIZE));

        
        verify(randomNumberGenerator, times(1)).generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
    }

    

    @Test
    void testGenerateIV_delegatesToRandomNumberGeneratorWithCorrectParams() {
        
        when(randomNumberGenerator.generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(Arrays.asList(1L, 2L));

        lcgService.generateIV(EXPECTED_IV_SIZE);

        
        ArgumentCaptor<Long> seedCaptor = ArgumentCaptor.forClass(Long.class);
        verify(randomNumberGenerator).generateLehmerNumbers(
                seedCaptor.capture(),
                eq(MULTIPLIER),
                eq(INCREMENT),
                eq(MODULUS),
                eq(IV_NUMBERS_COUNT));

        
        long capturedSeed = seedCaptor.getValue();
        
        assertTrue(capturedSeed >= 0 && capturedSeed < MODULUS, "Captured seed must be the timestamp modulo the modulus.");
    }

    @Test
    void testGenerateIV_convertsLongsToLittleEndianByteArray() {
        
        
        long long1 = 0x1122334455667788L;

        
        long long2 = 0xAABBCCDDEEFF0011L;

        when(randomNumberGenerator.generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(Arrays.asList(long1, long2));

        byte[] actualIv = lcgService.generateIV(EXPECTED_IV_SIZE);

        
        byte[] expectedIv = new byte[]{
                
                (byte) 0x88, (byte) 0x77, (byte) 0x66, (byte) 0x55, (byte) 0x44, (byte) 0x33, (byte) 0x22, (byte) 0x11,
                
                (byte) 0x11, (byte) 0x00, (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA
        };

        assertEquals(EXPECTED_IV_SIZE, actualIv.length);
        assertArrayEquals(expectedIv, actualIv, "IV bytes must be 16 bytes and represent the two long values in little-endian order.");
    }

    @Test
    void testGenerateIV_handlesZeroAndMaxValuesCorrectly() {
        
        long long1 = 0L;

        
        long long2 = Long.MAX_VALUE;

        when(randomNumberGenerator.generateLehmerNumbers(anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(Arrays.asList(long1, long2));

        byte[] actualIv = lcgService.generateIV(EXPECTED_IV_SIZE);

        
        byte[] expectedIv = new byte[]{
                
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F
        };

        assertArrayEquals(expectedIv, actualIv, "IV bytes must correctly encode zero and maximum positive long values.");
    }
}