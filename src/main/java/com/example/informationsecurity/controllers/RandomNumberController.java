package com.example.informationsecurity.controllers;

import com.example.informationsecurity.dto.CesaroTestRequest;
import com.example.informationsecurity.dto.CesaroTestResponse;
import com.example.informationsecurity.dto.ComparisonTestRequest;
import com.example.informationsecurity.dto.ComparisonTestResponse;
import com.example.informationsecurity.dto.RandomGeneratorRequest;
import com.example.informationsecurity.dto.RandomGeneratorResponse;
import com.example.informationsecurity.services.FileStorageService;
import com.example.informationsecurity.services.RandomNumberService;
import com.example.informationsecurity.utils.RequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Slf4j
@Controller
@RequestMapping("/lab1")
@RequiredArgsConstructor
public class RandomNumberController {

    private static final String MAIN_PAGE_VIEW = "lab1";
    private static final String COMPARE_PAGE_VIEW = "compare";
    private static final String PARAMETERS_PAGE_VIEW = "parameters";
    private static final String CESARO_TEST_VIEW = "cesaro-test";
    private static final String ERROR_ATTRIBUTE = "error";
    private static final String SUCCESS_ATTRIBUTE = "success";
    private static final String RESPONSE_ATTRIBUTE = "response";
    private static final String ERROR_MESSAGE = "Invalid request: ";

    private final RandomNumberService randomNumberService;
    private final RequestValidator requestValidator;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("generatorRequest", new RandomGeneratorRequest());
        return MAIN_PAGE_VIEW;
    }

    @PostMapping("/generate")
    public String generateNumbers(
            @Valid @ModelAttribute("generatorRequest") RandomGeneratorRequest request,
            BindingResult bindingResult,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String saveDirectory,
            Model model) {

        if (bindingResult.hasErrors()) {
            return MAIN_PAGE_VIEW;
        }

        try {
            requestValidator.validateRandomGeneratorRequest(request);

            RandomGeneratorResponse response = randomNumberService.generateNumbers(request);

            String directory = (saveDirectory != null && !saveDirectory.isBlank())
                    ? saveDirectory.trim()
                    : "generated";

            String name = (fileName != null && !fileName.isBlank())
                    ? fileName.trim()
                    : "random_numbers.txt";

            String savedFilePath = fileStorageService.saveNumbersToFile(response.getNumbers(), directory, name);

            model.addAttribute("generatorRequest", request);
            model.addAttribute(RESPONSE_ATTRIBUTE, response);
            model.addAttribute(SUCCESS_ATTRIBUTE,
                    String.format("Successfully generated %d numbers. File saved to: %s",
                            response.getGeneratedCount(), savedFilePath));

            log.info("Successfully generated {} numbers, saved to {}", response.getGeneratedCount(), savedFilePath);

            return MAIN_PAGE_VIEW;

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            model.addAttribute(ERROR_ATTRIBUTE, ERROR_MESSAGE + e.getMessage());
            return MAIN_PAGE_VIEW;

        } catch (Exception e) {
            log.error("Error generating numbers: {}", e.getMessage());
            model.addAttribute(ERROR_ATTRIBUTE, "Error generating numbers: " + e.getMessage());
            return MAIN_PAGE_VIEW;
        }
    }

    @GetMapping("/cesaro-test")
    public String cesaroTestPage(Model model) {
        model.addAttribute("cesaroRequest", new CesaroTestRequest());
        return CESARO_TEST_VIEW;
    }

    @PostMapping("/cesaro-test")
    public String performCesaroTest(
            @Valid @ModelAttribute("cesaroRequest") CesaroTestRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return CESARO_TEST_VIEW;
        }

        try {
            requestValidator.validateCesaroTestRequest(request);
            CesaroTestResponse response = randomNumberService.performCesaroTest(request);

            model.addAttribute("cesaroRequest", request);
            model.addAttribute(RESPONSE_ATTRIBUTE, response);
            model.addAttribute(SUCCESS_ATTRIBUTE, "Cesaro test completed successfully");

            log.info("Cesaro test completed. Estimated π: {}", response.getEstimatedPi());
            return CESARO_TEST_VIEW;

        } catch (IllegalArgumentException e) {
            log.error("Invalid Cesaro test parameters: {}", e.getMessage());
            model.addAttribute(ERROR_ATTRIBUTE, ERROR_MESSAGE + e.getMessage());
            return CESARO_TEST_VIEW;
        } catch (Exception e) {
            log.error("Error performing Cesaro test: {}", e.getMessage());
            model.addAttribute(ERROR_ATTRIBUTE, "Error performing Cesaro test: " + e.getMessage());
            return CESARO_TEST_VIEW;
        }
    }

    @GetMapping("/compare")
    public String comparePage(Model model) {
        model.addAttribute("comparisonRequest", new ComparisonTestRequest());
        return COMPARE_PAGE_VIEW;
    }

    @PostMapping("/compare")
    public String compareGenerators(
            @Valid @ModelAttribute("comparisonRequest") ComparisonTestRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return COMPARE_PAGE_VIEW;
        }

        try {
            requestValidator.validateComparisonTestRequest(request);
            ComparisonTestResponse response = randomNumberService.compareGenerators(request);

            model.addAttribute("comparisonRequest", request);
            model.addAttribute(RESPONSE_ATTRIBUTE, response);
            model.addAttribute(SUCCESS_ATTRIBUTE, "Generator comparison completed successfully");

            log.info("Comparison completed. Better generator: {}",
                    response.getComparison().getBetterGenerator());
            return COMPARE_PAGE_VIEW;

        } catch (IllegalArgumentException e) {
            log.error("Invalid comparison parameters: {}", e.getMessage());
            model.addAttribute(ERROR_ATTRIBUTE, ERROR_MESSAGE + e.getMessage());
            return COMPARE_PAGE_VIEW;
        } catch (Exception e) {
            log.error("Error comparing generators: {}", e.getMessage());
            model.addAttribute(ERROR_ATTRIBUTE, "Error comparing generators: " + e.getMessage());
            return COMPARE_PAGE_VIEW;
        }
    }

    @GetMapping("/parameters")
    public String parametersPage() {
        return PARAMETERS_PAGE_VIEW;
    }
}