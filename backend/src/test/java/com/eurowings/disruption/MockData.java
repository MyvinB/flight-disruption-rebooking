package com.eurowings.disruption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Test access to the mock data in src/main/resources/data.
 * Negative scenarios start from a real record and change only the field under test.
 */
public final class MockData {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockData() {
    }

    public static <T> T read(String file, Class<T> type) {
        return MAPPER.convertValue(tree(file), type);
    }

    public static JsonNode tree(String file) {
        try (InputStream in = MockData.class.getResourceAsStream("/data/" + file)) {
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copy of the record in the given array whose idField equals id, mapped to type after applying the change.
     */
    public static <T> T record(String file, String arrayPath, String idField, String id,
                               Class<T> type, Consumer<ObjectNode> change) {
        ObjectNode copy = find(tree(file).at(arrayPath), idField, id).deepCopy();
        change.accept(copy);
        return MAPPER.convertValue(copy, type);
    }

    public static <T> T record(String file, String arrayPath, String idField, String id, Class<T> type) {
        return record(file, arrayPath, idField, id, type, node -> { });
    }

    private static ObjectNode find(JsonNode array, String idField, String id) {
        for (JsonNode node : (ArrayNode) array) {
            if (id.equals(node.path(idField).asText())) {
                return (ObjectNode) node;
            }
        }
        throw new IllegalArgumentException("No record with " + idField + "=" + id);
    }
}
