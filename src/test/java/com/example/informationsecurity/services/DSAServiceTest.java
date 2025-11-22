package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Base64;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DSAServiceTest {

    @InjectMocks
    private DSAService dsaService;

    
    @TempDir
    Path tempDir;

    private KeyPair dsaKeyPair;
    private static final int KEY_SIZE = 1024;
    private static final String TEST_STRING = "The quick brown fox jumps over the lazy dog.";
    private final byte[] TEST_BYTES = TEST_STRING.getBytes(StandardCharsets.UTF_8);

    
    private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_FOOTER = "-----END PUBLIC KEY-----";
    private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_FOOTER = "-----END PRIVATE KEY-----";

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        
        dsaKeyPair = dsaService.generateKeyPair(KEY_SIZE);
    }

    
    private Path createTestFile(String fileName, byte[] content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, content);
        return filePath;
    }

    

    @Test
    void testGenerateKeyPair_success() {
        assertNotNull(dsaKeyPair.getPublic(), "Public key should be generated");
        assertNotNull(dsaKeyPair.getPrivate(), "Private key should be generated");
        assertEquals("DSA", dsaKeyPair.getPublic().getAlgorithm());

        
        assertTrue(dsaKeyPair.getPublic().getEncoded().length > KEY_SIZE / 8);
    }

    

    @Test
    void testSignDataAndVerify_success() throws Exception {
        byte[] signatureBytes = dsaService.signData(TEST_BYTES, dsaKeyPair.getPrivate());

        
        assertNotNull(signatureBytes);
        assertTrue(signatureBytes.length > 0);

        
        Signature verifier = Signature.getInstance(Objects.requireNonNull(ReflectionTestUtils.getField(dsaService, "SIGNATURE_ALGORITHM")).toString());
        verifier.initVerify(dsaKeyPair.getPublic());
        verifier.update(TEST_BYTES);

        assertTrue(verifier.verify(signatureBytes), "Verification should succeed with correct key and data.");
    }

    @Test
    void testSignDataAndVerify_failureDueToTamperedData() throws Exception {
        byte[] signatureBytes = dsaService.signData(TEST_BYTES, dsaKeyPair.getPrivate());

        
        byte[] tamperedBytes = "The quick brown fox jumps over the lazy cAt.".getBytes(StandardCharsets.UTF_8);

        Signature verifier = Signature.getInstance(Objects.requireNonNull(ReflectionTestUtils.getField(dsaService, "SIGNATURE_ALGORITHM")).toString());
        verifier.initVerify(dsaKeyPair.getPublic());
        verifier.update(tamperedBytes);

        assertFalse(verifier.verify(signatureBytes), "Verification should fail if data is tampered.");
    }

    

    @Test
    void testSignStringAndVerify() throws Exception {
        String signatureHex = dsaService.signString(TEST_STRING, dsaKeyPair.getPrivate());

        Path tempFile = createTestFile("signed_string.txt", TEST_BYTES);

        assertTrue(dsaService.verifyFileSignature(tempFile, signatureHex, dsaKeyPair.getPublic()),
                "Verification should succeed for signature generated from string data.");

        
        assertTrue(signatureHex.matches("[0-9A-F]+"), "Signature must be uppercase hexadecimal.");
    }

    @Test
    void testSignFileAndVerify() throws Exception {
        Path inputFile = createTestFile("input_data.txt", TEST_BYTES);
        String signatureHex = dsaService.signFile(inputFile, dsaKeyPair.getPrivate());

        assertTrue(dsaService.verifyFileSignature(inputFile, signatureHex, dsaKeyPair.getPublic()),
                "Verification should succeed for signature generated from file data.");

        
        assertTrue(signatureHex.matches("[0-9A-F]+"), "Signature must be uppercase hexadecimal.");
    }

    @Test
    void testVerifyFileSignature_failureDueToTamperedSignature() throws Exception {
        Path inputFile = createTestFile("verify_fail.txt", TEST_BYTES);
        String correctSignatureHex = dsaService.signFile(inputFile, dsaKeyPair.getPrivate());

        
        String tamperedSignatureHex = correctSignatureHex.substring(0, correctSignatureHex.length() - 1) +
                (correctSignatureHex.endsWith("F") ? "E" : "F");

        assertFalse(dsaService.verifyFileSignature(inputFile, tamperedSignatureHex, dsaKeyPair.getPublic()),
                "Verification should fail if signature is tampered.");
    }

    @Test
    void testVerifyFileSignature_failureDueToWrongKey() throws Exception {
        Path inputFile = createTestFile("verify_wrong_key.txt", TEST_BYTES);
        String correctSignatureHex = dsaService.signFile(inputFile, dsaKeyPair.getPrivate());

        
        KeyPair wrongKeyPair = dsaService.generateKeyPair(KEY_SIZE);

        assertFalse(dsaService.verifyFileSignature(inputFile, correctSignatureHex, wrongKeyPair.getPublic()),
                "Verification should fail if the wrong public key is used.");
    }

    

    @Test
    void testGetFileHash_SHA256() throws Exception {
        Path inputFile = createTestFile("hash_file.txt", "HashMe".getBytes());

        
        String expectedHash = "45ACBDA84F508B46843A63ED41F5FCA3B6B7923954DCA0AD5E15E978443E980A";

        String actualHash = dsaService.getFileHash(inputFile);

        assertEquals(expectedHash, actualHash, "Generated SHA-256 hash must match expected value.");
    }

    

    @Test
    void testGetPublicKeyFromFile_PEMFormatted() throws Exception {
        
        String base64Key = Base64.getEncoder().encodeToString(dsaKeyPair.getPublic().getEncoded());

        
        String pemKey = PUBLIC_HEADER + "\n" + base64Key + "\n" + PUBLIC_FOOTER;

        Path keyFile = createTestFile("public.pem", pemKey.getBytes());

        PublicKey loadedKey = dsaService.getPublicKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertEquals("DSA", loadedKey.getAlgorithm());
        assertArrayEquals(dsaKeyPair.getPublic().getEncoded(), loadedKey.getEncoded(), "Loaded public key must match original key bytes.");
    }

    @Test
    void testGetPrivateKeyFromFile_PEMFormatted() throws Exception {
        
        String base64Key = Base64.getEncoder().encodeToString(dsaKeyPair.getPrivate().getEncoded());

        
        String pemKey = PRIVATE_HEADER + "\n" + base64Key + "\n" + PRIVATE_FOOTER;

        Path keyFile = createTestFile("private.pem", pemKey.getBytes());

        PrivateKey loadedKey = dsaService.getPrivateKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertEquals("DSA", loadedKey.getAlgorithm());
        assertArrayEquals(dsaKeyPair.getPrivate().getEncoded(), loadedKey.getEncoded(), "Loaded private key must match original key bytes.");
    }

    @Test
    void testGetPrivateKeyFromFile_HandlesWhitespaceAndNewlines() throws Exception {
        String base64Key = Base64.getEncoder().encodeToString(dsaKeyPair.getPrivate().getEncoded());

        
        String messyPemKey = PRIVATE_HEADER + " \n" +
                base64Key.substring(0, 10) + " \t\n" +
                base64Key.substring(10) + "\n" +
                PRIVATE_FOOTER + " ";

        Path keyFile = createTestFile("messy_private.pem", messyPemKey.getBytes());
        PrivateKey loadedKey = dsaService.getPrivateKeyFromFile(keyFile);

        assertNotNull(loadedKey);
        assertArrayEquals(dsaKeyPair.getPrivate().getEncoded(), loadedKey.getEncoded(), "Key loading must handle embedded whitespace and headers.");
    }
}