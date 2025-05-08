package org.example.org.main.control.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class Pref {
    private static final ObjectMapper MAPPER;
    private static final File       ENV_FILE = new File("src/main/resources/.env");
    private static final ObjectNode ROOT_NODE;
    private static final Logger     LOGGER = LoggerFactory.getLogger("main");

    static {
        MAPPER = new ObjectMapper();
        try {
            ROOT_NODE = (ObjectNode) MAPPER.readTree(ENV_FILE);
        } catch (IOException e) {
            LOGGER.error("res/.env not found or invalid", e);
            throw new RuntimeException(e);
        }
    }

    // Getter für Token
    public static String getDcToken() {
        return ROOT_NODE.path("DISCORD_BOT_TOKEN").asText();
    }

    public static String getDbId() {
        return ROOT_NODE.path("DB_ID").asText();
    }

    public static String getDbPw() {
        return ROOT_NODE.path("DB_PW").asText();
    }

    public static String getDbUrl() {
        return ROOT_NODE.path("DB_URL").asText();
    }
    public static String getValue(String key) {
        return ROOT_NODE.path(key).asText();
    }

    // Getter für highlighting: { "alli": "emote", ... }
    public static Map<String, String> getHighlighting() {
        Map<String, String> result = new HashMap<>();
        JsonNode highlightingNode = ROOT_NODE.path("highlighting");
        if (highlightingNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = highlightingNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                result.put(entry.getKey(), entry.getValue().asText());
            }
        }
        return result;
    }

    // Setze einzelnes Highlighting-Paar
    public static void setHighlighting(String key, String emote) {
        JsonNode highlightingNode = ROOT_NODE.path("highlighting");
        if (!highlightingNode.isObject()) {
            (ROOT_NODE).set("highlighting", MAPPER.createObjectNode());
        }
        ((ObjectNode) ROOT_NODE.path("highlighting")).put(key, emote);
        saveEnvFile();
    }

    // Entferne ein Highlighting-Item anhand des Keys
    public static void removeHighlighting(String key) {
        JsonNode highlightingNode = ROOT_NODE.path("highlighting");
        if (highlightingNode.isObject() && highlightingNode.has(key)) {
            ((ObjectNode) highlightingNode).remove(key);
            saveEnvFile();
            LOGGER.info("Highlighting '{}' removed", key);
        } else {
            LOGGER.warn("Key '{}' not found in highlighting", key);
        }
    }

    // Allgemeiner Setter für String-Werte
    public static void setValue(String key, String value) {
        ROOT_NODE.put(key, value);
        saveEnvFile();
    }

    // Speichere .env-Datei
    private static void saveEnvFile() {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(ENV_FILE, ROOT_NODE);
            LOGGER.info(".env updated");
        } catch (IOException e) {
            LOGGER.error("Failed to write .env", e);
        }
    }
}
