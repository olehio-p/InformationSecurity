package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.MD5FileHashResponse;
import com.example.informationsecurity.dto.MD5HashRequest;
import com.example.informationsecurity.dto.MD5HashResponse;
import com.example.informationsecurity.dto.MD5VerificationResponse;
import com.example.informationsecurity.dto.TestResult;
import com.example.informationsecurity.services.FileStorageService;
import com.example.informationsecurity.services.MD5Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class MD5ControllerTest {

    private static final String MAIN_PAGE_VIEW = "lab2";
    private static final String FILE_HASH_VIEW = "md5-file";
    private static final String VERIFY_VIEW = "md5-verify";
    private static final String TEST_PAGE_VIEW = "md5-test";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";
    private static final String RESPONSE_ATTRIBUTE = "response";
    private static final String MOCK_HASH = "d3b07384d113edec49eaa6238ad5ee00";
    private static final byte[] MOCK_FILE_CONTENT = "file content".getBytes();

    @Mock
    private MD5Service md5Service;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private Model model;

    @Spy
    @InjectMocks
    private MD5Controller md5Controller;

    private MockedStatic<Files> mockedFiles;

    @BeforeEach
    void setUp() {
        mockedFiles = Mockito.mockStatic(Files.class);
        Path path = Path.of("/tmp/mock_temp_file");
        mockedFiles.when(() -> Files.createTempFile(anyString(), anyString(), any())).thenReturn(path);
        mockedFiles.when(() -> Files.createTempFile(anyString(), anyString())).thenReturn(path);
        mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        mockedFiles.close();
    }

    @Test
    void index_shouldReturnMainViewAndAddRequestModel() {
        String viewName = md5Controller.index(model);

        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(model).addAttribute(eq("hashRequest"), any(MD5HashRequest.class));
    }

    @Test
    void testPage_shouldReturnTestView() {
        String viewName = md5Controller.testPage(model);

        assertEquals(TEST_PAGE_VIEW, viewName);
    }

    @Test
    void fileHashPage_shouldReturnFileHashView() {
        String viewName = md5Controller.fileHashPage(model);

        assertEquals(FILE_HASH_VIEW, viewName);
    }

    @Test
    void verifyPage_shouldReturnVerifyView() {
        String viewName = md5Controller.verifyPage(model);

        assertEquals(VERIFY_VIEW, viewName);
    }

    @Test
    void hashString_success_noSave() throws Exception {
        MD5HashRequest request = new MD5HashRequest();
        request.setInputText("test");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(md5Service.hashString(request.getInputText())).thenReturn(MOCK_HASH);

        String viewName = md5Controller.hashString(request, bindingResult, false, null, null, model);

        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(md5Service, times(1)).hashString("test");
        verify(fileStorageService, never()).saveMD5Hash(anyString(), anyString(), anyString());
        verify(model).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("Hash computed successfully"));
        verify(model).addAttribute(eq(RESPONSE_ATTRIBUTE), any(MD5HashResponse.class));
    }

    @Test
    void hashString_success_withSave_customParams() throws Exception {
        MD5HashRequest request = new MD5HashRequest();
        request.setInputText("test");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(md5Service.hashString(request.getInputText())).thenReturn(MOCK_HASH);
        when(fileStorageService.saveMD5Hash(eq(MOCK_HASH), eq("custom_hash.md5"), eq("my/dir"))).thenReturn("/path/to/save/custom_hash.md5");

        String viewName = md5Controller.hashString(request, bindingResult, true, "custom_hash", "my/dir", model);

        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(fileStorageService, times(1)).saveMD5Hash(eq(MOCK_HASH), eq("custom_hash.md5"), eq("my/dir"));
        verify(model).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("Hash computed and saved to /path/to/save/custom_hash.md5"));
    }

    @Test
    void hashString_bindingErrors_returnsMainView() {
        MD5HashRequest request = new MD5HashRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = md5Controller.hashString(request, bindingResult, false, null, null, model);

        assertEquals(MAIN_PAGE_VIEW, viewName);
        verifyNoInteractions(md5Service, fileStorageService);
    }

    @Test
    void hashString_serviceException_returnsError() throws Exception {
        MD5HashRequest request = new MD5HashRequest();
        request.setInputText("test");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("MD5 failed")).when(md5Service).hashString(anyString());

        String viewName = md5Controller.hashString(request, bindingResult, false, null, null, model);

        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Error computing or saving hash: MD5 failed"));
    }

    @Test
    void runTests_allPassed_success() {
        Map<String, TestResult> passingResults = new HashMap<>();
        passingResults.put("T1", new TestResult("", "", "", true));
        passingResults.put("T2", new TestResult("", "", "", true));

        doReturn(passingResults).when(md5Controller).runRFC1321Tests();

        String viewName = md5Controller.runTests(model);

        assertEquals(TEST_PAGE_VIEW, viewName);
        verify(model).addAttribute(eq("testResults"), eq(passingResults));
        verify(model).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("All RFC 1321 tests passed!"));
    }

    @Test
    void runTests_someFailed_success() {
        Map<String, TestResult> mixedResults = new HashMap<>();
        mixedResults.put("T1", new TestResult("", "", "", true));
        mixedResults.put("T2", new TestResult("", "", "", false));

        doReturn(mixedResults).when(md5Controller).runRFC1321Tests();

        String viewName = md5Controller.runTests(model);

        assertEquals(TEST_PAGE_VIEW, viewName);
        verify(model).addAttribute(eq("testResults"), eq(mixedResults));
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Some tests failed. See details below."));
    }

    @Test
    void runTests_exception_returnsError() {
        doThrow(new RuntimeException("Test setup failed")).when(md5Controller).runRFC1321Tests();

        String viewName = md5Controller.runTests(model);

        assertEquals(TEST_PAGE_VIEW, viewName);
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Error running tests: Test setup failed"));
    }

    @Test
    void hashFile_success_noSave() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getSize()).thenReturn((long) MOCK_FILE_CONTENT.length);

        doNothing().when(file).transferTo(any(File.class));
        when(md5Service.hashFile(any(Path.class))).thenReturn(MOCK_HASH);

        String viewName = md5Controller.hashFile(file, false, null, null, model);

        assertEquals(FILE_HASH_VIEW, viewName);
        verify(file, times(1)).transferTo(any(File.class));
        verify(md5Service, times(1)).hashFile(any(Path.class));
        verify(model).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("File hash computed successfully"));
        verify(model).addAttribute(eq(RESPONSE_ATTRIBUTE), any(MD5FileHashResponse.class));
        mockedFiles.verify(() -> Files.deleteIfExists(any(Path.class)), times(1));
    }

    @Test
    void hashFile_emptyFile_returnsError() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        String viewName = md5Controller.hashFile(file, false, null, null, model);

        assertEquals(FILE_HASH_VIEW, viewName);
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Please select a file to hash"));
        verifyNoInteractions(md5Service);
    }

    @Test
    void hashFile_ioException_returnsError() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        doThrow(new IOException("Disk full")).when(file).transferTo(any(File.class));

        String viewName = md5Controller.hashFile(file, false, null, null, model);

        assertEquals(FILE_HASH_VIEW, viewName);
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Error processing or saving file: Disk full"));
        verifyNoInteractions(md5Service);
    }

    @Test
    void hashFile_success_withSave_customName() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("original.pdf");
        when(file.getSize()).thenReturn(100L);
        doNothing().when(file).transferTo(any(File.class));

        when(md5Service.hashFile(any(Path.class))).thenReturn(MOCK_HASH);
        when(fileStorageService.saveMD5Hash(eq(MOCK_HASH), eq("custom_name.md5"), eq(null))).thenReturn("/save/path/custom_name.md5");

        String viewName = md5Controller.hashFile(file, true, "custom_name", null, model);

        assertEquals(FILE_HASH_VIEW, viewName);
        verify(fileStorageService, times(1)).saveMD5Hash(eq(MOCK_HASH), eq("custom_name.md5"), eq(null));
        verify(model).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("File hash computed and saved to /save/path/custom_name.md5"));
    }

    @Test
    void verifyFileIntegrity_match_success() throws Exception {
        String expectedHash = MOCK_HASH;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("doc.txt");
        doNothing().when(file).transferTo(any(File.class));

        MultipartFile hashFile = mock(MultipartFile.class);
        when(hashFile.isEmpty()).thenReturn(false);
        when(hashFile.getBytes()).thenReturn(expectedHash.getBytes());

        when(md5Service.hashFile(any(Path.class))).thenReturn(expectedHash);

        String viewName = md5Controller.verifyFileIntegrity(file, hashFile, model);

        assertEquals(VERIFY_VIEW, viewName);
        verify(md5Service, times(1)).hashFile(any(Path.class));
        verify(model).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("File integrity verified successfully!"));
        verify(model).addAttribute(eq(RESPONSE_ATTRIBUTE), any(MD5VerificationResponse.class));
    }

    @Test
    void verifyFileIntegrity_mismatch_failure() throws Exception {

        String expectedHash = "A0B0C0D0E0F0A0B0C0D0E0F0A0B0C0D0";

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("doc.txt");
        doNothing().when(file).transferTo(any(File.class));

        MultipartFile hashFile = mock(MultipartFile.class);
        when(hashFile.isEmpty()).thenReturn(false);
        when(hashFile.getBytes()).thenReturn(expectedHash.getBytes());

        when(md5Service.hashFile(any(Path.class))).thenReturn(MOCK_HASH);

        String viewName = md5Controller.verifyFileIntegrity(file, hashFile, model);

        assertEquals(VERIFY_VIEW, viewName);
        verify(md5Service, times(1)).hashFile(any(Path.class));
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("File integrity check failed! Hash mismatch."));
        verify(model).addAttribute(eq(RESPONSE_ATTRIBUTE), any(MD5VerificationResponse.class));
    }

    @Test
    void verifyFileIntegrity_emptyFiles_returnsError() {
        MultipartFile emptyFile = mock(MultipartFile.class);
        MultipartFile hashFile = mock(MultipartFile.class);

        when(emptyFile.isEmpty()).thenReturn(true);

        String viewName = md5Controller.verifyFileIntegrity(emptyFile, hashFile, model);

        assertEquals(VERIFY_VIEW, viewName);
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Please select both file and hash file"));
        verifyNoInteractions(md5Service);
    }

    @Test
    void verifyFileIntegrity_ioException_returnsError() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("doc.txt");
        doNothing().when(file).transferTo(any(File.class));

        MultipartFile hashFile = mock(MultipartFile.class);
        when(hashFile.isEmpty()).thenReturn(false);
        when(hashFile.getBytes()).thenReturn(MOCK_HASH.getBytes());

        when(md5Service.hashFile(any(Path.class))).thenThrow(new IOException("Read error"));

        String viewName = md5Controller.verifyFileIntegrity(file, hashFile, model);

        assertEquals(VERIFY_VIEW, viewName);
        verify(model).addAttribute(eq(ERROR_ATTRIBUTE), eq("Error verifying file: Read error"));
    }
}