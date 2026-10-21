package com.eurowings.disruption.service;

import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.raw.DisruptionData;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Picks one recommendation per booking.
 *
 * For each booking the options serving its final destination (journey.origin -> journey.destination)
 * whose reported seats fit the whole party are ranked by earliest arrival, then by source priority.
 * The top option is recommended.
 *
 * Each booking is decided on its own: recommendations to other bookings do not reduce the seats
 * considered, since nobody is booked yet. Competition for seats is resolved at booking time
 * by the inventory owner.
 */
@Service
public class RankingService {

    static final Comparator<Transport> RANKING =
            Comparator.comparing(Transport::getArrEstTime)
                    .thenComparingInt(t -> t.getMode().getPriority());

    public List<BookingAllocation> rank(List<DisruptionData.Booking> bookings, List<Transport> options) {
        return bookings.stream()
                .map(booking -> allocate(booking, options))
                .toList();
    }

    /**
     * Total passengers recommended each offer. Informational only: it can exceed the offer's seats
     * and does not influence the recommendations.
     */
    public Map<String, Integer> suggestedPassengers(List<BookingAllocation> allocations) {
        return allocations.stream()
                .filter(a -> a.transport().isPresent())
                .collect(Collectors.groupingBy(
                        a -> a.transport().get().getOfferId(),
                        Collectors.summingInt(a -> a.booking().getPassengers())));
    }

    private BookingAllocation allocate(DisruptionData.Booking booking, List<Transport> options) {
        String origin = booking.getJourney().getOrigin();
        String destination = booking.getJourney().getDestination();
        int passengers = booking.getPassengers();

        Optional<Transport> best = options.stream()
                .filter(t -> origin.equals(t.getOrigin()) && destination.equals(t.getDestination()))
                .filter(t -> t.getAvailableSeats() >= passengers)
                .min(RANKING);

        return new BookingAllocation(booking, best);
    }
}
