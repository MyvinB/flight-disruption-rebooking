package com.eurowings.disruption.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Reads a JSON file from the classpath and maps it to the given raw DTO type.
 */
@Component
public class JsonResourceReader {

    private final ObjectMapper objectMapper;

    public JsonResourceReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T read(String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read classpath resource " + path, e);
        }
    }
}
