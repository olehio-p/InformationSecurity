package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RC5KeyServiceTest {

    @InjectMocks
    private RC5KeyService rc5KeyService;

    @Mock
    private MD5Service md5Service;

    private static final String TEST_PASSWORD = "12345678";
    private static final String HASH_P_HEX = "20021b033e08f87024f21d60b379c5c2";
    private static final String HASH_OF_HASH_P_HEX = "34812be2ab2b192e2101569426f84967";

    
    private final byte[] HASH_P_BYTES = new byte[] {
            (byte) 0x20, (byte) 0x02, (byte) 0x1B, (byte) 0x03, (byte) 0x3E, (byte) 0x08, (byte) 0xF8, (byte) 0x70,
            (byte) 0x24, (byte) 0xF2, (byte) 0x1D, (byte) 0x60, (byte) 0xB3, (byte) 0x79, (byte) 0xC5, (byte) 0xC2
    };

    
    private final byte[] HASH_OF_HASH_P_BYTES = new byte[] {
            (byte) 0x34, (byte) 0x81, (byte) 0x2B, (byte) 0xE2, (byte) 0xAB, (byte) 0x2B, (byte) 0x19, (byte) 0x2E,
            (byte) 0x21, (byte) 0x01, (byte) 0x56, (byte) 0x94, (byte) 0x26, (byte) 0xF8, (byte) 0x49, (byte) 0x67
    };

    @BeforeEach
    void setUp() throws IOException {
        
        
        lenient().when(md5Service.hashString(TEST_PASSWORD)).thenReturn(HASH_P_HEX);
    }

    

    @Test
    void testHexStringToByteArray_validInput() {
        
        byte[] result = ReflectionTestUtils.invokeMethod(rc5KeyService, "hexStringToByteArray", HASH_P_HEX);

        assertArrayEquals(HASH_P_BYTES, result, "Hex string conversion must produce the correct byte array.");
    }

    @Test
    void testHexStringToByteArray_emptyString() {
        byte[] result = ReflectionTestUtils.invokeMethod(rc5KeyService, "hexStringToByteArray", "");
        assertNotNull(result);
        assertEquals(0, result.length, "Empty hex string should result in an empty byte array.");
    }

    @Test
    void testHexStringToByteArray_invalidLengthThrowsException() {
        
        assertThrows(StringIndexOutOfBoundsException.class, () -> ReflectionTestUtils.invokeMethod(rc5KeyService, "hexStringToByteArray", "123"));
    }

    

    @Test
    void testDeriveKey_KeyLength8Bytes() throws IOException {
        
        byte[] expectedKey = Arrays.copyOfRange(HASH_P_BYTES, 8, 16);

        byte[] actualKey = rc5KeyService.deriveKey(TEST_PASSWORD, 8);

        assertEquals(8, actualKey.length);
        assertArrayEquals(expectedKey, actualKey, "8-byte key must be the last 8 bytes of the MD5 hash.");

        verify(md5Service, times(1)).hashString(TEST_PASSWORD);
        verify(md5Service, never()).hashString(HASH_P_HEX);
    }

    @Test
    void testDeriveKey_KeyLength16Bytes() throws IOException {

        byte[] actualKey = rc5KeyService.deriveKey(TEST_PASSWORD, 16);

        assertEquals(16, actualKey.length);
        assertArrayEquals(HASH_P_BYTES, actualKey, "16-byte key must be the full 16-byte MD5 hash.");

        verify(md5Service, times(1)).hashString(TEST_PASSWORD);
        verify(md5Service, never()).hashString(HASH_P_HEX);
    }

    @Test
    void testDeriveKey_KeyLength32Bytes() throws IOException {
        
        when(md5Service.hashString(HASH_P_HEX)).thenReturn(HASH_OF_HASH_P_HEX);

        
        byte[] expectedKey = new byte[32];
        System.arraycopy(HASH_OF_HASH_P_BYTES, 0, expectedKey, 0, 16);
        System.arraycopy(HASH_P_BYTES, 0, expectedKey, 16, 16);

        byte[] actualKey = rc5KeyService.deriveKey(TEST_PASSWORD, 32);

        assertEquals(32, actualKey.length);
        assertArrayEquals(expectedKey, actualKey, "32-byte key must be (MD5(MD5(P)) + MD5(P)).");

        verify(md5Service, times(1)).hashString(TEST_PASSWORD); 
        verify(md5Service, times(1)).hashString(HASH_P_HEX); 
    }

    @Test
    void testDeriveKey_FallbackLength4Bytes_TruncatesHash() throws IOException {
        
        byte[] expectedKey = Arrays.copyOfRange(HASH_P_BYTES, 0, 4);

        byte[] actualKey = rc5KeyService.deriveKey(TEST_PASSWORD, 4);

        assertEquals(4, actualKey.length);
        assertArrayEquals(expectedKey, actualKey, "Fallback key (4 bytes) must be the first 4 bytes of the hash.");
    }

    @Test
    void testDeriveKey_FallbackLength20Bytes_TruncatesTo16() throws IOException {

        byte[] actualKey = rc5KeyService.deriveKey(TEST_PASSWORD, 20);

        assertEquals(16, actualKey.length);
        assertArrayEquals(HASH_P_BYTES, actualKey, "Fallback key (>16, !=32) must be truncated to 16 bytes.");
    }

    @Test
    void testDeriveKey_MD5ServiceThrowsIOException() {
        
        try {
            when(md5Service.hashString(TEST_PASSWORD)).thenThrow(new IOException("MD5 Failure"));
        } catch (IOException e) {
            
        }

        assertThrows(IOException.class, () -> rc5KeyService.deriveKey(TEST_PASSWORD, 16), "IOException from MD5Service must propagate.");
    }
}