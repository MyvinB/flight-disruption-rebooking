package com.eurowings.disruption.model;

public enum TransportMode {
    INTERNAL_FLIGHT(1),   // Lufthansa Group — most preferred on tie
    EXTERNAL_FLIGHT(2),   // Other airlines
    TRAIN(3);             // Rail — least preferred on tie

    private final int priority;

    TransportMode(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
