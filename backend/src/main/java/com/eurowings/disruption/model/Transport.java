package com.eurowings.disruption.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contract that all transport sources must fulfill.
 * Each source (internal flights, external flights, trains) implements this interface,
 * transforming their raw format into a unified model during construction.
 * This is the Adapter pattern — the ranking service works against this interface
 * without knowing which source the data came from.
 */
public interface Transport {

    String getOfferId();

    TransportMode getMode();

    BigDecimal getAmountInEuro();

    int getAvailableSeats();

    OffsetDateTime getDepEstTime();

    OffsetDateTime getArrEstTime();

    String getOrigin();

    String getDestination();

    List<Leg> getLegs();
}
