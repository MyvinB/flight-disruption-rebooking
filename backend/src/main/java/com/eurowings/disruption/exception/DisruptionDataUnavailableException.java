package com.eurowings.disruption.exception;

/**
 * The disruption and its affected bookings could not be loaded, e.g. because the source is down.
 * Mapped to 503: retrying later may succeed.
 */
public class DisruptionDataUnavailableException extends RuntimeException {

    public DisruptionDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
