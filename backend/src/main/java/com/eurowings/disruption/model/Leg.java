package com.eurowings.disruption.model;

/**
 * Represents a single flight leg or train segment within a Transport option.
 * toString() format: operatorName#from#to#number#operatorCode
 */
public record Leg(
        String operatorName,   // "Lufthansa" / "Nimbus Airways" / "RheinSpree Express"
        String operatorCode,   // "LH" / "BX" (parsed from flight_no) / "RX"
        String from,           // origin airport code e.g. "CGN"
        String to,             // destination airport code e.g. "FRA"
        String number          // stripped number: "1975" from "LH 1975", "771" from "BX771", "1024" from "RX 1024"
) {
    @Override
    public String toString() {
        return operatorName + "#" + from + "#" + to + "#" + number + "#" + operatorCode;
    }
}
