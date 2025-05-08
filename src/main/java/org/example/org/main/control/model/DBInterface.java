package org.example.org.main.control.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class DBInterface {

    private static final String BASE_URL = Pref.getDbUrl();
    private static DBInterface instance;

    private final ObjectMapper MAPPER;
    private final HttpClient client;
    private String authToken;

    public final Logger LOGGER = LoggerFactory.getLogger("Db interface");

    public final Map<Integer, simplePlayerData> playerMap = new HashMap<>();
    public final Map<String,Integer> playerToIdMap = new HashMap<>();
    public final Map<Integer, String> alliMap = new HashMap<>();
    public final List<MoonData> phalanxList = new ArrayList<>();

    // Singleton
    public static DBInterface getInstance() {
        if (instance == null) instance = new DBInterface();
        return instance;
    }

    // Konstruktor – Initialisiert und lädt Daten
    private DBInterface() {
        MAPPER = new ObjectMapper();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        login();
        loadPlayers();
        loadAlliances();
        loadActivePhalanx();
        LOGGER.info("all loaded");

    }

    // Login zur Pocketbase-API
    private void login() {
        try {
            String jsonBody = String.format("{\"identity\":\"%s\",\"password\":\"%s\"}", Pref.getDbId(), Pref.getDbPw());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/collections/users/auth-with-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = MAPPER.readTree(response.body());
                authToken = rootNode.path("token").asText();
                LOGGER.info("login into db successful");
            } else {
                LOGGER.info("failed to login to db");
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("failed to authenticate", e);
            throw new RuntimeException(e);
        }
    }

    // Spieler-Datensatz
    public record simplePlayerData(String name, int alliId) {}

    // Lade alle Spieler
    private void loadPlayers() {
        JsonNode rootNode = makeFullTableRequest("/api/collections/players/records", "");
        JsonNode items = rootNode.path("items");

        if (!items.isArray()) {
            LOGGER.error("failed to load players");
            return;
        }

        LOGGER.debug("request size {}", items.size());
        for (JsonNode item : items) {
            int id = item.get("player_id").asInt();
            int alliId = item.get("alli_id").asInt();
            String name = item.get("player_name").asText();
            playerMap.put(id, new simplePlayerData(name, alliId));
            playerToIdMap.put(name,id);
        }
    }

    // Lade alle Allianzen
    private void loadAlliances() {
        JsonNode rootNode = makeFullTableRequest("/api/collections/alliances/records", "");
        JsonNode items = rootNode.path("items");

        if (!items.isArray()) {
            LOGGER.error("failed to load alliances");
            return;
        }

        for (JsonNode item : items) {
            String name = item.get("alli_name").asText();
            int id = item.get("alli_id").asInt();
            alliMap.put(id, name);
        }
    }
    /**
     * Parse spy data from a JsonNode into a SpyRep object
     * @param spyRep The SpyRep to populate
     * @param node The JSON node containing data
     * @return The populated SpyRep
     */
    private SpyRep parseSpyData(SpyRep spyRep, JsonNode node) {

        String[] categories = {"cat0","cat100","cat200","cat400"};

        for (String cat : categories ) {
            JsonNode category = node.path(cat);
            try {
                LOGGER.debug("cat node = {}",MAPPER.writeValueAsString(category));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            if (!category.isMissingNode() && category.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = category.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    try {
                        LOGGER.debug("cat single node = {}", MAPPER.writeValueAsString(entry));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    if (entry.getValue().asInt() != 0) {
                        spyRep.addField(entry.getKey(), entry.getValue().asInt());
                    }
                }
            }
        }
        return spyRep;
    }

    public static void main(String[] args) {
        DBInterface d = DBInterface.getInstance();
        List<SpyRep> reps = d.getSpyreports(d.playerMap.get(11463).name);
        d.LOGGER.debug("size of processed reps = {} ",reps.size());
        for (int i = 0; i < reps.size(); i++) {
            d.LOGGER.debug("entry {} = {}",i,reps.get(i).toString());
            Iterator<Map.Entry<String,Integer>> entryIterator = reps.get(i).getCatData().entrySet().iterator();
            while (entryIterator.hasNext()) {
                Map.Entry<String,Integer> entry = entryIterator.next();
                d.LOGGER.debug("SpyRep nr {} contains key {} with value {}",i,entry.getKey(),entry.getValue());
            }
        }

    }

    /**
     * Get all spy reports for a player
     * @param playerName Name of the player
     * @return List of SpyRep objects containing spy data
     */
    public List<SpyRep> getSpyreports(String playerName) {
        LOGGER.debug("fetching rep for {}",playerName);
        if (!playerToIdMap.containsKey(playerName)) {
            LOGGER.error("player {} not found", playerName);
            return null; // Return empty list instead of null
        }

        String playerId = playerToIdMap.get(playerName).toString();
        String filter = "player_id=" + playerId + " && (planet_buildings!=\"\" || moon_buildings!=\"\")";
        String expand = "planet_buildings,moon_buildings";

        JsonNode rootNode = makeRequestWithExpand("galaxy_state", filter, expand);
        JsonNode items = rootNode.path("items");

        try {
            LOGGER.debug(MAPPER.writeValueAsString(rootNode));
            LOGGER.debug("got {} items from db",items.size());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<SpyRep> results = new ArrayList<>();

        for (JsonNode item : items) {
            String planetName = item.path("planet_name").asText("");
            Coordinate coordinate = extractCoordinate(item);

            // Process planet data if available
            if (item.path("expand").has("planet_buildings")) {
                JsonNode planetBuildingsNode = item.path("expand").path("planet_buildings");
                if (!planetBuildingsNode.isMissingNode()) {
                    SpyRep spyRep = new SpyRep(playerName);
                    spyRep.setName(planetName);
                    spyRep.setCoordinate(coordinate);
                    spyRep.setIsMoon(false);

                    parseSpyData(spyRep, planetBuildingsNode);
                    results.add(spyRep);
                }
            }

            // Process moon data if available
            if (item.path("expand").has("moon_buildings")) {
                JsonNode moonBuildingsNode = item.path("expand").path("moon_buildings");
                if (!moonBuildingsNode.isMissingNode()) {
                    SpyRep spyRep = new SpyRep(playerName);
                    spyRep.setName(planetName + " (Moon)");
                    spyRep.setCoordinate(coordinate);
                    spyRep.setIsMoon(true);

                    parseSpyData(spyRep, moonBuildingsNode);
                    results.add(spyRep);
                }
            }
        }

        return results;
    }

    private void parseCat (JsonNode spyNode, SpyRep rep) {

    }

    /**
     * Extract coordinate from a JsonNode
     * @param item The JSON node
     * @return Coordinate object
     */
    private Coordinate extractCoordinate(JsonNode item) {
        int galaxy = item.path("pos_galaxy").asInt(0);
        int system = item.path("pos_system").asInt(0);
        int position = item.path("pos_planet").asInt(0);
        return new Coordinate(galaxy, system, position);
    }


    // Mond-Datenstruktur
    public record MoonData(
            Coordinate coordinate,
            String name,
            String alliance,
            int phalanxLvl
    ) {}

    // Lade alle aktiven Phalanx-Scanner
    public void loadActivePhalanx() {
        JsonNode rootNode = makeRequestWithExpand("galaxy_state", "has_moon=true", "moon_buildings");
        JsonNode items = rootNode.path("items");

        if (!items.isArray()) {
            LOGGER.error("Failed to get valid moon data");
            return;
        }

        LOGGER.info("{} moons with phalanx found", items.size());

        for (JsonNode item : items) {
            JsonNode report = item.path("expand").path("moon_buildings");
            int phalanxLvl = 0;

            if (report.has("cat0") && report.path("cat0").has("42")) {
                phalanxLvl = report.path("cat0").path("42").asInt();
            }

            if (phalanxLvl <= 1) continue;

            Coordinate coordinate = new Coordinate(
                    item.path("pos_galaxy").asInt(),
                    item.path("pos_system").asInt(),
                    item.path("pos_planet").asInt()
            );

            int playerId = item.path("player_id").asInt();
            simplePlayerData player = playerMap.get(playerId);

            phalanxList.add(new MoonData(
                    coordinate,
                    player.name,
                    alliMap.get(player.alliId),
                    phalanxLvl
            ));
        }
    }

    // Anfrage mit expand und optionalem Filter
    private JsonNode makeRequestWithExpand(String collection, String filter, String expand) {
        try {
            StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                    .append("/api/collections/")
                    .append(URLEncoder.encode(collection, StandardCharsets.UTF_8))
                    .append("/records?perPage=500");

            if (expand != null && !expand.isBlank()) {
                urlBuilder.append("&expand=").append(URLEncoder.encode(expand, StandardCharsets.UTF_8));
            }

            if (filter != null && !filter.isBlank()) {
                urlBuilder.append("&filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return MAPPER.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Fehler bei der API-Anfrage mit Expand", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("API-Anfrage fehlgeschlagen", e);
        }
    }

    // Lädt eine ganze Tabelle, auch bei Pagination
    private JsonNode makeFullTableRequest(String endpoint, String filter) {
        try {
            JsonNode firstPage = makeRequest(endpoint, filter, 500);
            int totalItems = firstPage.path("totalItems").asInt();
            int totalPages = firstPage.path("totalPages").asInt();

            if (totalPages <= 1) return firstPage;

            com.fasterxml.jackson.databind.node.ArrayNode allItems = MAPPER.createArrayNode();
            for (JsonNode item : firstPage.path("items")) {
                allItems.add(item);
            }

            for (int page = 2; page <= totalPages; page++) {
                try {
                    JsonNode pageData = makeRequest(endpoint, filter, 500);
                    for (JsonNode item : pageData.path("items")) {
                        allItems.add(item);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading page {}", page, e);
                }
            }

            com.fasterxml.jackson.databind.node.ObjectNode result = MAPPER.createObjectNode();
            result.put("totalItems", totalItems);
            result.put("totalPages", totalPages);
            result.set("items", allItems);
            return result;

        } catch (Exception e) {
            LOGGER.error("Error loading complete table {}", endpoint, e);
            throw new RuntimeException("Failed to load all data", e);
        }
    }

    // Einfache GET-Anfrage mit optionalem Filter
    private JsonNode makeRequest(String endpoint, String filter, int perPage) {
        try {
            StringBuilder urlBuilder = new StringBuilder(BASE_URL + endpoint)
                    .append("?perPage=").append(perPage);

            if (filter != null && !filter.isEmpty()) {
                urlBuilder.append("&filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return MAPPER.readTree(response.body());

        } catch (IOException | InterruptedException e) {
            LOGGER.error("ERROR on API-request");
            throw new RuntimeException("API-request failed -> ", e);
        }
    }
}
