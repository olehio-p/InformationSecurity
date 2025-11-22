package com.example.informationsecurity.services;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    private Path tempUserHome;
    private Path originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        fileStorageService = new FileStorageService();

        
        originalUserHome = Paths.get(System.getProperty("user.home"));
        tempUserHome = Files.createTempDirectory("fakeUserHome");
        System.setProperty("user.home", tempUserHome.toString());

        
        ReflectionTestUtils.setField(fileStorageService, "baseStorageDirectory", "test_md5/");
    }

    @AfterEach
    void cleanUp() throws IOException {
        System.setProperty("user.home", originalUserHome.toString());
        if (tempUserHome != null && Files.exists(tempUserHome)) {
            Files.walk(tempUserHome)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                    });
        }
    }

    
    
    
    @Test
    void testSaveNumbersToFile_defaultDirectory() throws IOException {
        List<Long> numbers = List.of(1L, 2L, 3L);

        String pathStr = fileStorageService.saveNumbersToFile(numbers);
        Path path = Paths.get(pathStr);

        assertTrue(Files.exists(path), "File must exist after saving numbers");

        List<String> lines = Files.readAllLines(path);
        assertEquals(List.of("1", "2", "3"), lines);
    }

    
    
    
    @Test
    void testSaveNumbersToFile_CustomDirectory() throws IOException {
        List<Long> numbers = List.of(10L, 20L, 30L);

        String pathStr = fileStorageService.saveNumbersToFile(
                numbers,
                "custom_numbers",
                "numbers.txt"
        );

        Path path = Paths.get(pathStr);

        assertTrue(Files.exists(path));
        assertEquals("numbers.txt", path.getFileName().toString());
        assertEquals("custom_numbers",
                path.getParent().getFileName().toString()
        );

        assertEquals(List.of("10", "20", "30"), Files.readAllLines(path));
    }

    @Test
    void testSaveNumbersToFile_CreatesDirectory() throws IOException {
        Path expectedDir = tempUserHome.resolve("Downloads").resolve("auto_create_dir");
        assertFalse(Files.exists(expectedDir));

        fileStorageService.saveNumbersToFile(List.of(7L), "auto_create_dir", "x.txt");

        assertTrue(Files.exists(expectedDir), "Directory must be created automatically");
    }

    
    
    
    @Test
    void testSaveMD5Hash_UsesDefaults() throws IOException {
        String result = fileStorageService.saveMD5Hash("abcd1234", "", "");
        Path path = Paths.get(result);

        assertTrue(Files.exists(path));
        assertEquals("hash_output.md5", path.getFileName().toString());
        assertEquals("test_md5", path.getParent().getFileName().toString());

        assertEquals("abcd1234", Files.readString(path));
    }

    @Test
    void testSaveMD5Hash_CustomNameAndDirectory() throws IOException {
        String result = fileStorageService.saveMD5Hash(
                "XYZ",
                "customName",
                "md5_test_dir"
        );

        Path path = Paths.get(result);

        assertTrue(Files.exists(path));
        assertEquals("customName.md5", path.getFileName().toString());
        assertEquals("md5_test_dir", path.getParent().getFileName().toString());
    }

    @Test
    void testSaveMD5Hash_SanitizesFileNameAndAddsExtension() throws IOException {
        String result = fileStorageService.saveMD5Hash(
                "HASH",
                "!!bad??name!!",
                "clean_dir"
        );

        Path path = Paths.get(result);

        assertTrue(Files.exists(path));
        assertEquals("badname.md5", path.getFileName().toString());
    }


    @Test
    void testSaveMD5Hash_CreatesDirectory() throws IOException {
        Path dir = tempUserHome.resolve("Downloads").resolve("zzz_new_md5");
        assertFalse(Files.exists(dir));

        fileStorageService.saveMD5Hash("HHH", "abc", "zzz_new_md5");

        assertTrue(Files.exists(dir));
    }
}
