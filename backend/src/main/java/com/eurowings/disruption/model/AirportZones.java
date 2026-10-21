package com.eurowings.disruption.model;

import java.time.ZoneId;
import java.util.Map;

/**
 * Time zone of each airport, used to interpret local times that come without an offset.
 * Schedules give each time in the local time of the airport it refers to.
 */
public final class AirportZones {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");

    private static final Map<String, ZoneId> ZONES = Map.of(
            "CGN", ZoneId.of("Europe/Berlin"),
            "DUS", ZoneId.of("Europe/Berlin"),
            "BER", ZoneId.of("Europe/Berlin"),
            "HAM", ZoneId.of("Europe/Berlin"),
            "FRA", ZoneId.of("Europe/Berlin"),
            "MUC", ZoneId.of("Europe/Berlin"),
            "PMI", ZoneId.of("Europe/Madrid"),
            "VIE", ZoneId.of("Europe/Vienna"),
            "ATH", ZoneId.of("Europe/Athens"),
            "DXB", ZoneId.of("Asia/Dubai"));

    private AirportZones() {
    }

    /**
     * Falls back to Europe/Berlin for airports not in the table.
     */
    public static ZoneId of(String airportCode) {
        return ZONES.getOrDefault(airportCode, DEFAULT_ZONE);
    }
}
