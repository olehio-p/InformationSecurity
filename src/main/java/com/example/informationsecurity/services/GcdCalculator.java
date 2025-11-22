package com.example.informationsecurity.services;

import org.springframework.stereotype.Component;


@Component
public class GcdCalculator {

    public long calculateGCD(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);

        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }

        return a;
    }
}