package com.eurowings.disruption.normalizer;

import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import com.eurowings.disruption.raw.InternalFlightsRaw;
import com.eurowings.disruption.raw.TrainsRaw;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void internalOffersAreNormalized() throws IOException {
        List<Transport> options = new InternalFlightNormalizer().normalize(read("internal-flights.json", InternalFlightsRaw.class));

        assertThat(options).hasSize(9);
        Transport int5 = byId(options, "INT-5");
        assertThat(int5.getMode()).isEqualTo(TransportMode.INTERNAL_FLIGHT);
        assertThat(int5.getAmountInEuro()).isEqualByComparingTo("486.00");
        assertThat(int5.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-22T06:10:00+04:00"));
        assertThat(int5.getLegs().get(0)).isEqualTo(new Leg("Lufthansa", "LH", "CGN", "FRA", "1975"));
    }

    @Test
    void externalOffersAreNormalized() throws IOException {
        List<Transport> options = new ExternalFlightNormalizer().normalize(read("external-flights.json", ExternalFlightsRaw.class));

        assertThat(options).hasSize(6);
        Transport ext1 = byId(options, "EXT-1");
        assertThat(ext1.getAmountInEuro()).isEqualByComparingTo(new BigDecimal("189.00"));
        assertThat(ext1.getDepEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T20:40:00+02:00"));
        // Arrival time is local to DXB, not Berlin
        Transport ext3 = byId(options, "EXT-3");
        assertThat(ext3.getDepEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T19:55:00+02:00"));
        assertThat(ext3.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-22T06:05:00+04:00"));
        Leg firstLeg = byId(options, "EXT-6").getLegs().get(0);
        assertThat(firstLeg).isEqualTo(new Leg("Borealis Air", "BX", "PMI", "HAM", "771"));
        assertThat(firstLeg.toString()).isEqualTo("Borealis Air#PMI#HAM#771#BX");
    }

    @Test
    void trainConnectionsAreNormalized() throws IOException {
        List<Transport> options = new TrainNormalizer().normalize(read("trains.json", TrainsRaw.class));

        assertThat(options).hasSize(3);
        Transport trn1 = byId(options, "TRN-1");
        assertThat(trn1.getOrigin()).isEqualTo("CGN");
        assertThat(trn1.getDestination()).isEqualTo("BER");
        assertThat(trn1.getAmountInEuro()).isEqualByComparingTo("89.90");
        assertThat(trn1.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-22T00:18:00+02:00"));
        assertThat(trn1.getLegs()).containsExactly(new Leg("RheinSpree Express", "RX", "CGN", "BER", "1024"));
    }

    private <T> T read(String file, Class<T> type) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/data/" + file)) {
            return mapper.readValue(in, type);
        }
    }

    private static Transport byId(List<Transport> options, String id) {
        return options.stream().filter(t -> t.getOfferId().equals(id)).findFirst().orElseThrow();
    }
}
