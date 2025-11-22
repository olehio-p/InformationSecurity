package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.CesaroTestResponse;
import com.example.informationsecurity.dto.ComparisonMetrics;
import com.example.informationsecurity.dto.ComparisonTestRequest;
import com.example.informationsecurity.dto.ComparisonTestResponse;
import com.example.informationsecurity.dto.RandomGeneratorRequest;
import com.example.informationsecurity.dto.RandomGeneratorResponse;
import com.example.informationsecurity.services.FileStorageService;
import com.example.informationsecurity.services.RandomNumberService;
import com.example.informationsecurity.utils.RequestValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomNumberControllerTest {

    private static final String MAIN_PAGE_VIEW = "lab1";
    private static final String COMPARE_PAGE_VIEW = "compare";
    private static final String PARAMETERS_PAGE_VIEW = "parameters";
    private static final String CESARO_TEST_VIEW = "cesaro-test";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";
    private static final String RESPONSE_ATTRIBUTE = "response";

    @Mock
    private RandomNumberService randomNumberService;

    @Mock
    private RequestValidator requestValidator;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private Model model;

    @InjectMocks
    private RandomNumberController controller;

    @Test
    void index_shouldReturnMainViewAndAddRequestModel() {
        String viewName = controller.index(model);
        
        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq("generatorRequest"), any(RandomGeneratorRequest.class));
    }

    @Test
    void cesaroTestPage_shouldReturnCesaroTestViewAndAddRequestModel() {
        String viewName = controller.cesaroTestPage(model);
        
        assertEquals(CESARO_TEST_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq("cesaroRequest"), any(CesaroTestRequest.class));
    }

    @Test
    void comparePage_shouldReturnCompareViewAndAddRequestModel() {
        String viewName = controller.comparePage(model);
        
        assertEquals(COMPARE_PAGE_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq("comparisonRequest"), any(ComparisonTestRequest.class));
    }

    @Test
    void parametersPage_shouldReturnParametersView() {
        String viewName = controller.parametersPage();
        
        assertEquals(PARAMETERS_PAGE_VIEW, viewName);
    }

    @Test
    void generateNumbers_success_withDefaultFileParams() {
        RandomGeneratorRequest request = new RandomGeneratorRequest(1L, 1L, 1L, 100L, 10);
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        RandomGeneratorResponse response = RandomGeneratorResponse.builder()
                .numbers(Arrays.asList(1L, 2L))
                .generatedCount(2)
                .build();
        when(randomNumberService.generateNumbers(request)).thenReturn(response);
        when(fileStorageService.saveNumbersToFile(anyList(), eq("generated"), eq("random_numbers.txt"))).thenReturn("/default/path.txt");

        String viewName = controller.generateNumbers(request, bindingResult, null, null, model);
        
        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(requestValidator, times(1)).validateRandomGeneratorRequest(request);
        verify(randomNumberService, times(1)).generateNumbers(request);
        verify(fileStorageService, times(1)).saveNumbersToFile(anyList(), eq("generated"), eq("random_numbers.txt"));
        verify(model, times(1)).addAttribute(eq(RESPONSE_ATTRIBUTE), eq(response));
        verify(model, times(1)).addAttribute(eq(SUCCESS_ATTRIBUTE), contains("Successfully generated 2 numbers. File saved to: /default/path.txt"));
    }

    @Test
    void generateNumbers_success_withCustomFileParams() {
        RandomGeneratorRequest request = new RandomGeneratorRequest(1L, 1L, 1L, 100L, 10);
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        RandomGeneratorResponse response = RandomGeneratorResponse.builder()
                .numbers(Arrays.asList(1L, 2L))
                .generatedCount(2)
                .build();
        when(randomNumberService.generateNumbers(request)).thenReturn(response);
        when(fileStorageService.saveNumbersToFile(anyList(), eq("customDir"), eq("my_file.txt"))).thenReturn("/customDir/my_file.txt");

        String viewName = controller.generateNumbers(request, bindingResult, "my_file.txt", "customDir", model);
        
        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(fileStorageService, times(1)).saveNumbersToFile(anyList(), eq("customDir"), eq("my_file.txt"));
        verify(model, times(1)).addAttribute(eq(SUCCESS_ATTRIBUTE), contains("File saved to: /customDir/my_file.txt"));
    }

    @Test
    void generateNumbers_bindingErrors_returnsMainView() {
        RandomGeneratorRequest request = new RandomGeneratorRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        
        String viewName = controller.generateNumbers(request, bindingResult, null, null, model);
        
        assertEquals(MAIN_PAGE_VIEW, viewName);
        verifyNoInteractions(randomNumberService);
        verifyNoInteractions(requestValidator);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void generateNumbers_illegalArgumentException_returnsError() {
        RandomGeneratorRequest request = new RandomGeneratorRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        String errorMessage = "Count must be positive";

        doThrow(new IllegalArgumentException(errorMessage))
                .when(requestValidator).validateRandomGeneratorRequest(any());

        String viewName = controller.generateNumbers(request, bindingResult, null, null, model);

        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq(ERROR_ATTRIBUTE), eq("Invalid request: Count must be positive"));
        verifyNoInteractions(randomNumberService);
    }

    @Test
    void generateNumbers_genericException_returnsError() {
        RandomGeneratorRequest request = new RandomGeneratorRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        String errorMessage = "I/O failure";

        doThrow(new RuntimeException(errorMessage))
                .when(randomNumberService).generateNumbers(any());
        
        String viewName = controller.generateNumbers(request, bindingResult, null, null, model);
        
        assertEquals(MAIN_PAGE_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq(ERROR_ATTRIBUTE), eq("Error generating numbers: I/O failure"));
    }

    @Test
    void performCesaroTest_success() {
        CesaroTestRequest request = CesaroTestRequest.builder().pairsCount(100).build();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        CesaroTestResponse response = CesaroTestResponse.builder().estimatedPi(3.1415).build();
        when(randomNumberService.performCesaroTest(request)).thenReturn(response);

        
        String viewName = controller.performCesaroTest(request, bindingResult, model);

        
        assertEquals(CESARO_TEST_VIEW, viewName);
        verify(requestValidator, times(1)).validateCesaroTestRequest(request);
        verify(randomNumberService, times(1)).performCesaroTest(request);
        verify(model, times(1)).addAttribute(eq(RESPONSE_ATTRIBUTE), eq(response));
        verify(model, times(1)).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("Cesaro test completed successfully"));
    }

    @Test
    void performCesaroTest_bindingErrors_returnsCesaroTestView() {
        
        CesaroTestRequest request = CesaroTestRequest.builder().build();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        
        String viewName = controller.performCesaroTest(request, bindingResult, model);

        
        assertEquals(CESARO_TEST_VIEW, viewName);
        verifyNoInteractions(randomNumberService);
    }

    @Test
    void performCesaroTest_illegalArgumentException_returnsError() {
        
        CesaroTestRequest request = CesaroTestRequest.builder().build();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        String errorMessage = "Pairs count must be positive";

        doThrow(new IllegalArgumentException(errorMessage))
                .when(requestValidator).validateCesaroTestRequest(any());

        
        String viewName = controller.performCesaroTest(request, bindingResult, model);

        
        assertEquals(CESARO_TEST_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq(ERROR_ATTRIBUTE), eq("Invalid request: Pairs count must be positive"));
        verifyNoInteractions(randomNumberService);
    }

    @Test
    void compareGenerators_success() {
        
        ComparisonTestRequest request = new ComparisonTestRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        ComparisonMetrics metrics = ComparisonMetrics.builder().betterGenerator("Lehmer Algorithm").build();
        ComparisonTestResponse response = ComparisonTestResponse.builder().comparison(metrics).build();
        when(randomNumberService.compareGenerators(request)).thenReturn(response);

        
        String viewName = controller.compareGenerators(request, bindingResult, model);

        
        assertEquals(COMPARE_PAGE_VIEW, viewName);
        verify(requestValidator, times(1)).validateComparisonTestRequest(request);
        verify(randomNumberService, times(1)).compareGenerators(request);
        verify(model, times(1)).addAttribute(eq(RESPONSE_ATTRIBUTE), eq(response));
        verify(model, times(1)).addAttribute(eq(SUCCESS_ATTRIBUTE), eq("Generator comparison completed successfully"));
    }

    @Test
    void compareGenerators_bindingErrors_returnsCompareView() {
        
        ComparisonTestRequest request = new ComparisonTestRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        
        String viewName = controller.compareGenerators(request, bindingResult, model);

        
        assertEquals(COMPARE_PAGE_VIEW, viewName);
        verifyNoInteractions(randomNumberService);
    }

    @Test
    void compareGenerators_illegalArgumentException_returnsError() {
        
        ComparisonTestRequest request = new ComparisonTestRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        String errorMessage = "Modulus must be positive";

        doThrow(new IllegalArgumentException(errorMessage))
                .when(requestValidator).validateComparisonTestRequest(any());

        
        String viewName = controller.compareGenerators(request, bindingResult, model);

        
        assertEquals(COMPARE_PAGE_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq(ERROR_ATTRIBUTE), eq("Invalid request: Modulus must be positive"));
        verifyNoInteractions(randomNumberService);
    }

    @Test
    void compareGenerators_genericException_returnsError() {
        ComparisonTestRequest request = new ComparisonTestRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        String errorMessage = "Unexpected database error";

        doThrow(new RuntimeException(errorMessage))
                .when(randomNumberService).compareGenerators(any());
        
        String viewName = controller.compareGenerators(request, bindingResult, model);

        assertEquals(COMPARE_PAGE_VIEW, viewName);
        verify(model, times(1)).addAttribute(eq(ERROR_ATTRIBUTE), eq("Error comparing generators: Unexpected database error"));
    }
}