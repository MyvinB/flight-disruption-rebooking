package com.eurowings.disruption.dto;

import com.eurowings.disruption.raw.DisruptionData;

/**
 * Recommendation for one booking (keyed by bookingRef in the response).
 * journey is the passenger's original routing, so agents can compare it with the alternative.
 * transport is null when no option on the booking's journey has enough seats.
 */
public record RecommendationDto(
        int passengers,
        DisruptionData.Journey journey,
        TransportOptionDto transport
) {
}
