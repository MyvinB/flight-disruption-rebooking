package com.eurowings.disruption.dto;

import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * API view of a normalized Transport option.
 * suggestedPassengers is the number of passengers this offer is recommended to. It is informational
 * only (nobody is booked yet) and can exceed totalSeats, which flags offers likely to sell out.
 */
public record TransportOptionDto(
        String offerId,
        TransportMode source,
        String operator,
        String origin,
        String destination,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime departure,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime arrival,
        int totalSeats,
        int suggestedPassengers,
        BigDecimal priceEur,
        List<Leg> legs
) {
    public static TransportOptionDto from(Transport t, int suggestedPassengers) {
        String operator = t.getLegs().isEmpty() ? null : t.getLegs().get(0).operatorName();
        return new TransportOptionDto(
                t.getOfferId(),
                t.getMode(),
                operator,
                t.getOrigin(),
                t.getDestination(),
                t.getDepEstTime(),
                t.getArrEstTime(),
                t.getAvailableSeats(),
                suggestedPassengers,
                t.getAmountInEuro(),
                t.getLegs());
    }
}
