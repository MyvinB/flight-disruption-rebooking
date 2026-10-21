package com.eurowings.disruption.model.impl;

import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.InternalFlightsRaw;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapts a raw Lufthansa Group offer into the unified Transport interface.
 * All transformation (parsing, stripping) happens in the constructor.
 * Getters are simple field returns.
 */
public class InternalFlightTransport implements Transport {

    private final String offerId;
    private final BigDecimal amountInEuro;
    private final int availableSeats;
    private final OffsetDateTime depEstTime;
    private final OffsetDateTime arrEstTime;
    private final String origin;
    private final String destination;
    private final List<Leg> legs;

    public InternalFlightTransport(InternalFlightsRaw.Offer offer) {
        this.offerId        = offer.getOfferId();
        // Price already in EUR as double — convert to BigDecimal
        this.amountInEuro   = BigDecimal.valueOf(offer.getPricePerSeat().getAmount())
                                        .setScale(2, java.math.RoundingMode.HALF_UP);
        this.availableSeats = offer.getAvailableSeats();
        // ISO 8601 with offset e.g. "2026-07-21T19:40:00+02:00" — parse directly
        this.depEstTime     = OffsetDateTime.parse(offer.getDeparture());
        this.arrEstTime     = OffsetDateTime.parse(offer.getArrival());
        this.origin         = offer.getDepartureAirport();
        this.destination    = offer.getArrivalAirport();
        this.legs           = offer.getLegs().stream()
                                   .map(l -> buildLeg(l, offer.getCarrier(), offer.getCarrierCode()))
                                   .collect(Collectors.toList());
    }

    /**
     * Strips the carrier code prefix from the flight number.
     * "LH 1975" with carrierCode "LH" → number "1975"
     */
    private Leg buildLeg(InternalFlightsRaw.LegRaw raw, String carrier, String carrierCode) {
        String number = raw.getFlightNumber()
                           .replace(carrierCode + " ", "")
                           .trim();
        return new Leg(carrier, carrierCode, raw.getFrom(), raw.getTo(), number);
    }

    @Override public String         getOfferId()        { return offerId; }
    @Override public TransportMode  getMode()           { return TransportMode.INTERNAL_FLIGHT; }
    @Override public BigDecimal     getAmountInEuro()   { return amountInEuro; }
    @Override public int            getAvailableSeats() { return availableSeats; }
    @Override public OffsetDateTime getDepEstTime()     { return depEstTime; }
    @Override public OffsetDateTime getArrEstTime()     { return arrEstTime; }
    @Override public String         getOrigin()         { return origin; }
    @Override public String         getDestination()    { return destination; }
    @Override public List<Leg>      getLegs()           { return legs; }
}
