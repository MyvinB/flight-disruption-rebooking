package com.eurowings.disruption.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    void serializesTimesWithTheirLocalOffsetAndPricesInEuros() throws Exception {
        mockMvc.perform(get("/api/disruption-options"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].source").value("EXTERNAL_FLIGHT"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].operator").value("Meridian Air"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].departure").value("2026-07-21T19:55:00+02:00"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].arrival").value("2026-07-22T06:05:00+04:00"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].priceEur").value(424.0))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].totalSeats").value(9))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].suggestedPassengers").value(30))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].legs", hasSize(2)))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].legs[1].from").value("VIE"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][0].legs[1].operatorCode").value("MD"))
                .andExpect(jsonPath("$.disruption.affectedBookings").value(48))
                .andExpect(jsonPath("$.disruption.affectedPassengers").value(120));
    }

    @Test
    void neverOffersTrainsToDubaiOrFromPalma() throws Exception {
        mockMvc.perform(get("/api/disruption-options"))
                .andExpect(jsonPath("$.transportOptions['CGN#DXB'][*].source", not(hasItem("TRAIN"))))
                .andExpect(jsonPath("$.transportOptions['PMI#BER'][*].source", not(hasItem("TRAIN"))))
                .andExpect(jsonPath("$.transportOptions['CGN#BER'][*].offerId", not(hasItem("EXT-2"))));
    }

    @Test
    void answersFasterThanTheSourcesRunInSequence() throws Exception {
        long start = System.nanoTime();
        mockMvc.perform(get("/api/disruption-options")).andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // 3 routes x (200 + 500 + 1500) ms = 6.6 s in sequence; in parallel about 1.5 s
        assertThat(elapsedMs).isLessThan(3000);
    }

    @Test
    void allowsCallsFromAnyOrigin() throws Exception {
        mockMvc.perform(get("/api/disruption-options").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    @Test
    void rejectsOtherHttpMethods() throws Exception {
        mockMvc.perform(post("/api/disruption-options")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownPathsAreNotFound() throws Exception {
        mockMvc.perform(get("/disruption-options")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/disruption-option")).andExpect(status().isNotFound());
    }

    @Test
    void exposesHealthForContainerChecks() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void doesNotExposeOtherActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }
}
