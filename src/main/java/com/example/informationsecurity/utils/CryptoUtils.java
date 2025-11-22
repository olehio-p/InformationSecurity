package com.example.informationsecurity.utils;

import java.util.Arrays;

public class CryptoUtils {
    public static final int BLOCK_SIZE = 16;

    public static long bytesToLong(byte[] b, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (b[offset + i] & 0xFF) << (i * 8));
        }
        return value;
    }

    public static void longToBytes(long n, byte[] b, int offset) {
        for (int i = 0; i < 8; i++) {
            b[offset + i] = (byte) ((n >>> (i * 8)) & 0xFF);
        }
    }

    public static void xorBlocks(byte[] block1, byte[] block2, byte[] output) {
        if (block1.length != block2.length || block1.length != output.length) {
            throw new IllegalArgumentException("Blocks must have the same length.");
        }
        for (int i = 0; i < block1.length; i++) {
            output[i] = (byte) (block1[i] ^ block2[i]);
        }
    }

    public static byte[] pad(byte[] data) {
        int paddingSize = BLOCK_SIZE - (data.length % BLOCK_SIZE);
        byte[] paddedData = new byte[data.length + paddingSize];
        System.arraycopy(data, 0, paddedData, 0, data.length);
        Arrays.fill(paddedData, data.length, paddedData.length, (byte) paddingSize);
        return paddedData;
    }


    public static byte[] unpad(byte[] paddedData) {
        if (paddedData.length == 0) return new byte[0];
        int paddingSize = paddedData[paddedData.length - 1] & 0xFF;
        if (paddingSize > BLOCK_SIZE || paddingSize == 0 || paddingSize > paddedData.length) {
            throw new IllegalArgumentException("Invalid padding size during unpadding.");
        }
        return Arrays.copyOfRange(paddedData, 0, paddedData.length - paddingSize);
    }
}