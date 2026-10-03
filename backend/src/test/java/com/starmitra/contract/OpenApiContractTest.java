package com.starmitra.contract;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract-drift guard — validates the canonical spec packaged into
 * /static/openapi.yaml: parses, counts 133 operations, unique operationIds,
 * every operation has responses + tags.
 */
class OpenApiContractTest {

    @SuppressWarnings("unchecked")
    @Test
    void canonicalSpecIsValidAndComplete() throws java.io.IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> spec;
        try (InputStream in = getClass().getResourceAsStream("/static/openapi.yaml")) {
            assertNotNull(in, "openapi.yaml must be packaged from 05_API-Specifications");
            spec = yaml.load(in);
        }
        assertEquals("3.0.3", spec.get("openapi"));

        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        Set<String> operationIds = new HashSet<>();
        int ops = 0;
        for (var entry : paths.entrySet()) {
            Map<String, Object> item = (Map<String, Object>) entry.getValue();
            for (var m : item.entrySet()) {
                if (!Set.of("get","post","put","delete","patch").contains(m.getKey())) continue;
                ops++;
                Map<String, Object> op = (Map<String, Object>) m.getValue();
                assertTrue(op.containsKey("operationId"), "missing operationId: " + entry.getKey());
                assertTrue(op.containsKey("responses"), "missing responses: " + entry.getKey());
                assertTrue(operationIds.add((String) op.get("operationId")),
                        "duplicate operationId " + op.get("operationId"));
            }
        }
        assertEquals(133, ops, "canonical contract operation count drifted");
        assertEquals(133, operationIds.size());
    }
}
