package com.eurowings.disruption.normalizer;

import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.impl.InternalFlightTransport;
import com.eurowings.disruption.raw.InternalFlightsRaw;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Flattens all internal searches into a single list of normalized Transport options.
 */
@Component
public class InternalFlightNormalizer {

    public List<Transport> normalize(InternalFlightsRaw raw) {
        if (raw == null || raw.getSearches() == null) {
            return List.of();
        }
        return raw.getSearches().stream()
                .map(InternalFlightsRaw.Search::getOffers)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .<Transport>map(InternalFlightTransport::new)
                .toList();
    }
}
