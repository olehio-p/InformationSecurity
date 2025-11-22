package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.ComparisonResult;
import com.example.informationsecurity.dto.RSAKeyGenRequest;
import com.example.informationsecurity.services.CryptoComparisonService;
import com.example.informationsecurity.services.RSAService;
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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("/lab4/rsa")
@RequiredArgsConstructor
public class RSAController {

    private static final String KEYGEN_VIEW = "rsa-keygen";
    private static final String CRYPTO_VIEW = "rsa-crypto";
    private static final String COMPARE_VIEW = "rsa-compare";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";

    private final RSAService rsaService;
    private final CryptoComparisonService comparisonService;

    @GetMapping("/keygen")
    public String keygenPage(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new RSAKeyGenRequest(2048, "rsa_key", "Downloads/RSA_Keys"));
        }
        return KEYGEN_VIEW;
    }

    @PostMapping("/generate-keys")
    public String generateKeys(
            @ModelAttribute("request") RSAKeyGenRequest request, // Приймаємо DTO
            RedirectAttributes redirectAttributes) {

        int keySize = request.getKeySize();
        String prefix = request.getKeyPrefix().replaceAll("[^a-zA-Z0-9._-]", "");
        String relativeDir = request.getSaveDirectory() != null && !request.getSaveDirectory().trim().isEmpty()
                ? request.getSaveDirectory() : "Downloads/RSA_Keys";

        try {
            KeyPair keyPair = rsaService.generateKeyPair(keySize);

            Path dirPath = Paths.get(System.getProperty("user.home"), relativeDir);
            Files.createDirectories(dirPath);

            String pubKeyContent = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) +
                    "\n-----END PUBLIC KEY-----";
            Path pubKeyPath = dirPath.resolve(String.format("%s_%d_public.pem", prefix, keySize));
            Files.writeString(pubKeyPath, pubKeyContent);

            String privKeyContent = "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()) +
                    "\n-----END PRIVATE KEY-----";
            Path privKeyPath = dirPath.resolve(String.format("%s_%d_private.pem", prefix, keySize));
            Files.writeString(privKeyPath, privKeyContent);

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE,
                    String.format("Successfully generated %d-bit keys with prefix '%s' and saved to: %s",
                            keySize, prefix, dirPath.toAbsolutePath().toString()));

            return "redirect:/lab4/rsa/keygen";

        } catch (Exception e) {
            log.error("Error generating keys: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Key generation failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("request", request);
            return "redirect:/lab4/rsa/keygen";
        }
    }


    @GetMapping("/crypto")
    public String cryptoPage(Model model) {
        return CRYPTO_VIEW;
    }

    @PostMapping("/encrypt")
    public String encryptFile(@RequestParam("file") MultipartFile file,
                              @RequestParam("publicKeyFile") MultipartFile publicKeyFile,
                              @RequestParam(value = "outputFileName", required = false) String customOutputFileName,
                              @RequestParam(value = "saveDirectory", required = false) String customSaveDirectory,
                              RedirectAttributes redirectAttributes) {

        if (file.isEmpty() || publicKeyFile.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please select both data file and public key file.");
            return "redirect:/lab4/rsa/crypto";
        }

        Path tempInput = null;
        Path tempPubKey = null;

        try {
            tempInput = Files.createTempFile("rsa_in_", file.getOriginalFilename());
            file.transferTo(tempInput.toFile());

            tempPubKey = Files.createTempFile("rsa_pub_", ".pem");
            publicKeyFile.transferTo(tempPubKey.toFile());

            PublicKey publicKey = rsaService.getPublicKeyFromFile(tempPubKey);

            String defaultOutputName = file.getOriginalFilename() + ".rsa_enc";
            String finalOutputName = (customOutputFileName != null && !customOutputFileName.trim().isEmpty())
                    ? customOutputFileName.trim() : defaultOutputName;

            String saveDir = (customSaveDirectory != null && !customSaveDirectory.trim().isEmpty())
                    ? customSaveDirectory : "Downloads";

            Path outputDir = Paths.get(System.getProperty("user.home"), saveDir);
            Files.createDirectories(outputDir);
            Path outputFilePath = outputDir.resolve(finalOutputName);

            rsaService.encryptFile(tempInput, outputFilePath, publicKey);

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE,
                    "File encrypted successfully to: " + outputFilePath.toAbsolutePath().toString());

            return "redirect:/lab4/rsa/crypto";

        } catch (Exception e) {
            log.error("RSA Encryption Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Encryption failed: " + e.getMessage());
            return "redirect:/lab4/rsa/crypto";
        } finally {
            try {
                if (tempInput != null) Files.deleteIfExists(tempInput);
                if (tempPubKey != null) Files.deleteIfExists(tempPubKey);
            } catch (IOException cleanupException) {
                log.warn("Failed to delete temporary files: {}", cleanupException.getMessage());
            }
        }
    }

    @PostMapping("/decrypt")
    public String decryptFile(@RequestParam("file") MultipartFile file,
                              @RequestParam("privateKeyFile") MultipartFile privateKeyFile,
                              @RequestParam(value = "outputFileName", required = false) String customOutputFileName,
                              @RequestParam(value = "saveDirectory", required = false) String customSaveDirectory,
                              RedirectAttributes redirectAttributes) {

        if (file.isEmpty() || privateKeyFile.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please select both encrypted file and private key file.");
            return "redirect:/lab4/rsa/crypto";
        }

        Path tempInput = null;
        Path tempPrivKey = null;

        try {
            tempInput = Files.createTempFile("rsa_enc_", file.getOriginalFilename());
            file.transferTo(tempInput.toFile());

            tempPrivKey = Files.createTempFile("rsa_priv_", ".pem");
            privateKeyFile.transferTo(tempPrivKey.toFile());

            PrivateKey privateKey = rsaService.getPrivateKeyFromFile(tempPrivKey);

            String defaultDecryptedName = Objects.requireNonNull(file.getOriginalFilename()).replaceFirst("\\.rsa_enc$", "");
            if (defaultDecryptedName.isEmpty()) {
                defaultDecryptedName = "decrypted_file";
            }

            String finalOutputName = (customOutputFileName != null && !customOutputFileName.trim().isEmpty())
                    ? customOutputFileName.trim() : defaultDecryptedName;

            String saveDir = (customSaveDirectory != null && !customSaveDirectory.trim().isEmpty())
                    ? customSaveDirectory : "Downloads";

            Path outputDir = Paths.get(System.getProperty("user.home"), saveDir);
            Files.createDirectories(outputDir);
            Path outputFilePath = outputDir.resolve(finalOutputName);

            rsaService.decryptFile(tempInput, outputFilePath, privateKey);

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE,
                    "File decrypted successfully to: " + outputFilePath.toAbsolutePath().toString());

            return "redirect:/lab4/rsa/crypto";

        } catch (Exception e) {
            log.error("RSA Decryption Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Decryption failed: " + e.getMessage());
            return "redirect:/lab4/rsa/crypto";
        } finally {
            try {
                if (tempInput != null) Files.deleteIfExists(tempInput);
                if (tempPrivKey != null) Files.deleteIfExists(tempPrivKey);
            } catch (IOException cleanupException) {
                log.warn("Failed to delete temporary files: {}", cleanupException.getMessage());
            }
        }
    }

    @GetMapping("/compare")
    public String comparePage(Model model) {
        if (!model.containsAttribute("keySize")) {
            model.addAttribute("keySize", 2048);
        }
        return COMPARE_VIEW;
    }

    @PostMapping("/compare-speeds")
    public String compareSpeeds(@RequestParam("file") MultipartFile file,
                                @RequestParam("rsaKeySize") int rsaKeySize,
                                RedirectAttributes redirectAttributes) {
        try {
            Path tempInput = Files.createTempFile("compare_in_", file.getOriginalFilename());
            file.transferTo(tempInput.toFile());

            ComparisonResult result = comparisonService.compareSpeeds(tempInput, rsaKeySize);

            Files.deleteIfExists(tempInput);

            redirectAttributes.addFlashAttribute("result", result);
            redirectAttributes.addFlashAttribute("rsaKeySize", rsaKeySize);
            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE, "Comparison completed successfully!");

            return "redirect:/lab4/rsa/compare";

        } catch (Exception e) {
            log.error("Comparison Error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Comparison failed: " + e.getMessage());
            return "redirect:/lab4/rsa/compare";
        }
    }
}