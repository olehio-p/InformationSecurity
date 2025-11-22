package com.example.informationsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MD5HashRequest {
    @NotBlank(message = "Input text cannot be empty")
    private String inputText;

    private boolean saveToFile;
    private String saveName;
    private String saveLocation;
}