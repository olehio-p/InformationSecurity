package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.RC5OperationRequest;
import com.example.informationsecurity.services.RC5ModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Path;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RC5Controller.class)
@ContextConfiguration(classes = {RC5ControllerTest.TestConfig.class, RC5Controller.class})
class RC5ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RC5ModeService rc5ModeService;

    private RC5OperationRequest request;

    @BeforeEach
    void setUp() {
        request = new RC5OperationRequest(64, 20, 16, "password", "MyFile.rc5", "TestDir");
    }

    @Configuration
    static class TestConfig {
        @Bean
        RC5ModeService rc5ModeService() {
            return Mockito.mock(RC5ModeService.class);
        }
    }

    @Test
    void encryptPage_shouldReturnEncryptViewWithRequest() throws Exception {
        mockMvc.perform(get("/lab3/rc5/encrypt"))
                .andExpect(status().isOk())
                .andExpect(view().name("rc5-encrypt"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    void decryptPage_shouldReturnDecryptViewWithRequest() throws Exception {
        mockMvc.perform(get("/lab3/rc5/decrypt"))
                .andExpect(status().isOk())
                .andExpect(view().name("rc5-decrypt"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    void encrypt_shouldReturnError_whenFileOrPasswordEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        mockMvc.perform(multipart("/lab3/rc5/encrypt")
                        .file(file)
                        .param("password", "")
                        .param("w", "64")
                        .param("r", "20")
                        .param("b", "16")
                        .param("saveDirectory", "TestDir")
                        .param("saveName", "MyFile.rc5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab3/rc5/encrypt"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void encrypt_shouldEncryptFileAndReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plain.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/lab3/rc5/encrypt")
                        .file(file)
                        .param("password", request.getPassword())
                        .param("w", String.valueOf(request.getW()))
                        .param("r", String.valueOf(request.getR()))
                        .param("b", String.valueOf(request.getB()))
                        .param("saveDirectory", request.getSaveDirectory())
                        .param("saveName", request.getSaveName()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab3/rc5/encrypt"))
                .andExpect(flash().attributeExists("success"));

        verify(rc5ModeService).encryptFile(any(Path.class), any(Path.class), any(), any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    void encrypt_shouldHandleServiceExceptionAndReturnError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plain.txt", "text/plain", "Hello".getBytes());
        doThrow(new RuntimeException("RC5 Failed")).when(rc5ModeService)
                .encryptFile(any(Path.class), any(Path.class), any(), any(Integer.class), any(Integer.class), any(Integer.class));

        mockMvc.perform(multipart("/lab3/rc5/encrypt")
                        .file(file)
                        .param("password", request.getPassword())
                        .param("w", String.valueOf(request.getW()))
                        .param("r", String.valueOf(request.getR()))
                        .param("b", String.valueOf(request.getB()))
                        .param("saveDirectory", request.getSaveDirectory())
                        .param("saveName", request.getSaveName()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab3/rc5/encrypt"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void decrypt_shouldReturnError_whenFileOrPasswordEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        mockMvc.perform(multipart("/lab3/rc5/decrypt")
                        .file(file)
                        .param("password", "")
                        .param("w", "64")
                        .param("r", "20")
                        .param("b", "16")
                        .param("saveDirectory", "TestDir")
                        .param("saveName", "MyFile.rc5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab3/rc5/decrypt"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void decrypt_shouldDecryptFileAndReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "encrypted.rc5", "application/octet-stream", "Encrypted".getBytes());

        mockMvc.perform(multipart("/lab3/rc5/decrypt")
                        .file(file)
                        .param("password", request.getPassword())
                        .param("w", String.valueOf(request.getW()))
                        .param("r", String.valueOf(request.getR()))
                        .param("b", String.valueOf(request.getB()))
                        .param("saveDirectory", request.getSaveDirectory())
                        .param("saveName", request.getSaveName()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab3/rc5/decrypt"))
                .andExpect(flash().attributeExists("success"));

        verify(rc5ModeService).decryptFile(any(Path.class), any(Path.class), any(), any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    void decrypt_shouldHandleServiceExceptionAndReturnError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "encrypted.rc5", "application/octet-stream", "Encrypted".getBytes());
        doThrow(new RuntimeException("RC5 Decrypt Failed")).when(rc5ModeService)
                .decryptFile(any(Path.class), any(Path.class), any(), any(Integer.class), any(Integer.class), any(Integer.class));

        mockMvc.perform(multipart("/lab3/rc5/decrypt")
                        .file(file)
                        .param("password", request.getPassword())
                        .param("w", String.valueOf(request.getW()))
                        .param("r", String.valueOf(request.getR()))
                        .param("b", String.valueOf(request.getB()))
                        .param("saveDirectory", request.getSaveDirectory())
                        .param("saveName", request.getSaveName()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lab3/rc5/decrypt"))
                .andExpect(flash().attributeExists("error"));
    }
}
