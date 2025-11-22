// src/main/java/com/example/informationsecurity/services/RC5KeyService.java

package com.example.informationsecurity.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class RC5KeyService {
    private final MD5Service md5Service;
    private static final int MD5_HASH_SIZE_BYTES = 16;

    public byte[] deriveKey(String password, int keyLengthBytes) throws IOException {
        String hashP = md5Service.hashString(password);
        byte[] hashPBytes = hexStringToByteArray(hashP);

        if (keyLengthBytes == 8) {
            return Arrays.copyOfRange(hashPBytes, MD5_HASH_SIZE_BYTES - 8, MD5_HASH_SIZE_BYTES);
        } else if (keyLengthBytes == 32) {
            String hashOfHashP = md5Service.hashString(hashP);
            byte[] hashOfHashPBytes = hexStringToByteArray(hashOfHashP);

            byte[] fullKey = new byte[32];
            System.arraycopy(hashOfHashPBytes, 0, fullKey, 0, MD5_HASH_SIZE_BYTES);
            System.arraycopy(hashPBytes, 0, fullKey, MD5_HASH_SIZE_BYTES, MD5_HASH_SIZE_BYTES);
            return fullKey;
        } else if (keyLengthBytes == 16) {
            return hashPBytes;
        }

        return Arrays.copyOf(hashPBytes, Math.min(keyLengthBytes, MD5_HASH_SIZE_BYTES));
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}