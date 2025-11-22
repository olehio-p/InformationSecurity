package com.example.informationsecurity.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CryptoUtilsTest {

    private static final int BLOCK_SIZE = 16;

    

    @Test
    void testBlockSizeConstant() {
        assertEquals(16, CryptoUtils.BLOCK_SIZE, "BLOCK_SIZE should be 16 as defined.");
    }

    

    @Test
    void testLongToBytesAndBytesToLong_ZeroValue() {
        long value = 0L;
        byte[] buffer = new byte[8];
        CryptoUtils.longToBytes(value, buffer, 0);

        byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        assertArrayEquals(expected, buffer, "Zero value should be encoded as all zeros.");

        long result = CryptoUtils.bytesToLong(buffer, 0);
        assertEquals(value, result, "Decoding all zeros should yield 0.");
    }

    @Test
    void testLongToBytesAndBytesToLong_MaxPositiveValue() {
        long value = Long.MAX_VALUE; 
        byte[] buffer = new byte[8];
        CryptoUtils.longToBytes(value, buffer, 0);

        byte[] expected = new byte[]{
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F
        }; 
        assertArrayEquals(expected, buffer, "Max value conversion failed (little endian).");

        long result = CryptoUtils.bytesToLong(buffer, 0);
        assertEquals(value, result, "Decoding Long.MAX_VALUE should yield the correct value.");
    }

    @Test
    void testLongToBytesAndBytesToLong_WithOffset() {
        long value = 0xDEADBEEFL;
        byte[] buffer = new byte[16];
        int offset = 4;
        CryptoUtils.longToBytes(value, buffer, offset);

        
        byte[] expectedSection = new byte[]{
                (byte) 0xEF, (byte) 0xBE, (byte) 0xAD, (byte) 0xDE, 0x00, 0x00, 0x00, 0x00
        };

        
        byte[] actualSection = Arrays.copyOfRange(buffer, offset, offset + 8);
        assertArrayEquals(expectedSection, actualSection, "Conversion with offset failed.");

        long result = CryptoUtils.bytesToLong(buffer, offset);
        assertEquals(value, result, "Decoding with offset failed.");
    }

    

    @Test
    void testXorBlocks_Success() {
        byte[] b1 = {0x0F, (byte) 0xAA, 0x11, 0x00};
        byte[] b2 = {0x0A, (byte) 0x55, 0x00, 0x01};
        byte[] output = new byte[4];

        
        byte[] expected = {0x05, (byte) 0xFF, 0x11, 0x01};

        CryptoUtils.xorBlocks(b1, b2, output);
        assertArrayEquals(expected, output, "XOR operation failed.");
    }

    @Test
    void testXorBlocks_MismatchedLengthThrowsException() {
        byte[] b1 = {1, 2, 3};
        byte[] b2 = {1, 2};
        byte[] output = new byte[3];

        assertThrows(IllegalArgumentException.class, () -> CryptoUtils.xorBlocks(b1, b2, output), "Should throw exception if block lengths mismatch.");
    }

    

    @Test
    void testPad_FullBlock_RequiresFullPaddingBlock() {
        byte[] data = new byte[BLOCK_SIZE]; 
        byte[] padded = CryptoUtils.pad(data);

        
        assertEquals(BLOCK_SIZE * 2, padded.length);

        
        assertEquals(16, padded[padded.length - 1]);
        assertEquals(16, padded[padded.length - 16]);
    }

    @Test
    void testPad_PartialBlock_RequiresPartialPadding() {
        byte[] data = new byte[BLOCK_SIZE - 5]; 
        byte[] padded = CryptoUtils.pad(data);

        
        assertEquals(BLOCK_SIZE, padded.length);

        
        assertEquals(5, padded[padded.length - 1]);
        assertEquals(5, padded[padded.length - 5]);
        assertNotEquals(5, padded[padded.length - 6]); 
    }

    @Test
    void testPad_EmptyData_RequiresFullPaddingBlock() {
        byte[] data = new byte[0];
        byte[] padded = CryptoUtils.pad(data);

        
        assertEquals(BLOCK_SIZE, padded.length);

        
        assertEquals(16, padded[padded.length - 1]);
    }

    

    @Test
    void testUnpad_Success_PartialBlock() {
        
        byte[] originalData = "TestStringHere".getBytes(); 
        byte[] padded = CryptoUtils.pad(originalData);

        byte[] unpadded = CryptoUtils.unpad(padded);

        assertArrayEquals(originalData, unpadded, "Unpadding should restore the original data.");
    }

    @Test
    void testUnpad_Success_FullPaddingBlock() {
        
        byte[] originalData = "SixteenBytesLong".getBytes();
        byte[] padded = CryptoUtils.pad(originalData);

        byte[] unpadded = CryptoUtils.unpad(padded);

        assertArrayEquals(originalData, unpadded, "Unpadding should handle the full extra padding block.");
    }

    @Test
    void testUnpad_EmptyInput() {
        byte[] unpadded = CryptoUtils.unpad(new byte[0]);
        assertEquals(0, unpadded.length, "Unpadding empty array should return empty array.");
    }

    @Test
    void testUnpad_ThrowsException_InvalidPaddingSizeZero() {
        
        byte[] corrupted = new byte[BLOCK_SIZE];
        

        assertThrows(IllegalArgumentException.class, () -> CryptoUtils.unpad(corrupted), "Should reject padding size of 0.");
    }

    @Test
    void testUnpad_ThrowsException_InvalidPaddingSizeTooLarge() {
        
        byte[] corrupted = new byte[BLOCK_SIZE];
        corrupted[BLOCK_SIZE - 1] = 17;

        assertThrows(IllegalArgumentException.class, () -> CryptoUtils.unpad(corrupted), "Should reject padding size > BLOCK_SIZE.");
    }

    @Test
    void testUnpad_ThrowsException_LengthExceedsData() {
        
        byte[] corrupted = new byte[5];
        corrupted[4] = 16;

        assertThrows(IllegalArgumentException.class, () -> CryptoUtils.unpad(corrupted), "Should reject padding size larger than data length.");
    }
}