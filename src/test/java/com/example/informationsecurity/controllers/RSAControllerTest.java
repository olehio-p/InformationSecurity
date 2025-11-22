package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.RSAKeyGenRequest;
import com.example.informationsecurity.services.CryptoComparisonService;
import com.example.informationsecurity.services.RSAService;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RSAController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RSAControllerTest {

    @Autowired
    MockMvc mvc;

    RSAService rsaService;
    CryptoComparisonService comparisonService;

    private KeyPair sampleKeyPair;
    private PublicKey mockPublicKey;
    private PrivateKey mockPrivateKey;

    private String originalUserHome;
    private final String MOCK_USER_HOME = "C:\\Users\\mock";

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RSAService rsaService() {
            return Mockito.mock(RSAService.class);
        }

        @Bean
        public CryptoComparisonService cryptoComparisonService() {
            return Mockito.mock(CryptoComparisonService.class);
        }
    }

    @BeforeEach
    void setup() {
        Assertions.assertNotNull(mvc.getDispatcherServlet().getWebApplicationContext());
        rsaService = mvc.getDispatcherServlet().getWebApplicationContext().getBean(RSAService.class);
        comparisonService = mvc.getDispatcherServlet().getWebApplicationContext().getBean(CryptoComparisonService.class);

        mockPublicKey = Mockito.mock(PublicKey.class);
        mockPrivateKey = Mockito.mock(PrivateKey.class);

        Mockito.when(mockPublicKey.getEncoded()).thenReturn("MOCK_PUBLIC_KEY_BYTES".getBytes());
        Mockito.when(mockPrivateKey.getEncoded()).thenReturn("MOCK_PRIVATE_KEY_BYTES".getBytes());

        sampleKeyPair = new KeyPair(mockPublicKey, mockPrivateKey);
        originalUserHome = System.getProperty("user.home");
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
        Mockito.reset(rsaService, comparisonService, mockPublicKey, mockPrivateKey);
    }

    @Test
    void testKeygenPageLoads() throws Exception {
        mvc.perform(get("/lab4/rsa/keygen"))
                .andExpect(status().isOk())
                .andExpect(view().name("rsa-keygen"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    void testKeygenPageLoads_WithExistingRequest() throws Exception {
        
        RSAKeyGenRequest existingRequest = new RSAKeyGenRequest(4096, "existing", "Custom/Path");

        mvc.perform(get("/lab4/rsa/keygen")
                        .flashAttr("request", existingRequest))
                .andExpect(status().isOk())
                .andExpect(view().name("rsa-keygen"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    void testGenerateKeys_success() throws Exception {
        System.setProperty("user.home", MOCK_USER_HOME);

        Path mockHome = Paths.get(MOCK_USER_HOME);
        Path mockOutputDir = mockHome.resolve("Downloads/RSA_Keys");

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            mockedPaths.when(() -> Paths.get(eq(MOCK_USER_HOME), anyString())).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.writeString(any(Path.class), anyString())).thenReturn(mockOutputDir.resolve("key.pem"));

            Mockito.when(rsaService.generateKeyPair(anyInt())).thenReturn(sampleKeyPair);

            mvc.perform(post("/lab4/rsa/generate-keys")
                            .flashAttr("request", new RSAKeyGenRequest(2048, "test_key", "Downloads/RSA_Keys")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/lab4/rsa/keygen"))
                    .andExpect(flash().attributeExists("success"));

            mockedFiles.verify(() -> Files.writeString(any(Path.class), anyString()), Mockito.times(2));
            Mockito.verify(rsaService).generateKeyPair(2048);
        }
    }

    @Test
    void testGenerateKeys_withSpecialCharactersInPrefix() throws Exception {
        System.setProperty("user.home", MOCK_USER_HOME);

        Path mockHome = Paths.get(MOCK_USER_HOME);
        Path mockOutputDir = mockHome.resolve("Downloads/RSA_Keys");

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            mockedPaths.when(() -> Paths.get(eq(MOCK_USER_HOME), anyString())).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.writeString(any(Path.class), anyString())).thenReturn(mockOutputDir.resolve("key.pem"));

            Mockito.when(rsaService.generateKeyPair(anyInt())).thenReturn(sampleKeyPair);

            
            mvc.perform(post("/lab4/rsa/generate-keys")
                            .flashAttr("request", new RSAKeyGenRequest(2048, "test@key#$%", "Downloads/RSA_Keys")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/lab4/rsa/keygen"))
                    .andExpect(flash().attributeExists("success"));

            Mockito.verify(rsaService).generateKeyPair(2048);
        }
    }

    @Test
    void testGenerateKeys_withEmptySaveDirectory() throws Exception {
        System.setProperty("user.home", MOCK_USER_HOME);

        Path mockHome = Paths.get(MOCK_USER_HOME);
        Path mockOutputDir = mockHome.resolve("Downloads/RSA_Keys");

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            mockedPaths.when(() -> Paths.get(eq(MOCK_USER_HOME), anyString())).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.writeString(any(Path.class), anyString())).thenReturn(mockOutputDir.resolve("key.pem"));

            Mockito.when(rsaService.generateKeyPair(anyInt())).thenReturn(sampleKeyPair);

            
            mvc.perform(post("/lab4/rsa/generate-keys")
                            .flashAttr("request", new RSAKeyGenRequest(2048, "test_key", "")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/lab4/rsa/keygen"))
                    .andExpect(flash().attributeExists("success"));

            Mockito.verify(rsaService).generateKeyPair(2048);
        }
    }

    @Test
    void testGenerateKeys_withNullSaveDirectory() throws Exception {
        System.setProperty("user.home", MOCK_USER_HOME);

        Path mockHome = Paths.get(MOCK_USER_HOME);
        Path mockOutputDir = mockHome.resolve("Downloads/RSA_Keys");

        try (MockedStatic<Paths> mockedPaths = Mockito.mockStatic(Paths.class);
             MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {

            mockedPaths.when(() -> Paths.get(eq(MOCK_USER_HOME), anyString())).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockOutputDir);
            mockedFiles.when(() -> Files.writeString(any(Path.class), anyString())).thenReturn(mockOutputDir.resolve("key.pem"));

            Mockito.when(rsaService.generateKeyPair(anyInt())).thenReturn(sampleKeyPair);

            
            mvc.perform(post("/lab4/rsa/generate-keys")
                            .flashAttr("request", new RSAKeyGenRequest(2048, "test_key", null)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/lab4/rsa/keygen"))
                    .andExpect(flash().attributeExists("success"));

            Mockito.verify(rsaService).generateKeyPair(2048);
        }
    }

    @Test
    void testGenerateKeys_failure() throws Exception {
        Mockito.when(rsaService.generateKeyPair(anyInt())).thenThrow(new RuntimeException("GEN_ERROR"));

        mvc.perform(post("/lab4/rsa/generate-keys")
                        .flashAttr("request", new RSAKeyGenRequest(2048, "bad", "Downloads/RSA_Keys")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab4/rsa/keygen"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attributeExists("request"));
    }

    @Test
    void testCryptoPageLoads() throws Exception {
        mvc.perform(get("/lab4/rsa/crypto"))
                .andExpect(status().isOk())
                .andExpect(view().name("rsa-crypto"));
    }

    @Test
    void testEncrypt_missingFiles() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        MockMultipartFile emptyKey = new MockMultipartFile("publicKeyFile", "empty.pem", "application/x-pem-file", new byte[0]);

        mvc.perform(multipart("/lab4/rsa/encrypt")
                        .file(emptyFile)
                        .file(emptyKey)
                        .param("outputFileName", "")
                        .param("saveDirectory", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab4/rsa/crypto"))
                .andExpect(flash().attribute("error", "Please select both data file and public key file."));

        Mockito.verifyNoInteractions(rsaService);
    }

    @Test
    void testDecrypt_missingFiles() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        MockMultipartFile emptyKey = new MockMultipartFile("privateKeyFile", "empty.pem", "application/x-pem-file", new byte[0]);

        mvc.perform(multipart("/lab4/rsa/decrypt")
                        .file(empty)
                        .file(emptyKey))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab4/rsa/crypto"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testComparePageLoads() throws Exception {
        mvc.perform(get("/lab4/rsa/compare"))
                .andExpect(status().isOk())
                .andExpect(view().name("rsa-compare"))
                .andExpect(model().attributeExists("keySize"));
    }

    @Test
    void testComparePageLoads_WithExistingKeySize() throws Exception {
        mvc.perform(get("/lab4/rsa/compare")
                        .flashAttr("keySize", 4096))
                .andExpect(status().isOk())
                .andExpect(view().name("rsa-compare"))
                .andExpect(model().attributeExists("keySize"));
    }
}