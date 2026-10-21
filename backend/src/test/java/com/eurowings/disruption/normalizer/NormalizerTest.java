package com.eurowings.disruption.normalizer;

import com.eurowings.disruption.MockData;
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
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void externalNormalizerKeepsOffersFromOtherAirports() throws IOException {
        // Route filtering is the search's job: EXT-2 departs from DUS and is still normalized here
        List<Transport> options = new ExternalFlightNormalizer().normalize(read("external-flights.json", ExternalFlightsRaw.class));

        assertThat(byId(options, "EXT-2").getOrigin()).isEqualTo("DUS");
    }

    @Test
    void trainQueriesWithoutConnectionsContributeNothing() throws IOException {
        TrainsRaw raw = read("trains.json", TrainsRaw.class);
        // CGN -> DXB and PMI -> BER have no train connections in the mock data
        raw.setQueries(raw.getQueries().subList(1, 3));

        assertThat(new TrainNormalizer().normalize(raw)).isEmpty();
    }

    @Test
    void internalSearchesWithoutOffersAreSkipped() throws IOException {
        InternalFlightsRaw raw = read("internal-flights.json", InternalFlightsRaw.class);
        raw.getSearches().get(0).setOffers(null);

        List<Transport> options = new InternalFlightNormalizer().normalize(raw);

        // The 4 CGN -> BER offers are gone, the other searches are still normalized
        assertThat(options).extracting(Transport::getOfferId)
                .containsExactly("INT-5", "INT-6", "INT-7", "INT-8", "INT-9");
    }

    @Test
    void trainQueryWithNullConnectionsIsSkipped() throws IOException {
        TrainsRaw raw = read("trains.json", TrainsRaw.class);
        raw.getQueries().get(0).setConnections(null);

        assertThat(new TrainNormalizer().normalize(raw)).isEmpty();
    }

    @Test
    void missingResponsesNormalizeToNoOptions() {
        assertThat(new InternalFlightNormalizer().normalize(null)).isEmpty();
        assertThat(new ExternalFlightNormalizer().normalize(null)).isEmpty();
        assertThat(new TrainNormalizer().normalize(null)).isEmpty();
    }

    @Test
    void responsesWithoutResultListsNormalizeToNoOptions() {
        assertThat(new InternalFlightNormalizer().normalize(new InternalFlightsRaw())).isEmpty();
        assertThat(new ExternalFlightNormalizer().normalize(new ExternalFlightsRaw())).isEmpty();
        assertThat(new TrainNormalizer().normalize(new TrainsRaw())).isEmpty();
    }

    @Test
    void oneMalformedOfferFailsTheWholeSource() throws IOException {
        ExternalFlightsRaw raw = read("external-flights.json", ExternalFlightsRaw.class);
        ExternalFlightsRaw.Result broken = MockData.record("external-flights.json", "/results",
                "offer_id", "EXT-1", ExternalFlightsRaw.Result.class, r -> r.put("dep_time", "tomorrow"));
        raw.setResults(List.of(raw.getResults().get(2), broken));

        // The service turns this into "source unavailable" (see DisruptionServiceTest)
        assertThatThrownBy(() -> new ExternalFlightNormalizer().normalize(raw))
                .isInstanceOf(DateTimeParseException.class);
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
