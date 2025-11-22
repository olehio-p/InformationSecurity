package com.example.informationsecurity.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class RSAService {
    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    public KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    public void encryptFile(Path inputFile, Path outputFile, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] inputBytes = Files.readAllBytes(inputFile);

        int keySizeInBytes;

        if (publicKey.getAlgorithm().equalsIgnoreCase("RSA") && publicKey instanceof RSAPublicKey rsaKey) {
            keySizeInBytes = rsaKey.getModulus().toByteArray().length;

            if (rsaKey.getModulus().toByteArray()[0] == 0x00) {
                keySizeInBytes--;
            }
        } else {
            keySizeInBytes = 256;
        }

        int maxInputBlockSize = keySizeInBytes - 11;

        if (maxInputBlockSize <= 0) {
            throw new IllegalStateException("Calculated maximum input block size is non-positive (" + maxInputBlockSize + "). Key initialization error or unsupported key size.");
        }


        int inputLength = inputBytes.length;
        int offset = 0;

        try (var os = Files.newOutputStream(outputFile)) {
            while (offset < inputLength) {
                int inputLen = Math.min(inputLength - offset, maxInputBlockSize);

                if (inputLen <= 0) break;

                byte[] buffer = cipher.doFinal(inputBytes, offset, inputLen);

                os.write(buffer);

                offset += inputLen;
            }
        }
    }

    public void decryptFile(Path inputFile, Path outputFile, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] inputBytes = Files.readAllBytes(inputFile);

        int inputBlockSize;

        if (privateKey.getAlgorithm().equalsIgnoreCase("RSA") && privateKey instanceof RSAPrivateKey rsaKey) {
            inputBlockSize = rsaKey.getModulus().toByteArray().length;

            if (rsaKey.getModulus().toByteArray()[0] == 0x00) {
                inputBlockSize--;
            }
        } else {
            inputBlockSize = 256;
        }

        if (inputBlockSize <= 0) {
            throw new IllegalStateException("Calculated input block size is non-positive. Key size calculation error.");
        }


        int inputLength = inputBytes.length;
        int offset = 0;

        try (var os = Files.newOutputStream(outputFile)) {
            while (offset < inputLength) {

                int inputLen = Math.min(inputLength - offset, inputBlockSize);

                if (inputLength - offset > 0 && inputLength - offset < inputBlockSize) {
                    throw new IllegalBlockSizeException(
                            "Encrypted data length is not a multiple of the RSA block size (required: " + inputBlockSize + " bytes). Data corruption suspected."
                    );
                }
                byte[] buffer = cipher.doFinal(inputBytes, offset, inputLen);

                os.write(buffer);
                offset += inputLen;
            }
        }
    }


    public PublicKey getPublicKeyFromFile(Path filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(filePath);
        if (new String(keyBytes).contains("PUBLIC KEY")) {
            String keyString = new String(keyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            keyBytes = Base64.getDecoder().decode(keyString);
        }
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(spec);
    }

    public PrivateKey getPrivateKeyFromFile(Path filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(filePath);
        if (new String(keyBytes).contains("PRIVATE KEY")) {
            String keyString = new String(keyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            keyBytes = Base64.getDecoder().decode(keyString);
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(spec);
    }
}