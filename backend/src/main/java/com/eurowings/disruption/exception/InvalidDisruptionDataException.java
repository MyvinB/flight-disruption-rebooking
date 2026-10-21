package com.eurowings.disruption.exception;

/**
 * The disruption data was loaded but breaks an assumption the service relies on,
 * e.g. a missing journey or a booking reference that occurs twice.
 * Mapped to 500: retrying will not help until the data is fixed.
 */
public class InvalidDisruptionDataException extends RuntimeException {

    public InvalidDisruptionDataException(String message) {
        super(message);
    }
}
