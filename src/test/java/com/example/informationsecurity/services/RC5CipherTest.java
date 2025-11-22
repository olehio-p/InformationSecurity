package com.example.informationsecurity.services;

import com.example.informationsecurity.utils.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RC5CipherTest {

    private RC5Cipher rc5Cipher;

    
    private static final int W = 64; 
    private static final int R = 12; 
    private static final int B = 16; 

    
    private final byte[] TEST_KEY = new byte[B];

    
    private final byte[] TEST_PLAINTEXT = new byte[16]; 

    @BeforeEach
    void setUp() {
        
        Arrays.fill(TEST_KEY, (byte) 0x11);
        Arrays.fill(TEST_PLAINTEXT, (byte) 0xAA);
    }


    @Test
    void testConstructor_validParameters() {
        assertDoesNotThrow(() -> new RC5Cipher(W, R, B));
        rc5Cipher = new RC5Cipher(W, R, B);
        assertEquals(W, rc5Cipher.getW());
        assertEquals(R, rc5Cipher.getR());
        assertEquals(B, rc5Cipher.getB());
        assertEquals(2 * (R + 1), rc5Cipher.getT());
    }

    @Test
    void testConstructor_invalidWordSize() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new RC5Cipher(32, R, B));
        assertTrue(thrown.getMessage().contains("RC5 implementation supports only w=64 bit word size."));
    }

    @Test
    void testSetupKey_initializesSArray() {
        rc5Cipher = new RC5Cipher(W, R, B);
        rc5Cipher.setupKey(TEST_KEY);

        assertNotNull(rc5Cipher.getS(), "S array must be initialized.");
        assertEquals(rc5Cipher.getT(), rc5Cipher.getS().length, "S array length must be 2*(r+1).");

        
        long initialS0 = (long) ReflectionTestUtils.getField(rc5Cipher, "P_64");
        long firstS0 = rc5Cipher.getS()[0];

        assertNotEquals(initialS0, firstS0, "S array elements should be mixed by the key schedule.");
    }

    

    @Test
    void testEncryptDecrypt_InverseProperty_SimpleBlock() {
        rc5Cipher = new RC5Cipher(W, R, B);
        rc5Cipher.setupKey(TEST_KEY);

        byte[] ciphertext = rc5Cipher.encryptBlock(TEST_PLAINTEXT);
        assertNotNull(ciphertext, "Ciphertext should not be null.");
        assertEquals(16, ciphertext.length, "Ciphertext length should be 16 bytes.");

        
        assertFalse(Arrays.equals(TEST_PLAINTEXT, ciphertext), "Ciphertext must be different from plaintext.");

        byte[] decryptedPlaintext = rc5Cipher.decryptBlock(ciphertext);

        assertArrayEquals(TEST_PLAINTEXT, decryptedPlaintext, "Decryption must return the original plaintext.");
    }

    @Test
    void testEncryptDecrypt_InverseProperty_MaxAndMinLongValues() {
        rc5Cipher = new RC5Cipher(W, R, B);
        rc5Cipher.setupKey(TEST_KEY);

        
        byte[] extremePlaintext = new byte[16];
        CryptoUtils.longToBytes(Long.MAX_VALUE, extremePlaintext, 0); 
        CryptoUtils.longToBytes(Long.MIN_VALUE, extremePlaintext, 8); 

        byte[] ciphertext = rc5Cipher.encryptBlock(extremePlaintext);
        byte[] decryptedPlaintext = rc5Cipher.decryptBlock(ciphertext);

        assertArrayEquals(extremePlaintext, decryptedPlaintext, "Decryption must correctly handle max/min long values (word boundaries).");
    }

    @Test
    void testEncryptDecrypt_InverseProperty_ZeroBlock() {
        rc5Cipher = new RC5Cipher(W, R, B);
        rc5Cipher.setupKey(TEST_KEY);

        byte[] zeroPlaintext = new byte[16]; 

        byte[] ciphertext = rc5Cipher.encryptBlock(zeroPlaintext);

        assertFalse(Arrays.equals(zeroPlaintext, ciphertext), "Ciphertext of zero block should not be zero.");

        byte[] decryptedPlaintext = rc5Cipher.decryptBlock(ciphertext);

        assertArrayEquals(zeroPlaintext, decryptedPlaintext, "Decryption must return the original zero plaintext.");
    }

    

    @Test
    void testKeySetup_Consistency() {
        
        RC5Cipher cipher1 = new RC5Cipher(W, R, B);
        cipher1.setupKey(TEST_KEY);

        RC5Cipher cipher2 = new RC5Cipher(W, R, B);
        cipher2.setupKey(TEST_KEY);

        
        byte[] c1 = cipher1.encryptBlock(TEST_PLAINTEXT);
        byte[] c2 = cipher2.encryptBlock(TEST_PLAINTEXT);

        assertArrayEquals(c1, c2, "Two ciphers set up with the same key must produce identical ciphertext.");
        assertArrayEquals(cipher1.getS(), cipher2.getS(), "The internal S array must be identical for the same key and parameters.");
    }

    @Test
    void testKeySetup_DifferentKeysProduceDifferentSArrays() {
        RC5Cipher cipher1 = new RC5Cipher(W, R, B);
        cipher1.setupKey(TEST_KEY);

        byte[] differentKey = new byte[B];
        Arrays.fill(differentKey, (byte) 0x22);

        RC5Cipher cipher2 = new RC5Cipher(W, R, B);
        cipher2.setupKey(differentKey);

        assertFalse(Arrays.equals(cipher1.getS(), cipher2.getS()), "Different keys must produce different S arrays.");

        
        byte[] c1 = cipher1.encryptBlock(TEST_PLAINTEXT);
        byte[] c2 = cipher2.encryptBlock(TEST_PLAINTEXT);

        assertFalse(Arrays.equals(c1, c2), "Different keys must produce different ciphertexts.");
    }
}