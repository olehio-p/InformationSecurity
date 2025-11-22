package com.example.informationsecurity.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;

@Slf4j
@Service
public class MD5Service {

    private static final int[] SHIFT_AMOUNTS = {
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    };

    private static final int[] TABLE_T = new int[64];

    static {
        for (int i = 0; i < 64; i++) {
            TABLE_T[i] = (int) (long) ((1L << 32) * Math.abs(Math.sin(i + 1)));
        }
    }

    public String hashString(String input) throws IOException {
        byte[] inputBytes = input.getBytes();
        return bytesToHex(computeMD5(new ByteArrayInputStream(inputBytes)));
    }

    public String hashFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return bytesToHex(computeMD5(is));
        }
    }

    public String hashFile(Path filePath) throws IOException {
        return hashFile(filePath.toFile());
    }

    public boolean verifyFileIntegrity(File file, String expectedHash) throws IOException {
        String actualHash = hashFile(file);
        return actualHash.equalsIgnoreCase(expectedHash.trim());
    }

    private byte[] computeMD5(InputStream is) throws IOException {
        int[] state = {0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476};

        byte[] block = new byte[64];
        long totalLength = 0;
        int bytesRead;

        while ((bytesRead = is.read(block)) == 64) {
            processBlock(block, state);
            totalLength += 64;
        }

        if (bytesRead == -1) {
            bytesRead = 0;
        } else {
            totalLength += bytesRead;
        }

        byte[] padBlock = new byte[64];
        if (bytesRead > 0) {
            System.arraycopy(block, 0, padBlock, 0, bytesRead);
        }
        padBlock[bytesRead] = (byte) 0x80;
        int filled = bytesRead + 1;

        if (filled > 56) {
            for (int i = filled; i < 64; i++) {
                padBlock[i] = 0;
            }
            processBlock(padBlock, state);
            padBlock = new byte[64];
            filled = 0;
        }

        for (int i = filled; i < 56; i++) {
            padBlock[i] = 0;
        }

        long bitLength = totalLength * 8;
        for (int i = 0; i < 8; i++) {
            padBlock[56 + i] = (byte) (bitLength >>> (i * 8));
        }

        processBlock(padBlock, state);

        byte[] result = new byte[16];
        intToBytes(state[0], result, 0);
        intToBytes(state[1], result, 4);
        intToBytes(state[2], result, 8);
        intToBytes(state[3], result, 12);

        return result;
    }

    private void processBlock(byte[] block, int[] state) {
        int A = state[0];
        int B = state[1];
        int C = state[2];
        int D = state[3];

        int[] M = new int[16];
        for (int j = 0; j < 16; j++) {
            M[j] = bytesToInt(block, j * 4);
        }

        for (int j = 0; j < 64; j++) {
            int F, g;

            if (j < 16) {
                F = (B & C) | (~B & D);
                g = j;
            } else if (j < 32) {
                F = (D & B) | (~D & C);
                g = (5 * j + 1) % 16;
            } else if (j < 48) {
                F = B ^ C ^ D;
                g = (3 * j + 5) % 16;
            } else {
                F = C ^ (B | ~D);
                g = (7 * j) % 16;
            }

            F = F + A + TABLE_T[j] + M[g];
            A = D;
            D = C;
            C = B;
            B = B + Integer.rotateLeft(F, SHIFT_AMOUNTS[j]);
        }

        state[0] += A;
        state[1] += B;
        state[2] += C;
        state[3] += D;
    }

    private int bytesToInt(byte[] b, int offset) {
        return (b[offset] & 0xFF) |
                ((b[offset + 1] & 0xFF) << 8) |
                ((b[offset + 2] & 0xFF) << 16) |
                ((b[offset + 3] & 0xFF) << 24);
    }

    private void intToBytes(int n, byte[] b, int offset) {
        b[offset] = (byte) (n & 0xFF);
        b[offset + 1] = (byte) ((n >>> 8) & 0xFF);
        b[offset + 2] = (byte) ((n >>> 16) & 0xFF);
        b[offset + 3] = (byte) ((n >>> 24) & 0xFF);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}