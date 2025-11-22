package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RSAServiceTest {

    private RSAService rsaService;

    
    @TempDir
    Path tempDir;

    private KeyPair keyPair2048;
    private static final int KEY_SIZE_2048 = 2048;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        rsaService = new RSAService();
        
        keyPair2048 = rsaService.generateKeyPair(KEY_SIZE_2048);
    }

    
    private Path createTestFile(String fileName, byte[] content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, content);
        return filePath;
    }

    

    @Test
    void testGenerateKeyPair_success() {
        assertNotNull(keyPair2048.getPublic(), "Public key should be generated");
        assertNotNull(keyPair2048.getPrivate(), "Private key should be generated");
        assertEquals("RSA", keyPair2048.getPublic().getAlgorithm());
        
        assertTrue(keyPair2048.getPublic().getEncoded().length > 256); 
    }

    @Test
    void testGenerateKeyPair_differentSize() throws NoSuchAlgorithmException {
        
        KeyPair keyPair1024 = rsaService.generateKeyPair(1024);
        assertNotNull(keyPair1024.getPublic());
        assertNotNull(keyPair1024.getPrivate());
    }

    

    
    private final int MAX_BLOCK_SIZE_2048 = 245;

    @Test
    void testEncryptDecrypt_SmallDataBlock() throws Exception {
        
        byte[] originalData = new byte[100];
        for (int i = 0; i < originalData.length; i++) {
            originalData[i] = (byte) i;
        }

        Path inputFile = createTestFile("small_input.txt", originalData);
        Path encryptedFile = tempDir.resolve("small_encrypted.bin");
        Path decryptedFile = tempDir.resolve("small_decrypted.txt");

        
        rsaService.encryptFile(inputFile, encryptedFile, keyPair2048.getPublic());

        
        rsaService.decryptFile(encryptedFile, decryptedFile, keyPair2048.getPrivate());

        
        byte[] decryptedData = Files.readAllBytes(decryptedFile);
        assertArrayEquals(originalData, decryptedData, "Decrypted data must match original data for small block");
    }

    @Test
    void testEncryptDecrypt_MaxSingleBlock() throws Exception {
        
        byte[] originalData = new byte[MAX_BLOCK_SIZE_2048];
        for (int i = 0; i < originalData.length; i++) {
            originalData[i] = (byte) (i % 256);
        }

        Path inputFile = createTestFile("max_input.txt", originalData);
        Path encryptedFile = tempDir.resolve("max_encrypted.bin");
        Path decryptedFile = tempDir.resolve("max_decrypted.txt");

        
        rsaService.encryptFile(inputFile, encryptedFile, keyPair2048.getPublic());

        
        rsaService.decryptFile(encryptedFile, decryptedFile, keyPair2048.getPrivate());

        
        byte[] decryptedData = Files.readAllBytes(decryptedFile);
        assertArrayEquals(originalData, decryptedData, "Decrypted data must match original data for max single block");
    }

    @Test
    void testEncryptDecrypt_MultipleBlocks() throws Exception {
        
        int totalLength = (2 * MAX_BLOCK_SIZE_2048) + 100; 
        byte[] originalData = new byte[totalLength];
        for (int i = 0; i < originalData.length; i++) {
            originalData[i] = (byte) (i % 128);
        }

        Path inputFile = createTestFile("multi_input.txt", originalData);
        Path encryptedFile = tempDir.resolve("multi_encrypted.bin");
        Path decryptedFile = tempDir.resolve("multi_decrypted.txt");

        
        rsaService.encryptFile(inputFile, encryptedFile, keyPair2048.getPublic());

        
        assertEquals(3 * 256, Files.readAllBytes(encryptedFile).length, "Encrypted size must be a multiple of the key size (256)");

        
        rsaService.decryptFile(encryptedFile, decryptedFile, keyPair2048.getPrivate());

        
        byte[] decryptedData = Files.readAllBytes(decryptedFile);
        assertArrayEquals(originalData, decryptedData, "Decrypted data must match original data for multiple blocks");
    }

    

    @Test
    void testDecryptFile_ThrowsIllegalBlockSizeException_WhenInputIsCorrupted() throws Exception {
        
        byte[] originalData = new byte[100];
        Path inputFile = createTestFile("error_input.txt", originalData);
        Path encryptedFile = tempDir.resolve("error_encrypted.bin");
        rsaService.encryptFile(inputFile, encryptedFile, keyPair2048.getPublic());

        
        byte[] encryptedBytes = Files.readAllBytes(encryptedFile);

        
        byte[] corruptedBytes = new byte[encryptedBytes.length - 1];
        System.arraycopy(encryptedBytes, 0, corruptedBytes, 0, corruptedBytes.length);

        Path corruptedFile = createTestFile("corrupted.bin", corruptedBytes);
        Path decryptedFile = tempDir.resolve("corrupted_decrypted.txt");

        
        Exception exception = assertThrows(IllegalBlockSizeException.class, () -> rsaService.decryptFile(corruptedFile, decryptedFile, keyPair2048.getPrivate()));

        assertTrue(exception.getMessage().contains("Encrypted data length is not a multiple of the RSA block size"), "Exception message should indicate block size issue");
    }

    
    

    

    @Test
    void testGetPublicKeyFromFile_RawEncoded() throws Exception {
        byte[] rawPublicKey = keyPair2048.getPublic().getEncoded();
        Path keyFile = createTestFile("raw_public.key", rawPublicKey);

        PublicKey loadedKey = rsaService.getPublicKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertEquals("X.509", loadedKey.getFormat());
        assertArrayEquals(rawPublicKey, loadedKey.getEncoded(), "Raw loaded key bytes must match original encoded bytes");
    }

    @Test
    void testGetPrivateKeyFromFile_RawEncoded() throws Exception {
        byte[] rawPrivateKey = keyPair2048.getPrivate().getEncoded();
        Path keyFile = createTestFile("raw_private.key", rawPrivateKey);

        PrivateKey loadedKey = rsaService.getPrivateKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertEquals("PKCS#8", loadedKey.getFormat());
        assertArrayEquals(rawPrivateKey, loadedKey.getEncoded(), "Raw loaded key bytes must match original encoded bytes");
    }

    @Test
    void testGetPublicKeyFromFile_PEMFormatted() throws Exception {
        
        String base64Key = Base64.getEncoder().encodeToString(keyPair2048.getPublic().getEncoded());

        
        String pemKey = "-----BEGIN PUBLIC KEY-----\n" +
                base64Key.substring(0, base64Key.length() / 2) + "\n" +
                base64Key.substring(base64Key.length() / 2) + "  \n" +
                "-----END PUBLIC KEY-----";

        Path keyFile = createTestFile("pem_public.pem", pemKey.getBytes());

        PublicKey loadedKey = rsaService.getPublicKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertArrayEquals(keyPair2048.getPublic().getEncoded(), loadedKey.getEncoded(), "PEM loaded public key must match original key");
    }

    @Test
    void testGetPrivateKeyFromFile_PEMFormatted() throws Exception {
        
        String base64Key = Base64.getEncoder().encodeToString(keyPair2048.getPrivate().getEncoded());

        
        String pemKey = "-----BEGIN PRIVATE KEY-----\n" +
                base64Key.substring(0, base64Key.length() / 2) + "\n" +
                base64Key.substring(base64Key.length() / 2) + "\t \n" +
                "-----END PRIVATE KEY-----";

        Path keyFile = createTestFile("pem_private.pem", pemKey.getBytes());

        PrivateKey loadedKey = rsaService.getPrivateKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertArrayEquals(keyPair2048.getPrivate().getEncoded(), loadedKey.getEncoded(), "PEM loaded private key must match original key");
    }
}