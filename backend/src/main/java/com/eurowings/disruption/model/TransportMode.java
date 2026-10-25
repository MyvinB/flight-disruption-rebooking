package com.eurowings.disruption.model;

/**
 * Which source an option comes from. The priority only decides between options
 * that arrive at the same time for the same price (lower = preferred).
 */
public enum TransportMode {
    INTERNAL_FLIGHT(1),   // Lufthansa Group
    EXTERNAL_FLIGHT(2),   // Other airlines
    TRAIN(3);

    private final int priority;

    TransportMode(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
