package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.RC5OperationRequest;
import com.example.informationsecurity.services.RC5ModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("/lab3/rc5")
@RequiredArgsConstructor
public class RC5Controller {
    private static final String ENCRYPT_VIEW = "rc5-encrypt";
    private static final String DECRYPT_VIEW = "rc5-decrypt";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";

    private final RC5ModeService rc5ModeService;

    @GetMapping("/encrypt")
    public String encryptPage(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new RC5OperationRequest(64, 20, 16, "", "", ""));
        }
        return ENCRYPT_VIEW;
    }

    @GetMapping("/decrypt")
    public String decryptPage(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new RC5OperationRequest(64, 20, 16, "", "", ""));
        }
        return DECRYPT_VIEW;
    }

    @PostMapping("/encrypt")
    public String encrypt(
            @ModelAttribute("request") RC5OperationRequest request,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty() || request.getPassword().isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please provide a file and a password.");
            return "redirect:/lab3/rc5/encrypt";
        }

        try {
            Path tempInput = Files.createTempFile("rc5_in_", file.getOriginalFilename());
            file.transferTo(tempInput.toFile());

            String defaultOutputName = file.getOriginalFilename() + ".rc5";
            Path outputFilePath = saveToFile(request, defaultOutputName);

            rc5ModeService.encryptFile(tempInput, outputFilePath,
                    request.getPassword(), request.getW(), request.getR(), request.getB());

            Files.deleteIfExists(tempInput);

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE,
                    "File encrypted successfully to: " + outputFilePath.toString());

            return "redirect:/lab3/rc5/encrypt";

        } catch (Exception e) {
            log.error("RC5 Encryption Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Encryption failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("request", request);
            return "redirect:/lab3/rc5/encrypt";
        }
    }

    private Path saveToFile(@ModelAttribute("request") RC5OperationRequest request, String defaultOutputName) throws IOException {
        String outputFileName = request.getSaveName() != null && !request.getSaveName().trim().isEmpty()
                ? request.getSaveName() : defaultOutputName;

        String saveDirectory = request.getSaveDirectory() != null ? request.getSaveDirectory() : "Downloads";
        Path outputDir = Paths.get(System.getProperty("user.home"), saveDirectory);
        Files.createDirectories(outputDir);

        return outputDir.resolve(outputFileName);
    }

    @PostMapping("/decrypt")
    public String decrypt(
            @ModelAttribute("request") RC5OperationRequest request,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty() || request.getPassword().isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please provide a file and a password.");
            return "redirect:/lab3/rc5/decrypt";
        }

        try {
            Path tempInput = Files.createTempFile("rc5_enc_", file.getOriginalFilename());
            file.transferTo(tempInput.toFile());

            String defaultOutputName = Objects.requireNonNull(file.getOriginalFilename()).replaceFirst("\\.rc5$", "");
            Path outputFilePath = saveToFile(request, defaultOutputName);

            rc5ModeService.decryptFile(tempInput, outputFilePath,
                    request.getPassword(), request.getW(), request.getR(), request.getB());

            Files.deleteIfExists(tempInput);

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE,
                    "File decrypted successfully to: " + outputFilePath);

            return "redirect:/lab3/rc5/decrypt";

        } catch (Exception e) {
            log.error("RC5 Decryption Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Decryption failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("request", request);
            return "redirect:/lab3/rc5/decrypt";
        }
    }
}