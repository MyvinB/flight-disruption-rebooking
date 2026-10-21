package com.eurowings.disruption.dto;

import com.eurowings.disruption.raw.DisruptionData;

public record DisruptionSummaryDto(
        String flight,
        String origin,
        String destination,
        String scheduledDeparture,
        String scheduledArrival,
        String status,
        String reason,
        int affectedBookings,
        int affectedPassengers
) {
    public static DisruptionSummaryDto from(DisruptionData.DisruptionInfo info) {
        return new DisruptionSummaryDto(
                info.getFlight(),
                info.getOrigin(),
                info.getDestination(),
                info.getScheduledDeparture(),
                info.getScheduledArrival(),
                info.getStatus(),
                info.getReason(),
                info.getAffectedBookings(),
                info.getAffectedPassengers());
    }
}
