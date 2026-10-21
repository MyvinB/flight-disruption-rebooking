package com.eurowings.disruption.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalFlightsRaw {

    private String source;
    private List<Result> results;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public List<Result> getResults() { return results; }
    public void setResults(List<Result> results) { this.results = results; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("offer_id")
        private String offerId;

        @JsonProperty("airline_name")
        private String airlineName;

        @JsonProperty("dep_airport")
        private String depAirport;

        @JsonProperty("arr_airport")
        private String arrAirport;

        @JsonProperty("dep_time")
        private String depTime;

        @JsonProperty("arr_time")
        private String arrTime;

        private int availability;

        @JsonProperty("price_cents")
        private int priceCents;

        private String currency;

        private List<LegRaw> legs;

        public String getOfferId() { return offerId; }
        public String getAirlineName() { return airlineName; }
        public String getDepAirport() { return depAirport; }
        public String getArrAirport() { return arrAirport; }
        public String getDepTime() { return depTime; }
        public String getArrTime() { return arrTime; }
        public int getAvailability() { return availability; }
        public int getPriceCents() { return priceCents; }
        public String getCurrency() { return currency; }
        public List<LegRaw> getLegs() { return legs; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LegRaw {
        @JsonProperty("flight_no")
        private String flightNo;

        private String from;
        private String to;

        public String getFlightNo() { return flightNo; }
        public String getFrom() { return from; }
        public String getTo() { return to; }
    }
}
