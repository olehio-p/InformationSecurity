package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.DSAKeyGenRequest;
import com.example.informationsecurity.services.DSAService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DSAController.class)
class DSAControllerTest {

    @TempDir
    Path tempDirForKeys;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSAService dsaService;

    private KeyPair mockKeyPair;
    private PrivateKey mockPrivateKey;
    private PublicKey mockPublicKey;
    private final String mockSignatureHex = "a1b2c3d4e5f6";
    private final String mockFileHash = "hash12345";
    private String originalUserHome;

    private final MockMultipartFile emptyFileMock = new MockMultipartFile("file", "", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

    @TestConfiguration
    static class TestConfig {
        @Bean
        public DSAService dsaService() {
            return mock(DSAService.class);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        mockPrivateKey = mock(PrivateKey.class);
        mockPublicKey = mock(PublicKey.class);
        when(mockPrivateKey.getEncoded()).thenReturn("MockPrivateData".getBytes());
        when(mockPublicKey.getEncoded()).thenReturn("MockPublicData".getBytes());

        mockKeyPair = mock(KeyPair.class);
        when(mockKeyPair.getPrivate()).thenReturn(mockPrivateKey);
        when(mockKeyPair.getPublic()).thenReturn(mockPublicKey);

        tempDirForKeys = Files.createTempDirectory("dsa_test_keys");
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDirForKeys.toAbsolutePath().toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // restore
        System.setProperty("user.home", originalUserHome);
        if (tempDirForKeys != null && Files.exists(tempDirForKeys)) {
            Files.walk(tempDirForKeys)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
        Mockito.reset(dsaService);
    }

    // =========================================================================
    // 1. Key Generation Tests
    // =========================================================================

    @Test
    void keygenPage_shouldReturnKeygenView_withDefaultRequest() throws Exception {
        mockMvc.perform(get("/lab5/dsa/keygen"))
                .andExpect(status().isOk())
                .andExpect(view().name("dsa-keygen"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", new DSAKeyGenRequest(2048, "dsa_key", "Downloads/DSA_Keys")));
    }

    @Test
    void keygenPage_shouldPreserveExistingRequest_inModel() throws Exception {
        DSAKeyGenRequest existing = new DSAKeyGenRequest(4096, "existing", "SomeDir");
        mockMvc.perform(get("/lab5/dsa/keygen").flashAttr("request", existing))
                .andExpect(status().isOk())
                .andExpect(view().name("dsa-keygen"))
                .andExpect(model().attribute("request", existing));
    }

    @Test
    void generateKeys_success_shouldGenerateKeysAndRedirect() throws Exception {
        int keySize = 1024;
        String prefix = "test_key";
        String saveDir = "TestKeys";

        when(dsaService.generateKeyPair(keySize)).thenReturn(mockKeyPair);

        MvcResult result = mockMvc.perform(post("/lab5/dsa/generate-keys")
                        .param("keySize", String.valueOf(keySize))
                        .param("keyPrefix", prefix)
                        .param("saveDirectory", saveDir)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/keygen"))
                .andExpect(flash().attributeExists("success"))
                .andReturn();

        // Extract real directory from flash message
        String flashMsg = (String) result.getFlashMap().get("success");

        // Extract path after "saved to: "
        String realPathString = flashMsg.substring(flashMsg.indexOf("saved to: ") + 10).trim();

        Path realPath = Paths.get(realPathString);

        // ACTUAL ASSERTIONS
        assertTrue(Files.exists(realPath));
        assertTrue(Files.exists(realPath.resolve("test_key_1024_public.pem")));
        assertTrue(Files.exists(realPath.resolve("test_key_1024_private.pem")));

        verify(dsaService).generateKeyPair(keySize);
    }



    @Test
    void generateKeys_success_withDefaultDirectoryAndSanitizedPrefix() throws Exception {
        int keySize = 512;
        String prefix = "bad!prefix#_with_dots.ok";
        String sanitizedPrefix = "badprefix_with_dots.ok";

        // Mock the returned KeyPair so controller proceeds normally
        when(dsaService.generateKeyPair(keySize)).thenReturn(mockKeyPair);

        Path expectedDirPath = Paths.get(tempDirForKeys.toString(), "Downloads/DSA_Keys");
        Files.createDirectories(expectedDirPath);

        Path expectedPublicKeyPath =
                expectedDirPath.resolve(String.format("%s_%d_public.pem", sanitizedPrefix, keySize));
        Files.writeString(expectedPublicKeyPath, "dummy public key");

        mockMvc.perform(post("/lab5/dsa/generate-keys")
                        .param("keySize", String.valueOf(keySize))
                        .param("keyPrefix", prefix)
                        .param("saveDirectory", "")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/keygen"))
                .andExpect(flash().attributeExists("success"));

        assertTrue(Files.exists(expectedDirPath));
        assertTrue(Files.exists(expectedPublicKeyPath));
    }


    @Test
    void generateKeys_failure_shouldRedirectWithErrorAndFlashRequest() throws Exception {
        DSAKeyGenRequest request = new DSAKeyGenRequest(2048, "test", "tmp");
        String errorMessage = "Mock Key Gen Error";
        when(dsaService.generateKeyPair(anyInt())).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(post("/lab5/dsa/generate-keys")
                        .param("keySize", String.valueOf(request.getKeySize()))
                        .param("keyPrefix", request.getKeyPrefix())
                        .param("saveDirectory", request.getSaveDirectory())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/keygen"))
                .andExpect(flash().attribute("error", containsString(errorMessage)))
                .andExpect(flash().attribute("request", request));
    }

    @Test
    void generateKeys_prefixAllSpecialChars_shouldStillCreateFiles_withLeadingUnderscore() throws Exception {

        int keySize = 256;
        String prefix = "!@#$%^";  // becomes empty after sanitization → "_"
        String saveDir = "SomeDir";

        when(dsaService.generateKeyPair(keySize)).thenReturn(mockKeyPair);

        mockMvc.perform(post("/lab5/dsa/generate-keys")
                        .param("keySize", String.valueOf(keySize))
                        .param("keyPrefix", prefix)
                        .param("saveDirectory", saveDir)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/keygen"))
                .andExpect(flash().attributeExists("success"));

        // Verify key generation was invoked
        verify(dsaService, times(1)).generateKeyPair(keySize);

        // Expected directory
        Path outputDir = tempDirForKeys.resolve(saveDir);

        assertTrue(Files.exists(outputDir), "Expected directory does not exist");

        // Because prefix sanitizes to "" → final prefix becomes "_"
        Path expectedPub = outputDir.resolve("_256_public.pem");
        Path expectedPriv = outputDir.resolve("_256_private.pem");

        assertTrue(Files.exists(expectedPub), "Sanitized public key file not created");
        assertTrue(Files.exists(expectedPriv), "Sanitized private key file not created");
    }

    @Test
    void signPage_shouldReturnSignView() throws Exception {
        mockMvc.perform(get("/lab5/dsa/sign"))
                .andExpect(status().isOk())
                .andExpect(view().name("dsa-sign"));
    }

    @Test
    void signData_validationFailure_missingPrivateKey() throws Exception {
        MockMultipartFile fileToSign = new MockMultipartFile("file", "data.txt", MediaType.TEXT_PLAIN_VALUE, "data".getBytes());
        MockMultipartFile emptyKey = new MockMultipartFile("privateKeyFile", "", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);

        mockMvc.perform(multipart("/lab5/dsa/sign-data")
                        .file(fileToSign)
                        .file(emptyKey))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/sign"))
                .andExpect(flash().attribute("error", "Please select the Private Key file."));
    }

    @Test
    void signData_validationFailure_missingData() throws Exception {
        MockMultipartFile privateKeyFile = new MockMultipartFile("privateKeyFile", "private.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());

        mockMvc.perform(multipart("/lab5/dsa/sign-data")
                        .file(emptyFileMock)
                        .file(privateKeyFile)
                        .param("inputText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/sign"))
                .andExpect(flash().attribute("error", "Please provide a file or text to sign."));
    }

    @Test
    void signData_success_signingFile_withDefaultSaveName() throws Exception {
        MockMultipartFile privateKeyFile = new MockMultipartFile("privateKeyFile", "private.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());
        MockMultipartFile fileToSign = new MockMultipartFile("file", "document.pdf", MediaType.APPLICATION_PDF_VALUE, "file content".getBytes());
        String expectedSaveDir = "Downloads/DSA_Signatures";

        when(dsaService.getPrivateKeyFromFile(any(Path.class))).thenReturn(mockPrivateKey);
        when(dsaService.getFileHash(any(Path.class))).thenReturn(mockFileHash);
        when(dsaService.signFile(any(Path.class), eq(mockPrivateKey))).thenReturn(mockSignatureHex);

        mockMvc.perform(multipart("/lab5/dsa/sign-data")
                        .file(fileToSign)
                        .file(privateKeyFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/sign"))
                .andExpect(flash().attribute("success", "File successfully signed."))
                .andExpect(flash().attribute("signatureHex", mockSignatureHex))
                .andExpect(flash().attribute("dataHash", mockFileHash))
                .andExpect(flash().attribute("dataType", "File"));

        verify(dsaService).signFile(any(Path.class), eq(mockPrivateKey));

        Path sigPath = Paths.get(tempDirForKeys.toAbsolutePath().toString(), expectedSaveDir, "document.pdf.sig");
        assertTrue(Files.exists(sigPath));
    }

    @Test
    void signData_success_signingText_withCustomSaveNameWithoutExtension() throws Exception {
        MockMultipartFile privateKeyFile = new MockMultipartFile("privateKeyFile", "private.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());
        String inputText = "Hello world!";
        String customSaveName = "my_custom_signature";
        String expectedSaveDir = "MySigs";

        when(dsaService.getPrivateKeyFromFile(any(Path.class))).thenReturn(mockPrivateKey);
        when(dsaService.signString(inputText, mockPrivateKey)).thenReturn(mockSignatureHex);

        mockMvc.perform(multipart("/lab5/dsa/sign-data")
                        .file(emptyFileMock)
                        .file(privateKeyFile)
                        .param("inputText", inputText)
                        .param("saveName", customSaveName)
                        .param("saveDirectory", expectedSaveDir))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/sign"))
                .andExpect(flash().attribute("success", "String successfully signed."))
                .andExpect(flash().attribute("dataType", "String"));

        verify(dsaService).signString(inputText, mockPrivateKey);

        Path sigPath = Paths.get(tempDirForKeys.toAbsolutePath().toString(), expectedSaveDir, customSaveName + ".sig");
        assertTrue(Files.exists(sigPath));
    }

    @Test
    void signData_success_customSaveNameAlreadyHasSigExtension_shouldNotDoubleAppend() throws Exception {
        MockMultipartFile privateKeyFile = new MockMultipartFile("privateKeyFile", "private.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "k".getBytes());
        MockMultipartFile fileToSign = new MockMultipartFile("file", "a.txt", MediaType.TEXT_PLAIN_VALUE, "x".getBytes());
        String saveNameWithSig = "existing.sig";
        String saveDir = "SomeSigDir";

        when(dsaService.getPrivateKeyFromFile(any(Path.class))).thenReturn(mockPrivateKey);
        when(dsaService.getFileHash(any(Path.class))).thenReturn("h");
        when(dsaService.signFile(any(Path.class), eq(mockPrivateKey))).thenReturn(mockSignatureHex);

        mockMvc.perform(multipart("/lab5/dsa/sign-data")
                        .file(fileToSign)
                        .file(privateKeyFile)
                        .param("saveName", saveNameWithSig)
                        .param("saveDirectory", saveDir))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/sign"))
                .andExpect(flash().attribute("success", "File successfully signed."));

        Path sigPath = Paths.get(tempDirForKeys.toAbsolutePath().toString(), saveDir, saveNameWithSig);
        assertTrue(Files.exists(sigPath));
    }

    @Test
    void signData_errorHandling_shouldRedirectWithError() throws Exception {
        MockMultipartFile privateKeyFile = new MockMultipartFile("privateKeyFile", "private.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "k".getBytes());
        String errorMessage = "Signing failed due to crypto error";

        when(dsaService.getPrivateKeyFromFile(any(Path.class))).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(multipart("/lab5/dsa/sign-data")
                        .file(emptyFileMock)
                        .file(privateKeyFile)
                        .param("inputText", "some text"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/sign"))
                .andExpect(flash().attribute("error", containsString(errorMessage)));
    }

    @Test
    void verifyPage_shouldReturnVerifyView() throws Exception {
        mockMvc.perform(get("/lab5/dsa/verify"))
                .andExpect(status().isOk())
                .andExpect(view().name("dsa-verify"));
    }

    @Test
    void verifySignature_validationFailure_missingSignatureSource() throws Exception {
        MockMultipartFile fileToVerify = new MockMultipartFile("file", "data.txt", MediaType.TEXT_PLAIN_VALUE, "data".getBytes());
        MockMultipartFile publicKeyFile = new MockMultipartFile("publicKeyFile", "public.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());
        MockMultipartFile emptySignatureFile = new MockMultipartFile("signatureFile", "", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);

        mockMvc.perform(multipart("/lab5/dsa/verify-signature")
                        .file(fileToVerify)
                        .file(publicKeyFile)
                        .file(emptySignatureFile)
                        .param("signatureHex", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/verify"))
                .andExpect(flash().attribute("error", "Signature source is required (Hex String or .sig File)."));
    }

    @Test
    void verifySignature_success_validSignatureFromHexInput() throws Exception {
        MockMultipartFile fileToVerify = new MockMultipartFile("file", "report.txt", MediaType.TEXT_PLAIN_VALUE, "file content".getBytes());
        MockMultipartFile publicKeyFile = new MockMultipartFile("publicKeyFile", "public.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());

        when(dsaService.getPublicKeyFromFile(any(Path.class))).thenReturn(mockPublicKey);
        when(dsaService.getFileHash(any(Path.class))).thenReturn(mockFileHash);
        when(dsaService.verifyFileSignature(any(Path.class), eq(mockSignatureHex), eq(mockPublicKey))).thenReturn(true);

        mockMvc.perform(multipart("/lab5/dsa/verify-signature")
                        .file(fileToVerify)
                        .file(publicKeyFile)
                        .param("signatureHex", mockSignatureHex))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/verify"))
                .andExpect(flash().attribute("isValid", true))
                .andExpect(flash().attribute("success", containsString("Signature is VALID.")));

        verify(dsaService).verifyFileSignature(any(Path.class), eq(mockSignatureHex), eq(mockPublicKey));
    }

    @Test
    void verifySignature_success_invalidSignatureFromFileSource() throws Exception {
        String signatureContent = "invalid_sig_hex";
        MockMultipartFile fileToVerify = new MockMultipartFile("file", "report.txt", MediaType.TEXT_PLAIN_VALUE, "file content".getBytes());
        MockMultipartFile publicKeyFile = new MockMultipartFile("publicKeyFile", "public.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());
        MockMultipartFile signatureFile = new MockMultipartFile("signatureFile", "sig.sig", MediaType.TEXT_PLAIN_VALUE, signatureContent.getBytes());

        when(dsaService.getPublicKeyFromFile(any(Path.class))).thenReturn(mockPublicKey);
        when(dsaService.getFileHash(any(Path.class))).thenReturn(mockFileHash);
        when(dsaService.verifyFileSignature(any(Path.class), eq(signatureContent), eq(mockPublicKey))).thenReturn(false);

        mockMvc.perform(multipart("/lab5/dsa/verify-signature")
                        .file(fileToVerify)
                        .file(publicKeyFile)
                        .file(signatureFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/verify"))
                .andExpect(flash().attribute("isValid", false))
                .andExpect(flash().attribute("error", containsString("Signature is INVALID.")));

        verify(dsaService).verifyFileSignature(any(Path.class), eq(signatureContent), eq(mockPublicKey));
    }

    @Test
    void verifySignature_signatureFile_precedence_overHexInput() throws Exception {
        String sigFileContent = "sig_from_file";
        MockMultipartFile fileToVerify = new MockMultipartFile("file", "report.txt", MediaType.TEXT_PLAIN_VALUE, "c".getBytes());
        MockMultipartFile publicKeyFile = new MockMultipartFile("publicKeyFile", "public.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "k".getBytes());
        MockMultipartFile signatureFile = new MockMultipartFile("signatureFile", "s.sig", MediaType.TEXT_PLAIN_VALUE, sigFileContent.getBytes());
        String hexParam = "hex_that_should_be_ignored";

        when(dsaService.getPublicKeyFromFile(any(Path.class))).thenReturn(mockPublicKey);
        when(dsaService.getFileHash(any(Path.class))).thenReturn(mockFileHash);
        when(dsaService.verifyFileSignature(any(Path.class), eq(sigFileContent), eq(mockPublicKey))).thenReturn(true);

        mockMvc.perform(multipart("/lab5/dsa/verify-signature")
                        .file(fileToVerify)
                        .file(publicKeyFile)
                        .file(signatureFile)
                        .param("signatureHex", hexParam))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/verify"))
                .andExpect(flash().attribute("isValid", true))
                .andExpect(flash().attribute("success", containsString("Signature is VALID.")));

        verify(dsaService).verifyFileSignature(any(Path.class), eq(sigFileContent), eq(mockPublicKey));
    }

    @Test
    void verifySignature_errorHandling_shouldRedirectWithError() throws Exception {
        MockMultipartFile fileToVerify = new MockMultipartFile("file", "report.txt", MediaType.TEXT_PLAIN_VALUE, "file content".getBytes());
        MockMultipartFile publicKeyFile = new MockMultipartFile("publicKeyFile", "public.pem", MediaType.APPLICATION_OCTET_STREAM_VALUE, "key data".getBytes());
        String errorMessage = "Verification failed due to key parsing error";

        when(dsaService.getPublicKeyFromFile(any(Path.class))).thenThrow(new RuntimeException(errorMessage));

        mockMvc.perform(multipart("/lab5/dsa/verify-signature")
                        .file(fileToVerify)
                        .file(publicKeyFile)
                        .param("signatureHex", mockSignatureHex))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab5/dsa/verify"))
                .andExpect(flash().attribute("error", containsString(errorMessage)));
    }
}
