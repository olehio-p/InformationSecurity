package com.example.informationsecurity.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class DSAService {
    private static final String ALGORITHM = "DSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withDSA";

    public KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    public byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    public String signFile(Path filePath, PrivateKey privateKey) throws Exception {
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] signatureBytes = signData(fileBytes, privateKey);
        return HexFormat.of().formatHex(signatureBytes).toUpperCase();
    }

    public String signString(String input, PrivateKey privateKey) throws Exception {
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] signatureBytes = signData(inputBytes, privateKey);
        return HexFormat.of().formatHex(signatureBytes).toUpperCase();
    }

    public boolean verifyFileSignature(Path filePath, String signatureHex, PublicKey publicKey) throws Exception {
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] signatureBytes = HexFormat.of().parseHex(signatureHex);

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(fileBytes);
        return signature.verify(signatureBytes);
    }

    public String getFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hash = digest.digest(fileBytes);
        return HexFormat.of().formatHex(hash).toUpperCase();
    }

    public PrivateKey getPrivateKeyFromFile(Path filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(filePath);
        String keyString = new String(keyBytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        keyBytes = Base64.getDecoder().decode(keyString);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(spec);
    }

    public PublicKey getPublicKeyFromFile(Path filePath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(filePath);
        String keyString = new String(keyBytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        keyBytes = Base64.getDecoder().decode(keyString);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(spec);
    }
}