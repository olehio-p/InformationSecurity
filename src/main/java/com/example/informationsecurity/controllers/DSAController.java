package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.DSAKeyGenRequest;
import com.example.informationsecurity.services.DSAService;
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

@Slf4j
@Controller
@RequestMapping("/lab5/dsa")
@RequiredArgsConstructor
public class DSAController {

    private static final String KEYGEN_VIEW = "dsa-keygen";
    private static final String SIGN_VIEW = "dsa-sign";
    private static final String VERIFY_VIEW = "dsa-verify";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";

    private final DSAService dsaService;

    // --- 1. Генерація Ключів ---

    @GetMapping("/keygen")
    public String keygenPage(Model model) {
        if (!model.containsAttribute("request")) {
            model.addAttribute("request", new DSAKeyGenRequest(2048, "dsa_key", "Downloads/DSA_Keys"));
        }
        return KEYGEN_VIEW;
    }

    @PostMapping("/generate-keys")
    public String generateKeys(
            @ModelAttribute("request") DSAKeyGenRequest request,
            RedirectAttributes redirectAttributes) {

        int keySize = request.getKeySize();
        String prefix = request.getKeyPrefix().replaceAll("[^a-zA-Z0-9._-]", "");
        String relativeDir = request.getSaveDirectory() != null && !request.getSaveDirectory().trim().isEmpty()
                ? request.getSaveDirectory() : "Downloads/DSA_Keys";

        try {
            KeyPair keyPair = dsaService.generateKeyPair(keySize);

            // 1. Формування шляху збереження
            Path dirPath = Paths.get(System.getProperty("user.home"), relativeDir);
            Files.createDirectories(dirPath);

            // 2. Збереження Public Key (X.509)
            String pubKeyContent = "-----BEGIN PUBLIC KEY-----\n" +
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) +
                    "\n-----END PUBLIC KEY-----";
            Path pubKeyPath = dirPath.resolve(String.format("%s_%d_public.pem", prefix, keySize));
            Files.writeString(pubKeyPath, pubKeyContent);

            // 3. Збереження Private Key (PKCS#8)
            String privKeyContent = "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()) +
                    "\n-----END PRIVATE KEY-----";
            Path privKeyPath = dirPath.resolve(String.format("%s_%d_private.pem", prefix, keySize));
            Files.writeString(privKeyPath, privKeyContent);

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE,
                    String.format("Successfully generated %d-bit DSA keys and saved to: %s",
                            keySize, dirPath.toAbsolutePath().toString()));

