package com.eurowings.disruption.model;

import com.eurowings.disruption.MockData;
import com.eurowings.disruption.dto.TransportOptionDto;
import com.eurowings.disruption.model.impl.ExternalFlightTransport;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {

    @Test
    void routeKeyMatchesTheResponseMapKeys() {
        Route route = new Route("CGN", "DXB");

        assertThat(route.key()).isEqualTo("CGN#DXB");
        assertThat(route.toString()).isEqualTo("CGN->DXB");
        assertThat(route).isEqualTo(new Route("CGN", "DXB")).isNotEqualTo(new Route("DXB", "CGN"));
    }

    @Test
    void knowsTheZoneOfEveryAirportInTheMockData() {
        assertThat(AirportZones.of("DXB")).isEqualTo(ZoneId.of("Asia/Dubai"));
        assertThat(AirportZones.of("PMI")).isEqualTo(ZoneId.of("Europe/Madrid"));
        assertThat(AirportZones.of("VIE")).isEqualTo(ZoneId.of("Europe/Vienna"));
        assertThat(AirportZones.of("ATH")).isEqualTo(ZoneId.of("Europe/Athens"));
        assertThat(AirportZones.of("CGN")).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void fallsBackToBerlinForUnknownAirports() {
        assertThat(AirportZones.of("JFK")).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void ranksLufthansaGroupBeforeOtherAirlinesBeforeTrains() {
        assertThat(TransportMode.INTERNAL_FLIGHT.getPriority())
                .isLessThan(TransportMode.EXTERNAL_FLIGHT.getPriority());
        assertThat(TransportMode.EXTERNAL_FLIGHT.getPriority())
                .isLessThan(TransportMode.TRAIN.getPriority());
    }

    @Test
    void dtoTakesOperatorFromTheFirstLeg() {
        TransportOptionDto dto = TransportOptionDto.from(external(r -> { }), 30);

        assertThat(dto.offerId()).isEqualTo("EXT-3");
        assertThat(dto.operator()).isEqualTo("Meridian Air");
        assertThat(dto.source()).isEqualTo(TransportMode.EXTERNAL_FLIGHT);
        assertThat(dto.totalSeats()).isEqualTo(9);
        assertThat(dto.suggestedPassengers()).isEqualTo(30);
        assertThat(dto.priceEur()).isEqualByComparingTo("424.00");
        assertThat(dto.legs()).hasSize(2);
    }

    @Test
    void dtoHasNoOperatorWhenThereAreNoLegs() {
        TransportOptionDto dto = TransportOptionDto.from(external(r -> r.set("legs", r.arrayNode())), 0);

        assertThat(dto.operator()).isNull();
        assertThat(dto.legs()).isEmpty();
    }

    private static ExternalFlightTransport external(Consumer<ObjectNode> change) {
        return new ExternalFlightTransport(MockData.record(
                "external-flights.json", "/results", "offer_id", "EXT-3", ExternalFlightsRaw.Result.class, change));
    }
}
