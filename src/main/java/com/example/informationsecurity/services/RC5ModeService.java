package com.example.informationsecurity.services;

import com.example.informationsecurity.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class RC5ModeService {
    private final RC5KeyService keyService;
    private final LCGService lcgService;

    public void encryptFile(Path inputFile, Path outputFile, String password, int w, int r, int b) throws Exception {
        RC5Cipher cipher = new RC5Cipher(w, r, b);
        byte[] key = keyService.deriveKey(password, b);
        cipher.setupKey(key);

        byte[] iv = lcgService.generateIV(CryptoUtils.BLOCK_SIZE);

        byte[] encryptedIV = cipher.encryptBlock(iv);

        Files.write(outputFile, encryptedIV, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        try (InputStream is = Files.newInputStream(inputFile);
             OutputStream os = Files.newOutputStream(outputFile, StandardOpenOption.APPEND)) {

            byte[] currentIV = Arrays.copyOf(iv, iv.length);
            byte[] buffer = new byte[CryptoUtils.BLOCK_SIZE];
            int bytesRead;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while ((bytesRead = is.read(buffer)) != -1) {
                if (bytesRead < CryptoUtils.BLOCK_SIZE) {
                    byte[] lastBlock = Arrays.copyOf(buffer, bytesRead);
                    lastBlock = CryptoUtils.pad(lastBlock);
                    System.arraycopy(lastBlock, 0, buffer, 0, CryptoUtils.BLOCK_SIZE);

                    byte[] xorBlock = new byte[CryptoUtils.BLOCK_SIZE];
                    CryptoUtils.xorBlocks(buffer, currentIV, xorBlock);
                    byte[] cipherBlock = cipher.encryptBlock(xorBlock);

                    os.write(cipherBlock);
                    currentIV = cipherBlock;

                    break;
                }

                byte[] xorBlock = new byte[CryptoUtils.BLOCK_SIZE];
                CryptoUtils.xorBlocks(buffer, currentIV, xorBlock);
                byte[] cipherBlock = cipher.encryptBlock(xorBlock);

                os.write(cipherBlock);
                currentIV = cipherBlock;
            }
        }
        log.info("File encrypted successfully in RC5-CBC-Pad mode.");
    }

    public void decryptFile(Path inputFile, Path outputFile, String password, int w, int r, int b) throws Exception {
        RC5Cipher cipher = new RC5Cipher(w, r, b);
        byte[] key = keyService.deriveKey(password, b);
        cipher.setupKey(key);

        long fileSize = Files.size(inputFile);
        if (fileSize < CryptoUtils.BLOCK_SIZE) {
            throw new IOException("Encrypted file is too short (less than one block).");
        }

        byte[] encryptedIV = new byte[CryptoUtils.BLOCK_SIZE];
        try (InputStream is = Files.newInputStream(inputFile)) {
            if (is.read(encryptedIV) != CryptoUtils.BLOCK_SIZE) {
                throw new IOException("Failed to read the encrypted IV block.");
            }
        }
        byte[] iv = cipher.decryptBlock(encryptedIV);

        try (InputStream is = Files.newInputStream(inputFile);
             OutputStream os = Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            is.skip(CryptoUtils.BLOCK_SIZE);
            long dataLength = fileSize - CryptoUtils.BLOCK_SIZE;

            byte[] previousCipherBlock = encryptedIV;
            byte[] currentCipherBlock = new byte[CryptoUtils.BLOCK_SIZE];
            byte[] decryptedBlock;
            byte[] currentIV;

            ByteArrayOutputStream decryptedDataBuffer = new ByteArrayOutputStream();

            for (long i = 0; i < dataLength / CryptoUtils.BLOCK_SIZE; i++) {
                if (is.read(currentCipherBlock) != CryptoUtils.BLOCK_SIZE) {
                    throw new IOException("Unexpected end of file while reading data blocks.");
                }

                decryptedBlock = cipher.decryptBlock(currentCipherBlock);

                currentIV = (i == 0) ? iv : previousCipherBlock;

                CryptoUtils.xorBlocks(decryptedBlock, currentIV, decryptedBlock);
                previousCipherBlock = Arrays.copyOf(currentCipherBlock, currentCipherBlock.length);
                decryptedDataBuffer.write(decryptedBlock);
            }

            byte[] finalData = CryptoUtils.unpad(decryptedDataBuffer.toByteArray());
            os.write(finalData);
        }
        log.info("File decrypted successfully in RC5-CBC-Pad mode.");
    }
}