            return "redirect:/lab5/dsa/keygen";

        } catch (Exception e) {
            log.error("Error generating DSA keys: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Key generation failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("request", request);
            return "redirect:/lab5/dsa/keygen";
        }
    }

    // --- 2. Підписання (Sign) ---

    @GetMapping("/sign")
    public String signPage(Model model) {
        return SIGN_VIEW;
    }

    @PostMapping("/sign-data")
    public String signData(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "inputText", required = false) String inputText,
            @RequestParam("privateKeyFile") MultipartFile privateKeyFile,
            @RequestParam(value = "saveName", required = false) String saveName,
            @RequestParam(value = "saveDirectory", required = false) String saveDirectory,
            RedirectAttributes redirectAttributes) {

        if (privateKeyFile.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please select the Private Key file.");
            return "redirect:/lab5/dsa/sign";
        }
        if (file.isEmpty() && (inputText == null || inputText.trim().isEmpty())) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please provide a file or text to sign.");
            return "redirect:/lab5/dsa/sign";
        }

        Path tempPrivKey = null;
        Path tempInput = null;
        String signatureHex;
        String dataHash = null;
        String dataType;
        String originalName;

        try {
            tempPrivKey = Files.createTempFile("dsa_priv_", ".pem");
            privateKeyFile.transferTo(tempPrivKey.toFile());
            PrivateKey privateKey = dsaService.getPrivateKeyFromFile(tempPrivKey);

            // --- 1. ПІДПИСАННЯ ДАНИХ І ВИЗНАЧЕННЯ ІМЕНІ ---

            if (!file.isEmpty()) {
                // Підпис файлу
                dataType = "File";
                originalName = file.getOriginalFilename();

                // Створення тимчасового файлу для вмісту
                tempInput = Files.createTempFile("dsa_in_", originalName);
                file.transferTo(tempInput.toFile());

                dataHash = dsaService.getFileHash(tempInput); // Додатковий функціонал: хеш
                signatureHex = dsaService.signFile(tempInput, privateKey);

            } else {
                // Підпис рядка
                dataType = "String";
                originalName = "Input String";
                signatureHex = dsaService.signString(inputText, privateKey);
                // Для рядка хеш не обчислюється явно, оскільки він зашитий у підпис
            }

            // --- 2. ГАРАНТОВАНА ЛОГІКА ЗБЕРЕЖЕННЯ ПІДПИСУ В ФАЙЛ ---

            String baseName = file.isEmpty() ? "string_data" : originalName;

            // a) Визначення кінцевого імені файлу
            String defaultFileName = baseName + ".sig";
            String finalSaveName = (saveName != null && !saveName.trim().isEmpty())
                    ? saveName.trim()
                    : defaultFileName;

            // Додаємо розширення .sig, якщо воно відсутнє
            if (!finalSaveName.toLowerCase().endsWith(".sig")) {
                finalSaveName += ".sig";
            }

            // b) Визначення кінцевої директорії
            String finalDir = (saveDirectory != null && !saveDirectory.trim().isEmpty())
                    ? saveDirectory : "Downloads/DSA_Signatures";

            // c) Формування шляху та запис
            Path saveDirPath = Paths.get(System.getProperty("user.home"), finalDir);
            Files.createDirectories(saveDirPath);
            Path savePath = saveDirPath.resolve(finalSaveName);

            Files.writeString(savePath, signatureHex);

            // --- 3. ПЕРЕДАЧА РЕЗУЛЬТАТІВ У VIEW ---

            redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE, dataType + " successfully signed.");
            redirectAttributes.addFlashAttribute("saveSuccess", "Signature saved to: " + savePath.toAbsolutePath());
            redirectAttributes.addFlashAttribute("originalName", originalName);
            redirectAttributes.addFlashAttribute("signatureHex", signatureHex);
            redirectAttributes.addFlashAttribute("dataHash", dataHash);
            redirectAttributes.addFlashAttribute("dataType", dataType);

            return "redirect:/lab5/dsa/sign";

        } catch (Exception e) {
            log.error("Error signing data: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Signature failed: " + e.getMessage());
            return "redirect:/lab5/dsa/sign";
        } finally {
            try {
                if (tempPrivKey != null) Files.deleteIfExists(tempPrivKey);
                if (tempInput != null) Files.deleteIfExists(tempInput);
            } catch (IOException cleanupException) {
                log.warn("Failed to delete temporary files: {}", cleanupException.getMessage());
            }
        }
    }

    // --- 3. Перевірка (Verify) ---

    @GetMapping("/verify")
    public String verifyPage(Model model) {
        return VERIFY_VIEW;
    }

    @PostMapping("/verify-signature")
    public String verifySignature(
            @RequestParam("file") MultipartFile fileToVerify,
            @RequestParam("publicKeyFile") MultipartFile publicKeyFile,
            @RequestParam(value = "signatureHex", required = false) String signatureHexInput,
            @RequestParam(value = "signatureFile", required = false) MultipartFile signatureFile,
            RedirectAttributes redirectAttributes) {

        if (fileToVerify.isEmpty() || publicKeyFile.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Please provide the file, the Public Key, and the signature source.");
            return "redirect:/lab5/dsa/verify";
        }

        String signatureHex = null;
        Path tempPubKey = null;
        Path tempFile = null;

        try {
            // --- 1. Визначення джерела підпису ---
            if (signatureFile != null && !signatureFile.isEmpty()) {
                // Читаємо підпис із завантаженого файлу (.sig)
                signatureHex = new String(signatureFile.getBytes()).trim();
                log.info("Signature source: File ({} bytes)", signatureFile.getSize());
            } else if (signatureHexInput != null && !signatureHexInput.trim().isEmpty()) {
                // Використовуємо підпис із текстового поля
                signatureHex = signatureHexInput.trim();
                log.info("Signature source: Hex Input ({} chars)", signatureHex.length());
            } else {
                redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Signature source is required (Hex String or .sig File).");
                return "redirect:/lab5/dsa/verify";
            }

            // Фінальна перевірка підпису
            if (signatureHex.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Signature content cannot be empty after processing.");
                return "redirect:/lab5/dsa/verify";
            }

            // --- 2. Виконання перевірки ---

            // Збереження Public Key у тимчасовий файл
            tempPubKey = Files.createTempFile("dsa_pub_", ".pem");
            publicKeyFile.transferTo(tempPubKey.toFile());
            PublicKey publicKey = dsaService.getPublicKeyFromFile(tempPubKey);

            // Збереження файлу для перевірки у тимчасовий файл
            tempFile = Files.createTempFile("dsa_verify_", fileToVerify.getOriginalFilename());
            fileToVerify.transferTo(tempFile.toFile());

            String dataHash = dsaService.getFileHash(tempFile);

            // Виклик сервісу перевірки
            boolean isValid = dsaService.verifyFileSignature(tempFile, signatureHex, publicKey);

            // --- 3. Передача результатів ---

            redirectAttributes.addFlashAttribute("isValid", isValid);
            redirectAttributes.addFlashAttribute("fileName", fileToVerify.getOriginalFilename());
            redirectAttributes.addFlashAttribute("dataHash", dataHash);
            redirectAttributes.addFlashAttribute("signatureToVerify", signatureHex);

            if (isValid) {
                redirectAttributes.addFlashAttribute(SUCCESS_ATTRIBUTE, "Signature is VALID. The file integrity is confirmed.");
            } else {
                redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Signature is INVALID. The file has been altered or the key is wrong.");
            }

            return "redirect:/lab5/dsa/verify";

        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Verification failed: " + e.getMessage());
            return "redirect:/lab5/dsa/verify";
        } finally {
            try {
                if (tempPubKey != null) Files.deleteIfExists(tempPubKey);
                if (tempFile != null) Files.deleteIfExists(tempFile);
            } catch (IOException cleanupException) {
                log.warn("Failed to delete temporary files: {}", cleanupException.getMessage());
            }
        }
    }
}