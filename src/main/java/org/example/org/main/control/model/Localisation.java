package org.example.org.main.control.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Localisation {
    private static       Map<String,String > localistationMap = new HashMap<>();
    private static final Logger              logger           = LoggerFactory.getLogger("localisation");
    private static Map<String ,String> shortenMap = new HashMap<>();
    static {
        localistationMap = jsonToMap(new File("src/main/resources/localisation.json"));
        shortenMap = jsonToMap(new File("src/main/resources/abkürzungsMap.json"));

    }
    private static Map<String ,String > jsonToMap( File location) {
        Map<String,String> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNote = mapper.readTree(location);
            Iterator<Map.Entry<String,JsonNode>> fields = rootNote.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                map.put(entry.getKey(), entry.getValue().asText());
            }
        } catch (IOException e) {
            logger.error("failed to load map");
            throw new RuntimeException(e);
        }
        return map;
    }
    public static String getShortened (String val) {
        if (!shortenMap.containsKey(val)) return val;
        return shortenMap.get(val);
    }
    public static String translate (String key) {
        return localistationMap.get(key);
    }
}
