package com.eurowings.disruption.raw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainsRaw {

    private String provider;
    private List<Query> queries;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public List<Query> getQueries() { return queries; }
    public void setQueries(List<Query> queries) { this.queries = queries; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        private String fromCity;
        private String fromAirport;
        private String toCity;
        private String toAirport;
        private List<Connection> connections;

        public String getFromCity() { return fromCity; }
        public void setFromCity(String fromCity) { this.fromCity = fromCity; }
        public String getFromAirport() { return fromAirport; }
        public void setFromAirport(String fromAirport) { this.fromAirport = fromAirport; }
        public String getToCity() { return toCity; }
        public void setToCity(String toCity) { this.toCity = toCity; }
        public String getToAirport() { return toAirport; }
        public void setToAirport(String toAirport) { this.toAirport = toAirport; }
        public List<Connection> getConnections() { return connections; }
        public void setConnections(List<Connection> connections) { this.connections = connections; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Connection {
        private String id;
        private String operator;
        private String operatorCode;
        private String trainNumber;
        private String departureStation;
        private String arrivalStation;
        private String departure;
        private String arrival;
        private int availableSeats;
        private String price;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getOperatorCode() { return operatorCode; }
        public void setOperatorCode(String operatorCode) { this.operatorCode = operatorCode; }
        public String getTrainNumber() { return trainNumber; }
        public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
        public String getDepartureStation() { return departureStation; }
        public void setDepartureStation(String departureStation) { this.departureStation = departureStation; }
        public String getArrivalStation() { return arrivalStation; }
        public void setArrivalStation(String arrivalStation) { this.arrivalStation = arrivalStation; }
        public String getDeparture() { return departure; }
        public void setDeparture(String departure) { this.departure = departure; }
        public String getArrival() { return arrival; }
        public void setArrival(String arrival) { this.arrival = arrival; }
        public int getAvailableSeats() { return availableSeats; }
        public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
    }
}
