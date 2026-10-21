package com.eurowings.disruption.dto;

import java.util.List;
import java.util.Map;

/**
 * transportOptions maps each searched route ("CGN#BER") to its options, ranked best first.
 * recommendations maps bookingRef (PNR) to its recommendation, in the order of the affected bookings.
 */
public record DisruptionOptionsResponse(
        DisruptionSummaryDto disruption,
        Map<String, List<TransportOptionDto>> transportOptions,
        Map<String, RecommendationDto> recommendations
) {
}
