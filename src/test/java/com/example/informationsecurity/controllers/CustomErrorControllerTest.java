package com.example.informationsecurity.controllers;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomErrorController.class)
class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handleError_withStatus404_shouldReturnErrorViewWithCorrectAttributes() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Not Found")
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/some/path"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("error", "Page Not Found"))
                .andExpect(model().attribute("message", "Not Found"))
                .andExpect(model().attributeExists("timestamp"))
                .andExpect(model().attribute("path", "/some/path"));
    }

    @Test
    void handleError_withUnknownStatus_shouldReturnGenericErrorTitle() throws Exception {
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 999)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Weird Error"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 999))
                .andExpect(model().attribute("error", "Error 999"))
                .andExpect(model().attribute("message", "Weird Error"))
                .andExpect(model().attributeExists("timestamp"));
    }

    @Test
    void handleError_withoutStatus_shouldDefaultTo500() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("error", "Internal Server Error"))
                .andExpect(model().attribute("message", "Unknown error"))
                .andExpect(model().attributeExists("timestamp"));
    }
}
