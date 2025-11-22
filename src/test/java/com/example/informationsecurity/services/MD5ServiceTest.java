package com.example.informationsecurity.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class MD5ServiceTest {

    @InjectMocks
    private MD5Service md5Service;

    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        
        
    }

    private Path createTestFile(String fileName, String content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, content.getBytes());
        return filePath;
    }

    

    @Test
    void testBytesToIntAndIntToBytes_littleEndianConversion() {
        
        

        
        int originalInt = 0x12345678;

        byte[] bytes = new byte[4];

        
        ReflectionTestUtils.invokeMethod(md5Service, "intToBytes", originalInt, bytes, 0);

        assertEquals(0x78, bytes[0] & 0xFF);
        assertEquals(0x56, bytes[1] & 0xFF);
        assertEquals(0x34, bytes[2] & 0xFF);
        assertEquals(0x12, bytes[3] & 0xFF);

        
        int restoredInt = ReflectionTestUtils.invokeMethod(md5Service, "bytesToInt", bytes, 0);

        assertEquals(originalInt, restoredInt, "Conversion back to int must preserve the value.");
    }

    @Test
    void testBytesToHex() {
        byte[] input = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        String hex = ReflectionTestUtils.invokeMethod(md5Service, "bytesToHex", input);
        assertEquals("DEADBEEF", hex);
    }

    

    @Test
    void testHashString_emptyString() throws IOException {
        String input = "";
        String expectedHash = "D41D8CD98F00B204E9800998ECF8427E"; 

        assertEquals(expectedHash, md5Service.hashString(input));
    }

    @Test
    void testHashString_shortString() throws IOException {
        String input = "abc";
        String expectedHash = "900150983CD24FB0D6963F7D28E17F72"; 

        assertEquals(expectedHash, md5Service.hashString(input));
    }

    @Test
    void testHashString_paddingBoundary_55Bytes() throws IOException {
        
        
        String input = IntStream.range(0, 55).mapToObj(i -> "a").collect(Collectors.joining());
        String expectedHash = "EF1772B6DFF9A122358552954AD0DF65";

        assertEquals(expectedHash, md5Service.hashString(input));
    }

    @Test
    void testHashString_paddingBoundary_56Bytes_RequiresTwoBlocks() throws IOException {
        
        
        
        String input = IntStream.range(0, 56).mapToObj(i -> "a").collect(Collectors.joining());
        String expectedHash = "3B0C8AC703F828B04C6C197006D17218";

        assertEquals(expectedHash, md5Service.hashString(input));
    }

    @Test
    void testHashString_multiBlock_64Bytes() throws IOException {
        
        
        String input = IntStream.range(0, 64).mapToObj(i -> "a").collect(Collectors.joining());
        String expectedHash = "014842D480B571495A4A0363793F7367";

        assertEquals(expectedHash, md5Service.hashString(input));
    }

    @Test
    void testHashString_multiBlock_65Bytes() throws IOException {
        
        String input = IntStream.range(0, 65).mapToObj(i -> "a").collect(Collectors.joining());
        String expectedHash = "C743A45E0D2E6A95CB859ADAE0248435";

        assertEquals(expectedHash, md5Service.hashString(input));
    }


    

    @Test
    void testHashFile_File() throws IOException {
        String content = "Test file content for MD5 hashing.";
        Path filePath = createTestFile("test_file.txt", content);
        String expectedHash = md5Service.hashString(content); 

        
        assertEquals(expectedHash, md5Service.hashFile(filePath.toFile()));
    }

    @Test
    void testHashFile_Path() throws IOException {
        String content = "Another test file content.";
        Path filePath = createTestFile("test_file2.txt", content);
        String expectedHash = md5Service.hashString(content); 

        
        assertEquals(expectedHash, md5Service.hashFile(filePath));
    }

    @Test
    void testHashFile_NonExistentFile() {
        File nonExistent = tempDir.resolve("non_existent.txt").toFile();
        assertThrows(IOException.class, () -> md5Service.hashFile(nonExistent));
    }

    

    @Test
    void testVerifyFileIntegrity_success() throws IOException {
        String content = "Data to verify.";
        Path filePath = createTestFile("verify_ok.txt", content);
        String expectedHash = md5Service.hashString(content);

        assertTrue(md5Service.verifyFileIntegrity(filePath.toFile(), expectedHash), "Verification should succeed with correct hash.");
    }

    @Test
    void testVerifyFileIntegrity_failure() throws IOException {
        String content = "Data to verify.";
        Path filePath = createTestFile("verify_fail.txt", content);
        String incorrectHash = "DEADBEEF000000000000000000000000";

        assertFalse(md5Service.verifyFileIntegrity(filePath.toFile(), incorrectHash), "Verification should fail with incorrect hash.");
    }

    @Test
    void testVerifyFileIntegrity_toleratesWhitespaceInExpectedHash() throws IOException {
        String content = "Tolerate space.";
        Path filePath = createTestFile("verify_trim.txt", content);
        String actualHash = md5Service.hashString(content);

        
        String expectedHashWithSpace = " " + actualHash + "\t";

        assertTrue(md5Service.verifyFileIntegrity(filePath.toFile(), expectedHashWithSpace), "Verification should succeed by trimming the expected hash.");
    }
}