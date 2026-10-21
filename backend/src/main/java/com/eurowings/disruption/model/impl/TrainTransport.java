package com.eurowings.disruption.model.impl;

import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.TrainsRaw;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Adapts a raw train connection into the unified Transport interface.
 *
 * Key normalizations performed in constructor:
 *  - price "89.90 EUR" (String) → BigDecimal 89.90
 *  - departure/arrival (no timezone) → OffsetDateTime (assumed Europe/Berlin)
 *  - trainNumber "RX 1024" → strip operatorCode "RX " → number "1024"
 *  - Train is a single-segment journey — one Leg
 */
public class TrainTransport implements Transport {

    private static final DateTimeFormatter TRAIN_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final ZoneId EUROPE_BERLIN = ZoneId.of("Europe/Berlin");

    private final String offerId;
    private final BigDecimal amountInEuro;
    private final int availableSeats;
    private final OffsetDateTime depEstTime;
    private final OffsetDateTime arrEstTime;
    private final String origin;
    private final String destination;
    private final List<Leg> legs;

    public TrainTransport(TrainsRaw.Connection connection, String fromAirport, String toAirport) {
        this.offerId        = connection.getId();
        // Parse "89.90 EUR" → 89.90
        this.amountInEuro   = parsePriceString(connection.getPrice());
        this.availableSeats = connection.getAvailableSeats();
        this.depEstTime     = parseDateTime(connection.getDeparture());
        this.arrEstTime     = parseDateTime(connection.getArrival());
        this.origin         = fromAirport;
        this.destination    = toAirport;
        // Trains are single-segment; use airport codes as from/to
        this.legs           = List.of(buildLeg(connection, fromAirport, toAirport));
    }

    /**
     * Parses "89.90 EUR" → BigDecimal 89.90
     */
    private BigDecimal parsePriceString(String priceStr) {
        String numeric = priceStr.replace("EUR", "").trim();
        return new BigDecimal(numeric).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private OffsetDateTime parseDateTime(String raw) {
        LocalDateTime ldt = LocalDateTime.parse(raw, TRAIN_DATE_FORMAT);
        return ldt.atZone(EUROPE_BERLIN).toOffsetDateTime();
    }

    /**
     * Strips operator code prefix from train number.
     * "RX 1024" with operatorCode "RX" → number "1024"
     */
    private Leg buildLeg(TrainsRaw.Connection conn, String from, String to) {
        String number = conn.getTrainNumber()
                            .replace(conn.getOperatorCode() + " ", "")
                            .trim();
        return new Leg(conn.getOperator(), conn.getOperatorCode(), from, to, number);
    }

    @Override public String         getOfferId()        { return offerId; }
    @Override public TransportMode  getMode()           { return TransportMode.TRAIN; }
    @Override public BigDecimal     getAmountInEuro()   { return amountInEuro; }
    @Override public int            getAvailableSeats() { return availableSeats; }
    @Override public OffsetDateTime getDepEstTime()     { return depEstTime; }
    @Override public OffsetDateTime getArrEstTime()     { return arrEstTime; }
    @Override public String         getOrigin()         { return origin; }
    @Override public String         getDestination()    { return destination; }
    @Override public List<Leg>      getLegs()           { return legs; }
}
