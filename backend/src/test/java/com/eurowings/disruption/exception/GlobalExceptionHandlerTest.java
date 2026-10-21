package com.eurowings.disruption.exception;

import com.eurowings.disruption.controller.DisruptionController;
import com.eurowings.disruption.service.DisruptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DisruptionController.class)
class GlobalExceptionHandlerTest {

    private static final String URL = "/api/disruption-options";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DisruptionService disruptionService;

    @Test
    void unavailableDataIsServiceUnavailable() throws Exception {
        when(disruptionService.getDisruptionOptions()).thenThrow(new DisruptionDataUnavailableException(
                "Could not load data/disruption.json", new UncheckedIOException(new IOException("disk"))));

        mockMvc.perform(get(URL))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Disruption data unavailable"))
                .andExpect(jsonPath("$.detail").value("The disruption could not be loaded. Please try again later."))
                .andExpect(jsonPath("$.code").value("DISRUPTION_DATA_UNAVAILABLE"))
                .andExpect(jsonPath("$.instance").value(URL))
                // The internal cause is logged, not returned
                .andExpect(content().string(not(containsString("disk"))));
    }

    @Test
    void invalidDataIsInternalErrorWithTheReason() throws Exception {
        when(disruptionService.getDisruptionOptions())
                .thenThrow(new InvalidDisruptionDataException("Duplicate bookingRef D3HRVC"));

        mockMvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid disruption data"))
                .andExpect(jsonPath("$.detail").value("Duplicate bookingRef D3HRVC"))
                .andExpect(jsonPath("$.code").value("INVALID_DISRUPTION_DATA"));
    }

    @Test
    void unexpectedErrorsDoNotLeakInternals() throws Exception {
        when(disruptionService.getDisruptionOptions())
                .thenThrow(new NullPointerException("Cannot invoke \"String.equals(Object)\" because \"origin\" is null"));

        mockMvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unexpected error"))
                .andExpect(jsonPath("$.detail").value("Something went wrong while finding options."))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("NullPointerException"))))
                .andExpect(content().string(not(containsString("origin"))));
    }

    @Test
    void unsupportedMethodIsAProblemResponse() throws Exception {
        mockMvc.perform(post(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"));
    }

    @Test
    void unknownPathIsAProblemResponse() throws Exception {
        mockMvc.perform(get("/api/disruption-option"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

}
