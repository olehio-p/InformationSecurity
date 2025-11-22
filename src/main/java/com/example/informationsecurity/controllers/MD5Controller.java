package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.*;
import com.example.informationsecurity.services.FileStorageService;
import com.example.informationsecurity.services.MD5Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Controller
@RequestMapping("/lab2")
@RequiredArgsConstructor
public class MD5Controller {

    private static final String MAIN_PAGE_VIEW = "lab2";
    private static final String TEST_PAGE_VIEW = "md5-test";
    private static final String FILE_HASH_VIEW = "md5-file";
    private static final String VERIFY_VIEW = "md5-verify";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";
    private static final String RESPONSE_ATTRIBUTE = "response";

    private final MD5Service md5Service;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("hashRequest", new MD5HashRequest());
        return MAIN_PAGE_VIEW;
    }

    @PostMapping("/hash-string")
    public String hashString(
            @Valid @ModelAttribute("hashRequest") MD5HashRequest request,
            BindingResult bindingResult,
            @RequestParam(required = false, defaultValue = "false") boolean saveToFile,
            @RequestParam(required = false) String saveName,
            @RequestParam(required = false) String saveDirectory,
            Model model) {

        if (bindingResult.hasErrors()) {
            return MAIN_PAGE_VIEW;
        }

        try {
            long startTime = System.currentTimeMillis();
            String hash = md5Service.hashString(request.getInputText());
            long executionTime = System.currentTimeMillis() - startTime;

            MD5HashResponse response = new MD5HashResponse(
                    request.getInputText(),
                    hash,
                    executionTime
            );

            if (saveToFile) {
                String filename = saveName != null && !saveName.isEmpty() ? saveName : "string_hash.md5";
                if (!filename.toLowerCase().endsWith(".md5")) {
                    filename += ".md5";
                }
                String savePath = fileStorageService.saveMD5Hash(hash, filename, saveDirectory);
                model.addAttribute(SUCCESS_ATTRIBUTE, "Hash computed and saved to " + savePath);
            } else {
                model.addAttribute(SUCCESS_ATTRIBUTE, "Hash computed successfully");
            }

            model.addAttribute("hashRequest", request);
            model.addAttribute(RESPONSE_ATTRIBUTE, response);

            log.info("MD5 hash computed for string (length: {}): {}",
                    request.getInputText().length(), hash);

            return MAIN_PAGE_VIEW;

        } catch (Exception e) {
            log.error("Error computing or saving MD5 hash: {}", e.getMessage(), e);
            model.addAttribute(ERROR_ATTRIBUTE, "Error computing or saving hash: " + e.getMessage());
            return MAIN_PAGE_VIEW;
        }
    }

    @GetMapping("/test")
    public String testPage(Model model) {
        return TEST_PAGE_VIEW;
    }

    @PostMapping("/test")
    public String runTests(Model model) {
        try {
            Map<String, TestResult> testResults = runRFC1321Tests();
            model.addAttribute("testResults", testResults);

            boolean allPassed = testResults.values().stream().allMatch(TestResult::isPassed);
            if (allPassed) {
                model.addAttribute(SUCCESS_ATTRIBUTE, "All RFC 1321 tests passed!");
            } else {
                model.addAttribute(ERROR_ATTRIBUTE, "Some tests failed. See details below.");
            }

            log.info("RFC 1321 tests completed. Pass rate: {}/{}",
                    testResults.values().stream().filter(TestResult::isPassed).count(),
                    testResults.size());

            return TEST_PAGE_VIEW;

        } catch (Exception e) {
            log.error("Error running tests: {}", e.getMessage(), e);
            model.addAttribute(ERROR_ATTRIBUTE, "Error running tests: " + e.getMessage());
            return TEST_PAGE_VIEW;
        }
    }

    @GetMapping("/file-hash")
    public String fileHashPage(Model model) {
        return FILE_HASH_VIEW;
    }

    @PostMapping("/file-hash")
    public String hashFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean saveToFile,
            @RequestParam(required = false) String saveName,
            @RequestParam(required = false) String saveDirectory,
            Model model) {

        if (file.isEmpty()) {
            model.addAttribute(ERROR_ATTRIBUTE, "Please select a file to hash");
            return FILE_HASH_VIEW;
        }

        try {
            Path tempFile = Files.createTempFile("md5_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            long startTime = System.currentTimeMillis();
            String hash = md5Service.hashFile(tempFile);
            long executionTime = System.currentTimeMillis() - startTime;

            MD5FileHashResponse response = new MD5FileHashResponse(
                    file.getOriginalFilename(),
                    hash,
                    file.getSize(),
                    executionTime
            );

            if (saveToFile) {
                String filename = saveName != null && !saveName.isEmpty() ? saveName : file.getOriginalFilename() + ".md5";
                if (!filename.toLowerCase().endsWith(".md5")) {
                    filename += ".md5";
                }
                String savePath = fileStorageService.saveMD5Hash(hash, filename, saveDirectory);
                model.addAttribute(SUCCESS_ATTRIBUTE, "File hash computed and saved to " + savePath);
            } else {
                model.addAttribute(SUCCESS_ATTRIBUTE, "File hash computed successfully");
            }

            model.addAttribute(RESPONSE_ATTRIBUTE, response);

            Files.deleteIfExists(tempFile);

            log.info("MD5 hash computed for file: {} (size: {} bytes): {}",
                    file.getOriginalFilename(), file.getSize(), hash);

            return FILE_HASH_VIEW;

        } catch (IOException e) {
            log.error("Error processing or saving file: {}", e.getMessage(), e);
            model.addAttribute(ERROR_ATTRIBUTE, "Error processing or saving file: " + e.getMessage());
            return FILE_HASH_VIEW;
        }
    }

    @GetMapping("/verify")
    public String verifyPage(Model model) {
        return VERIFY_VIEW;
    }

    @PostMapping("/verify")
    public String verifyFileIntegrity(
            @RequestParam("file") MultipartFile file,
            @RequestParam("hashFile") MultipartFile hashFile,
            Model model) {

        if (file.isEmpty() || hashFile.isEmpty()) {
            model.addAttribute(ERROR_ATTRIBUTE, "Please select both file and hash file");
            return VERIFY_VIEW;
        }

        try {
            String expectedHash = new String(hashFile.getBytes()).trim();

            Path tempFile = Files.createTempFile("verify_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            long startTime = System.currentTimeMillis();
            String actualHash = md5Service.hashFile(tempFile);
            boolean isValid = actualHash.equalsIgnoreCase(expectedHash);
            long executionTime = System.currentTimeMillis() - startTime;

            MD5VerificationResponse response = new MD5VerificationResponse(
                    file.getOriginalFilename(),
                    expectedHash,
                    actualHash,
                    isValid,
                    executionTime
            );

            model.addAttribute(RESPONSE_ATTRIBUTE, response);

            if (isValid) {
                model.addAttribute(SUCCESS_ATTRIBUTE, "File integrity verified successfully!");
            } else {
                model.addAttribute(ERROR_ATTRIBUTE, "File integrity check failed! Hash mismatch.");
            }

            Files.deleteIfExists(tempFile);

            log.info("File verification completed for: {}. Valid: {}",
                    file.getOriginalFilename(), isValid);

            return VERIFY_VIEW;

        } catch (IOException e) {
            log.error("Error verifying file: {}", e.getMessage(), e);
            model.addAttribute(ERROR_ATTRIBUTE, "Error verifying file: " + e.getMessage());
            return VERIFY_VIEW;
        }
    }

    Map<String, TestResult> runRFC1321Tests() {
        Map<String, TestResult> results = new TreeMap<>();

        results.put("Test 1: Empty string",
                testHash("", "D41D8CD98F00B204E9800998ECF8427E"));
        results.put("Test 2: 'a'",
                testHash("a", "0CC175B9C0F1B6A831C399E269772661"));
        results.put("Test 3: 'abc'",
                testHash("abc", "900150983CD24FB0D6963F7D28E17F72"));
        results.put("Test 4: 'message digest'",
                testHash("message digest", "F96B697D7CB7938D525A2F31AAF161D0"));
        results.put("Test 5: 'a-z'",
                testHash("abcdefghijklmnopqrstuvwxyz", "C3FCD3D76192E4007DFB496CCA67E13B"));
        results.put("Test 6: 'A-Za-z0-9'",
                testHash("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
                        "D174AB98D277D9F5A5611C2C9F419D9F"));
        results.put("Test 7: '1234567890' x8",
                testHash("12345678901234567890123456789012345678901234567890123456789012345678901234567890",
                        "57EDF4A22BE3C955AC49DA2E2107B67A"));

        return results;
    }

    private TestResult testHash(String input, String expected) {
        try {
            String actual = md5Service.hashString(input);
            boolean passed = actual.equalsIgnoreCase(expected);
            return new TestResult(input, expected, actual, passed);
        } catch (Exception e) {
            return new TestResult(input, expected, "ERROR: " + e.getMessage(), false);
        }
    }
}