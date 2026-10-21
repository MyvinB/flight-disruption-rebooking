package com.eurowings.disruption.normalizer;

import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.impl.TrainTransport;
import com.eurowings.disruption.raw.TrainsRaw;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Maps train connections to normalized Transport options.
 * Connections carry no airport codes themselves, so they are taken from the enclosing query.
 */
@Component
public class TrainNormalizer {

    public List<Transport> normalize(TrainsRaw raw) {
        if (raw == null || raw.getQueries() == null) {
            return List.of();
        }
        return raw.getQueries().stream()
                .filter(q -> Objects.nonNull(q.getConnections()))
                .flatMap(q -> q.getConnections().stream()
                        .<Transport>map(c -> new TrainTransport(c, q.getFromAirport(), q.getToAirport())))
                .toList();
    }
}
