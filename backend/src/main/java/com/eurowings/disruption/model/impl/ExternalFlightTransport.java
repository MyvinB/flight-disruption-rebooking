package com.eurowings.disruption.model.impl;

import com.eurowings.disruption.model.AirportZones;
import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.ExternalFlightsRaw;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adapts a raw external airline offer into the unified Transport interface.
 *
 * Key normalizations performed in constructor:
 *  - price_cents (int) → amountInEuro (BigDecimal)  e.g. 18900 → 189.00
 *  - dep_time/arr_time (String, no timezone) → OffsetDateTime in the local zone of dep_airport / arr_airport
 *  - flight_no "BX771" → operatorCode="BX", number="771" (regex split)
 */
public class ExternalFlightTransport implements Transport {

    private static final DateTimeFormatter EXT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // Splits "BX771" → group(1)="BX", group(2)="771"
    private static final Pattern FLIGHT_NO_PATTERN = Pattern.compile("^([A-Z]+)(\\d+.*)$");

    private final String offerId;
    private final BigDecimal amountInEuro;
    private final int availableSeats;
    private final OffsetDateTime depEstTime;
    private final OffsetDateTime arrEstTime;
    private final String origin;
    private final String destination;
    private final List<Leg> legs;

    public ExternalFlightTransport(ExternalFlightsRaw.Result result) {
        this.offerId        = result.getOfferId();
        // Convert cents to EUR
        this.amountInEuro   = BigDecimal.valueOf(result.getPriceCents())
                                        .divide(BigDecimal.valueOf(100))
                                        .setScale(2, java.math.RoundingMode.HALF_UP);
        this.availableSeats = result.getAvailability();
        // No timezone in source — times are local to the departure / arrival airport
        this.depEstTime     = parseDateTime(result.getDepTime(), result.getDepAirport());
        this.arrEstTime     = parseDateTime(result.getArrTime(), result.getArrAirport());
        this.origin         = result.getDepAirport();
        this.destination    = result.getArrAirport();
        this.legs           = result.getLegs().stream()
                                    .map(l -> buildLeg(l, result.getAirlineName()))
                                    .collect(Collectors.toList());
    }

    private OffsetDateTime parseDateTime(String raw, String airportCode) {
        LocalDateTime ldt = LocalDateTime.parse(raw, EXT_DATE_FORMAT);
        return ldt.atZone(AirportZones.of(airportCode)).toOffsetDateTime();
    }

    /**
     * Parses "BX771" into operatorCode="BX", number="771".
     * Falls back gracefully if pattern does not match.
     */
    private Leg buildLeg(ExternalFlightsRaw.LegRaw raw, String airlineName) {
        Matcher m = FLIGHT_NO_PATTERN.matcher(raw.getFlightNo());
        String operatorCode;
        String number;
        if (m.matches()) {
            operatorCode = m.group(1); // "BX"
            number       = m.group(2); // "771"
        } else {
            operatorCode = "";
            number       = raw.getFlightNo();
        }
        return new Leg(airlineName, operatorCode, raw.getFrom(), raw.getTo(), number);
    }

    @Override public String         getOfferId()        { return offerId; }
    @Override public TransportMode  getMode()           { return TransportMode.EXTERNAL_FLIGHT; }
    @Override public BigDecimal     getAmountInEuro()   { return amountInEuro; }
    @Override public int            getAvailableSeats() { return availableSeats; }
    @Override public OffsetDateTime getDepEstTime()     { return depEstTime; }
    @Override public OffsetDateTime getArrEstTime()     { return arrEstTime; }
    @Override public String         getOrigin()         { return origin; }
    @Override public String         getDestination()    { return destination; }
    @Override public List<Leg>      getLegs()           { return legs; }
}
