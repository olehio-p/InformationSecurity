package com.example.informationsecurity.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TestResult {
    private final String input;
    private final String expected;
    private final String actual;
    private final boolean passed;
}
