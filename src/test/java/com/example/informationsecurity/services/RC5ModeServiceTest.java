package com.example.informationsecurity.services;

import com.example.informationsecurity.utils.CryptoUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RC5ModeServiceTest {

    @TempDir
    Path tempDir;

    private RC5ModeService rc5ModeService;
    private LCGService lcgService;

    private static final int W = 64;
    private static final int R = 12;
    private static final int B = 16;
    private static final String PASSWORD = "test123";

    @BeforeEach
    void setUp() throws Exception {
        RC5KeyService keyService = mock(RC5KeyService.class);
        lcgService = mock(LCGService.class);

        Mockito.when(keyService.deriveKey(PASSWORD, B)).thenReturn(new byte[B]);

        
        Mockito.when(lcgService.generateIV(CryptoUtils.BLOCK_SIZE))
                .thenReturn("1234567890ABCDEF".getBytes());

        rc5ModeService = new RC5ModeService(keyService, lcgService);
    }


    @Test
    void testEncryptAndDecrypt_partialBlock_padding() throws Exception {
        byte[] inputData = "HELLO WORLD".getBytes(); 

        Path input = tempDir.resolve("plain2.txt");
        Path encrypted = tempDir.resolve("cipher2.bin");
        Path decrypted = tempDir.resolve("plain2_out.txt");

        Files.write(input, inputData);

        rc5ModeService.encryptFile(input, encrypted, PASSWORD, W, R, B);
        rc5ModeService.decryptFile(encrypted, decrypted, PASSWORD, W, R, B);

        byte[] out = Files.readAllBytes(decrypted);
        assertArrayEquals(inputData, out);
    }

    @Test
    void testEncryptFile_writesEncryptedIV() throws Exception {
        byte[] inputData = "ABCDEFGH12345678".getBytes();

        Path input = tempDir.resolve("a.txt");
        Path encrypted = tempDir.resolve("a.enc");
        Files.write(input, inputData);

        rc5ModeService.encryptFile(input, encrypted, PASSWORD, W, R, B);

        byte[] file = Files.readAllBytes(encrypted);

        assertTrue(file.length > CryptoUtils.BLOCK_SIZE);
        assertNotEquals("1234567890ABCDEF".getBytes()[0], file[0],
                "IV must be encrypted, not stored as plaintext");
    }

    @Test
    void testDecryptFile_invalidShortFile() throws Exception {
        Path input = tempDir.resolve("bad.bin");
        Files.write(input, new byte[8]); 

        Path out = tempDir.resolve("bad_out");

        assertThrows(IOException.class, () ->
                rc5ModeService.decryptFile(input, out, PASSWORD, W, R, B));
    }

    @Test
    void testDecryptFile_correctlyUsesIV() throws Exception {
        
        byte[] data = "TESTDATA1234".getBytes();

        Path plain = tempDir.resolve("plain3.txt");
        Path enc = tempDir.resolve("cipher3.bin");
        Path dec = tempDir.resolve("plain3_out.txt");

        Files.write(plain, data);

        rc5ModeService.encryptFile(plain, enc, PASSWORD, W, R, B);

        
        Mockito.when(lcgService.generateIV(anyInt()))
                .thenReturn("1234567890ABCDEF".getBytes());

        rc5ModeService.decryptFile(enc, dec, PASSWORD, W, R, B);

        assertArrayEquals(data, Files.readAllBytes(dec));
    }
}
