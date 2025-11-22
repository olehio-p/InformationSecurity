package com.example.informationsecurity.services;

import com.example.informationsecurity.dto.ComparisonResult;
import com.example.informationsecurity.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoComparisonService {
    private final RC5KeyService rc5KeyService;
    private final RSAService rsaService;

    private static final int RC5_W = 64;
    private static final int RC5_R = 20;
    private static final int RC5_B = 16;


    public ComparisonResult compareSpeeds(Path tempFile, int rsaKeySize) throws Exception {
        long fileSize = Files.size(tempFile);

        KeyPair rsaKeys = rsaService.generateKeyPair(rsaKeySize);
        long rsaTimeMs = measureRSATime(tempFile, rsaKeys.getPublic());

        String testPassword = "RC5_Test_Password";
        long rc5TimeMs = measureRC5Time(tempFile, testPassword);

        String winner = rsaTimeMs < rc5TimeMs ? "RSA" : "RC5";

        double rsaSpeed = (double) fileSize / rsaTimeMs;
        double rc5Speed = (double) fileSize / rc5TimeMs;

        return ComparisonResult.builder()
                .rc5TimeMs(rc5TimeMs)
                .rsaTimeMs(rsaTimeMs)
                .winner(winner)
                .fileSizeBytes(fileSize)
                .rc5SpeedBps(rc5Speed * 1000)
                .rsaSpeedBps(rsaSpeed * 1000)
                .build();
    }

    long measureRSATime(Path inputFile, PublicKey publicKey) throws Exception {
        Path tempEncrypted = Files.createTempFile("rsa_enc_temp", ".bin");
        long startTime = System.currentTimeMillis();

        rsaService.encryptFile(inputFile, tempEncrypted, publicKey);

        long endTime = System.currentTimeMillis();
        Files.deleteIfExists(tempEncrypted);
        return endTime - startTime;
    }

    long measureRC5Time(Path inputFile, String password) throws Exception {
        byte[] fileData = Files.readAllBytes(inputFile);
        byte[] key = rc5KeyService.deriveKey(password, RC5_B);
        RC5Cipher cipher = new RC5Cipher(RC5_W, RC5_R, RC5_B);
        cipher.setupKey(key);

        long startTime = System.currentTimeMillis();

        byte[] paddedData = CryptoUtils.pad(fileData);
        byte[] iv = new byte[CryptoUtils.BLOCK_SIZE];
        byte[] currentIV = iv;
        byte[] buffer = new byte[CryptoUtils.BLOCK_SIZE];

        for (int i = 0; i < paddedData.length; i += CryptoUtils.BLOCK_SIZE) {
            System.arraycopy(paddedData, i, buffer, 0, CryptoUtils.BLOCK_SIZE);

            CryptoUtils.xorBlocks(buffer, currentIV, buffer);

            byte[] cipherBlock = cipher.encryptBlock(buffer);
            currentIV = cipherBlock;
        }

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}