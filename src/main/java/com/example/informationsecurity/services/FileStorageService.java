package com.example.informationsecurity.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class FileStorageService {

    private static final String OUTPUT_DIRECTORY = "GeneratedNumbers";
    @Value("${file.storage.directory:/uploads/md5_hashes/}")
    private String baseStorageDirectory;

    public FileStorageService() {
        File directory = new File(OUTPUT_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public String saveNumbersToFile(List<Long> numbers) {
        String fileName = String.format("random_numbers_%s.txt",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        String filePath = OUTPUT_DIRECTORY + File.separator + fileName;

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            for (Long number : numbers) {
                writer.write(number.toString() + "\n");
            }
            return filePath;
        } catch (IOException e) {
            log.error("Error saving file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save numbers to file", e);
        }
    }

    public String saveNumbersToFile(List<Long> numbers, String directory, String fileName) {
        try {
            Path dirPath = Paths.get(System.getProperty("user.home"), "Downloads", directory);
            Files.createDirectories(dirPath);
            Path filePath = dirPath.resolve(fileName);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                for (Long number : numbers) {
                    writer.write(number.toString());
                    writer.newLine();
                }
            }

            return filePath.toString();
        } catch (IOException e) {
            log.error("Error saving file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save numbers to file", e);
        }
    }

    public String saveMD5Hash(String hash, String saveName, String saveDirectory) throws IOException {
        String defaultFileName = "hash_output.md5";
        String fileName = (saveName != null && !saveName.trim().isEmpty()) ? saveName : defaultFileName;

        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");
        if (!fileName.toLowerCase().endsWith(".md5")) {
            fileName += ".md5";
        }

        String targetDirectory = (saveDirectory != null && !saveDirectory.trim().isEmpty())
                ? saveDirectory
                : baseStorageDirectory;

        targetDirectory = targetDirectory.replaceAll("[^a-zA-Z0-9._/-]", "");

        Path dirPath;
        if (targetDirectory.startsWith("/")) {
            dirPath = Paths.get(targetDirectory);
        } else {
            dirPath = Paths.get(System.getProperty("user.home"), "Downloads", targetDirectory);
        }

        Files.createDirectories(dirPath);

        Path filePath = dirPath.resolve(fileName);

        Files.writeString(filePath, hash, StandardCharsets.UTF_8);

        return filePath.toAbsolutePath().toString();
    }
}