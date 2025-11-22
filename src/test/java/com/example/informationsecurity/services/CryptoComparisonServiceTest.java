package com.example.informationsecurity.services;

import com.example.informationsecurity.dto.ComparisonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CryptoComparisonServiceTest {
    @Spy
    @InjectMocks
    private CryptoComparisonService spyComparisonService;

    @Mock
    private RSAService rsaService;

    @TempDir
    Path tempDir;

    private Path testFile;
    private final int RSA_KEY_SIZE = 2048;
    private final long TEST_FILE_SIZE = 10000; 

    @BeforeEach
    void setUp() throws IOException, NoSuchAlgorithmException {
        
        byte[] fileContent = new byte[(int) TEST_FILE_SIZE];
        testFile = tempDir.resolve("test_data.bin");
        Files.write(testFile, fileContent);

        
        when(rsaService.generateKeyPair(eq(RSA_KEY_SIZE))).thenReturn(new KeyPair(null, null));
    }

    
    private Path createTestFileWithSize() throws IOException {
        Path path = tempDir.resolve("sized_file.bin");
        Files.write(path, new byte[(int) (long) 100]);
        return path;
    }

    @Test
    void testCompareSpeeds_RSAIsFaster() throws Exception {
        
        long rsaTime = 100;
        long rc5Time = 200;

        doReturn(rsaTime).when(spyComparisonService).measureRSATime(any(), any());
        doReturn(rc5Time).when(spyComparisonService).measureRC5Time(any(), any());

        
        ComparisonResult result = spyComparisonService.compareSpeeds(testFile, RSA_KEY_SIZE);

        
        assertEquals(rsaTime, result.getRsaTimeMs());
        assertEquals(rc5Time, result.getRc5TimeMs());
        assertEquals("RSA", result.getWinner(), "RSA should be the winner.");

        
        double expectedRsaSpeed = (double) TEST_FILE_SIZE / rsaTime * 1000;
        double expectedRc5Speed = (double) TEST_FILE_SIZE / rc5Time * 1000;

        assertEquals(expectedRsaSpeed, result.getRsaSpeedBps(), 0.001);
        assertEquals(expectedRc5Speed, result.getRc5SpeedBps(), 0.001);
        assertEquals(TEST_FILE_SIZE, result.getFileSizeBytes());

        
        verify(rsaService).generateKeyPair(RSA_KEY_SIZE);
        verify(spyComparisonService).measureRSATime(eq(testFile), any());
        verify(spyComparisonService).measureRC5Time(eq(testFile), any());
    }

    @Test
    void testCompareSpeeds_RC5IsFaster() throws Exception {
        
        long rsaTime = 500;
        long rc5Time = 50;

        doReturn(rsaTime).when(spyComparisonService).measureRSATime(any(), any());
        doReturn(rc5Time).when(spyComparisonService).measureRC5Time(any(), any());

        
        ComparisonResult result = spyComparisonService.compareSpeeds(testFile, RSA_KEY_SIZE);

        
        assertEquals("RC5", result.getWinner(), "RC5 should be the winner.");

        double expectedRsaSpeed = (double) TEST_FILE_SIZE / rsaTime * 1000;
        double expectedRc5Speed = (double) TEST_FILE_SIZE / rc5Time * 1000;

        assertEquals(expectedRsaSpeed, result.getRsaSpeedBps(), 0.001);
        assertEquals(expectedRc5Speed, result.getRc5SpeedBps(), 0.001);
    }

    @Test
    void testCompareSpeeds_Tie() throws Exception {
        
        long time = 100;

        doReturn(time).when(spyComparisonService).measureRSATime(any(), any());
        doReturn(time).when(spyComparisonService).measureRC5Time(any(), any());

        
        ComparisonResult result = spyComparisonService.compareSpeeds(testFile, RSA_KEY_SIZE);

        
        assertEquals("RC5", result.getWinner(), "RC5 should be the winner in case of a tie.");
    }

    @Test
    void testCompareSpeeds_ZeroTimeHandling() throws Exception {
        long time = 0;
        
        Path zeroTimeFile = createTestFileWithSize();

        doReturn(time).when(spyComparisonService).measureRSATime(any(), any());
        doReturn(time).when(spyComparisonService).measureRC5Time(any(), any());

        
        ComparisonResult result = spyComparisonService.compareSpeeds(zeroTimeFile, RSA_KEY_SIZE);
        
        assertTrue(Double.isInfinite(result.getRsaSpeedBps()));
        assertTrue(Double.isInfinite(result.getRc5SpeedBps()));
        assertEquals("RC5", result.getWinner());
    }
}