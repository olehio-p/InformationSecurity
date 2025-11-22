package com.example.informationsecurity.services;

import com.example.informationsecurity.utils.CryptoUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class RC5Cipher {

    private final int w;
    private final int r;
    private final int b;

    private final long P_64 = 0xb7e151628aed2a6bL;
    private final long Q_64 = 0x9e3779b97f4a7c15L;

    private long[] S;
    private final int T;

    public RC5Cipher(int w, int r, int b) {
        if (w != 64) {
            throw new IllegalArgumentException("RC5 implementation supports only w=64 bit word size.");
        }
        this.w = w;
        this.r = r;
        this.b = b;
        this.T = 2 * (r + 1);
    }

    public void setupKey(byte[] key) {
        int c = b / 8;
        long[] L = new long[c];

        for (int i = 0; i < c; i++) {
            L[i] = CryptoUtils.bytesToLong(key, i * 8);
        }

        S = new long[T];
        S[0] = P_64;
        for (int i = 1; i < T; i++) {
            S[i] = S[i - 1] + Q_64;
        }

        long A = 0, B = 0;
        int i = 0, j = 0;
        int max = 3 * Math.max(T, c);

        for (int k = 0; k < max; k++) {
            A = S[i] = Long.rotateLeft(S[i] + A + B, 3);
            B = L[j] = Long.rotateLeft(L[j] + A + B, (int) (A + B));
            i = (i + 1) % T;
            j = (j + 1) % c;
        }
    }

    public byte[] encryptBlock(byte[] block) {
        long A = CryptoUtils.bytesToLong(block, 0);
        long B = CryptoUtils.bytesToLong(block, 8);

        A += S[0];
        B += S[1];

        for (int i = 1; i <= r; i++) {
            A = Long.rotateLeft(A ^ B, (int) (B % 64)) + S[2 * i];
            B = Long.rotateLeft(B ^ A, (int) (A % 64)) + S[2 * i + 1];
        }

        byte[] output = new byte[16];
        CryptoUtils.longToBytes(A, output, 0);
        CryptoUtils.longToBytes(B, output, 8);
        return output;
    }

    public byte[] decryptBlock(byte[] block) {
        long A = CryptoUtils.bytesToLong(block, 0);
        long B = CryptoUtils.bytesToLong(block, 8);

        for (int i = r; i >= 1; i--) {
            B = Long.rotateRight(B - S[2 * i + 1], (int) (A % 64)) ^ A;
            A = Long.rotateRight(A - S[2 * i], (int) (B % 64)) ^ B;
        }

        B -= S[1];
        A -= S[0];

        byte[] output = new byte[16];
        CryptoUtils.longToBytes(A, output, 0);
        CryptoUtils.longToBytes(B, output, 8);
        return output;
    }
}