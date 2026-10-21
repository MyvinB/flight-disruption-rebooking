package com.eurowings.disruption.model;

/**
 * Origin and destination airport codes of a search, e.g. CGN -> BER.
 */
public record Route(String origin, String destination) {

    /**
     * Key used to group options by route in the API response, e.g. "CGN#BER".
     */
    public String key() {
        return origin + "#" + destination;
    }

    @Override
    public String toString() {
        return origin + "->" + destination;
    }
}
