package com.eurowings.disruption.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalFlightsRaw {

    private String source;
    private String generatedAt;
    private List<Search> searches;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public List<Search> getSearches() { return searches; }
    public void setSearches(List<Search> searches) { this.searches = searches; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Search {
        private String searchId;
        private Query query;
        private List<Offer> offers;

        public String getSearchId() { return searchId; }
        public void setSearchId(String searchId) { this.searchId = searchId; }
        public Query getQuery() { return query; }
        public void setQuery(Query query) { this.query = query; }
        public List<Offer> getOffers() { return offers; }
        public void setOffers(List<Offer> offers) { this.offers = offers; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        private String origin;
        private String destination;

        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Offer {
        private String offerId;
        private String carrier;
        private String carrierCode;
        private String departureAirport;
        private String arrivalAirport;
        private String departure;
        private String arrival;
        private int availableSeats;
        private Price pricePerSeat;
        private List<LegRaw> legs;

        public String getOfferId() { return offerId; }
        public void setOfferId(String offerId) { this.offerId = offerId; }
        public String getCarrier() { return carrier; }
        public void setCarrier(String carrier) { this.carrier = carrier; }
        public String getCarrierCode() { return carrierCode; }
        public void setCarrierCode(String carrierCode) { this.carrierCode = carrierCode; }
        public String getDepartureAirport() { return departureAirport; }
        public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }
        public String getArrivalAirport() { return arrivalAirport; }
        public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }
        public String getDeparture() { return departure; }
        public void setDeparture(String departure) { this.departure = departure; }
        public String getArrival() { return arrival; }
        public void setArrival(String arrival) { this.arrival = arrival; }
        public int getAvailableSeats() { return availableSeats; }
        public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
        public Price getPricePerSeat() { return pricePerSeat; }
        public void setPricePerSeat(Price pricePerSeat) { this.pricePerSeat = pricePerSeat; }
        public List<LegRaw> getLegs() { return legs; }
        public void setLegs(List<LegRaw> legs) { this.legs = legs; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private double amount;
        private String currency;

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegRaw {
        private String flightNumber;
        private String from;
        private String to;

        public String getFlightNumber() { return flightNumber; }
        public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }
}
