package com.eurowings.disruption.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DisruptionData {

    private DisruptionInfo disruption;
    private List<Booking> bookings;

    public DisruptionInfo getDisruption() { return disruption; }
    public void setDisruption(DisruptionInfo disruption) { this.disruption = disruption; }
    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisruptionInfo {
        private String flight;
        private String origin;
        private String destination;
        private String scheduledDeparture;
        private String scheduledArrival;
        private String status;
        private String reason;
        private String cancelledAt;
        private int affectedBookings;
        private int affectedPassengers;

        public String getFlight() { return flight; }
        public void setFlight(String flight) { this.flight = flight; }
        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getScheduledDeparture() { return scheduledDeparture; }
        public void setScheduledDeparture(String scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }
        public String getScheduledArrival() { return scheduledArrival; }
        public void setScheduledArrival(String scheduledArrival) { this.scheduledArrival = scheduledArrival; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
        public int getAffectedBookings() { return affectedBookings; }
        public void setAffectedBookings(int affectedBookings) { this.affectedBookings = affectedBookings; }
        public int getAffectedPassengers() { return affectedPassengers; }
        public void setAffectedPassengers(int affectedPassengers) { this.affectedPassengers = affectedPassengers; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Booking {
        private String bookingRef;
        private int passengers;
        private Journey journey;

        public String getBookingRef() { return bookingRef; }
        public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
        public int getPassengers() { return passengers; }
        public void setPassengers(int passengers) { this.passengers = passengers; }
        public Journey getJourney() { return journey; }
        public void setJourney(Journey journey) { this.journey = journey; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Journey {
        private String origin;
        private String destination;
        private List<Segment> segments;

        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public List<Segment> getSegments() { return segments; }
        public void setSegments(List<Segment> segments) { this.segments = segments; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Segment {
        private String flight;
        private String from;
        private String to;
        private String departure;
        private String arrival;
        private String status;

        public String getFlight() { return flight; }
        public void setFlight(String flight) { this.flight = flight; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getDeparture() { return departure; }
        public void setDeparture(String departure) { this.departure = departure; }
        public String getArrival() { return arrival; }
        public void setArrival(String arrival) { this.arrival = arrival; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
