package com.example.informationsecurity.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LCGService {
    private final RandomNumberGenerator randomNumberGenerator;
    @Value("${rc5.iv.lcg.modulus:2147483647}")
    private long modulus;

    @Value("${rc5.iv.lcg.multiplier:16807}")
    private long multiplier;

    @Value("${rc5.iv.lcg.increment:0}")
    private long increment;

    private static final int IV_NUMBERS_COUNT = 2;

    public byte[] generateIV(int bytesCount) {
        if (bytesCount != 16) {
            throw new IllegalArgumentException("IV size must be 16 bytes for RC5 w=64.");
        }

        long dynamicSeed = System.currentTimeMillis();

        List<Long> randomNumbers = randomNumberGenerator.generateLehmerNumbers(
                dynamicSeed % modulus, multiplier, increment, modulus, IV_NUMBERS_COUNT);

        byte[] iv = new byte[bytesCount];

        for (int i = 0; i < IV_NUMBERS_COUNT; i++) {
            long randomValue = randomNumbers.get(i);
            for (int j = 0; j < 8; j++) {
                iv[i * 8 + j] = (byte) ((randomValue >>> (j * 8)) & 0xFF);
            }
        }
        return iv;
    }
}