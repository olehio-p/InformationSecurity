package com.example.informationsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RandomGeneratorResponse {
    private List<Long> numbers;
    private String filePath;
    private Integer generatedCount;
    private String message;
}
