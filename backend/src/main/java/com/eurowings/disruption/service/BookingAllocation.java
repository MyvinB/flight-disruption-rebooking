package com.eurowings.disruption.service;

import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.raw.DisruptionData;

import java.util.Optional;

/**
 * Recommendation for one booking. transport is empty when no option with enough seats was left.
 */
public record BookingAllocation(DisruptionData.Booking booking, Optional<Transport> transport) {
}
