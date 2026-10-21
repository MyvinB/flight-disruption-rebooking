package com.eurowings.disruption.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DisruptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsOneRecommendationPerBooking() throws Exception {
        mockMvc.perform(get("/api/disruption-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disruption.flight").value("EW 4711"))
                // Only the affected routes are searched, so EXT-2 (DUS -> BER) never comes back
                .andExpect(jsonPath("$.transportOptions", aMapWithSize(3)))
                .andExpect(jsonPath("$.transportOptions['CGN#BER']", hasSize(8)))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB']", hasSize(5)))
                .andExpect(jsonPath("$.transportOptions['PMI#BER']", hasSize(4)))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].offerId").value("EXT-3"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][1].arrival").value("2026-07-22T06:10:00+04:00"))
                .andExpect(jsonPath("$.recommendations", aMapWithSize(48)))
                // EXT-3 lands at 06:05 Dubai time, 5 minutes before INT-5, so it is picked first for DXB
                .andExpect(jsonPath("$.recommendations.AL0SAU.passengers").value(4))
                .andExpect(jsonPath("$.recommendations.AL0SAU.transport.offerId").value("EXT-3"))
                // Original routing is included so agents can compare it with the alternative
                .andExpect(jsonPath("$.recommendations.AJWFFO.journey.origin").value("PMI"))
                .andExpect(jsonPath("$.recommendations.AJWFFO.journey.segments[0].flight").value("EW 9585"))
                .andExpect(jsonPath("$.recommendations.AJWFFO.journey.segments[0].status").value("NOT_DEPARTED"))
                .andExpect(jsonPath("$.recommendations.AJWFFO.journey.segments[1].status").value("CANCELLED"));
    }
}
