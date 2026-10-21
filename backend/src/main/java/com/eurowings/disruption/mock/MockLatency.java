package com.eurowings.disruption.mock;

/**
 * Simulates network latency of a remote source.
 */
final class MockLatency {

    private MockLatency() {
    }

    static void simulate(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mock call interrupted", e);
        }
    }
}
