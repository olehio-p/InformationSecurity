package com.example.informationsecurity.services;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@Component
public class RandomNumberGenerator {

    public List<Long> generateLehmerNumbers(long seed, long multiplier, long increment, long modulus, int count) {
        List<Long> numbers = new ArrayList<>();
        long current = seed;

        for (int i = 0; i < count; i++) {
            current = (multiplier * current + increment) % modulus;
            numbers.add(Math.abs(current));
        }

        return numbers;
    }

    public List<Long> generateSystemRandomNumbers(int count) {
        Random random = new Random();
        List<Long> numbers = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            numbers.add((long) random.nextInt(Integer.MAX_VALUE - 1) + 1);
        }

        return numbers;
    }
}