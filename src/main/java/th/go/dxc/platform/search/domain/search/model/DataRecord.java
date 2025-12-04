package th.go.dxc.platform.search.domain.search.model;

import java.time.Instant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public record DataRecord(
        JsonNode data,
        String datasetId, // provenance (optional but useful)
        String schema, // logical schema/version (optional)
        Instant sourceTimestamp // timestamp from source/QM (optional)
) {
    public JsonNode at(String jsonPointer) {
        return data.at(jsonPointer);
    }

    public DataRecord withData(JsonNode newData) {
        return new DataRecord(newData, datasetId, schema, sourceTimestamp);
    }

    /**
     * True if data is null, missing, or an empty object/array.
     */
    public boolean isEmpty() {
        return data == null
                || data.isMissingNode()
                || (data.isObject() && data.size() == 0)
                || (data.isArray() && data.size() == 0)
                || data.isNull();
    }
    public static DataRecord of(JsonNode data) {
        return new DataRecord(data, null, null, null);
    }
    public static DataRecord of(JsonNode data, String datasetId, String schema) {
        return new DataRecord(data, datasetId, schema, null);
    }   
    public static DataRecord of(JsonNode data, String datasetId, String schema, Instant sourceTimestamp) {
        return new DataRecord(data, datasetId, schema, sourceTimestamp);
    }   
    public static DataRecord empty(String datasetId, String schema) {
        return new DataRecord(JsonNodeFactory.instance.objectNode(), datasetId, schema, null);
    }

    public static DataRecord empty() {
        return new DataRecord(JsonNodeFactory.instance.objectNode(), null, null, null);
    }
}
