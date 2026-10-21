package com.eurowings.disruption.normalizer;

import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.impl.ExternalFlightTransport;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps every external airline result to a normalized Transport option.
 */
@Component
public class ExternalFlightNormalizer {

    public List<Transport> normalize(ExternalFlightsRaw raw) {
        if (raw == null || raw.getResults() == null) {
            return List.of();
        }
        return raw.getResults().stream()
                .<Transport>map(ExternalFlightTransport::new)
                .toList();
    }
}
