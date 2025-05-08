package org.example.org.main.control.model;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpyRep {
    private final Map<String, Integer> catData = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger("spy rep");
    private Coordinate coordinate;
    private       String  name;
    private final String  playerName;
    private       boolean   isMoon;
    private final String[]  relevantBuildingKeys = {"Metallmine","Kristallmine","Deuteriumsynthetisierer","Sensorphalanx",
            "Sprungtor",};
    private final String [] deffKeys             = {"Raketenwerfer","Leichtes Lasergeschütz","Schweres Lasergeschütz","Gausskanone",
            "Ionengeschütz","Plasmawerfer","Kleine Schildkuppel","Große Schildkuppel","Abfangrakete","Interplanetarrakete",};
    private final String [] fleetKeys            = {"Kleiner Transporter","Großer Transporter","Leichter Jäger","Schwerer Jäger",
            "Kreuzer","Schlachtschiff","Kolonieschiff","Recycler","Spionagesonde","Bomber","Zerstörer","Schlachter",
            "Todesstern","Solar Satellit",};
    private final String [] techKeys  = {"Waffentechnik","Schildtechnik","Raumschiffpanzerung","Verbrennungstriebwerk",
            "Impulstriebwerk","Hyperraumantrieb","Astrophysik","Produktionsmaximierung Metall","Produktionsmaximierung Kristall",
            "Produktionsmaximierung Deuterium",};


    public Map<String,Integer> getCatData () {
        return catData;
    }
    public SpyRep(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public boolean isMoon() {
        return isMoon;
    }

    public void setIsMoon(boolean moon) {
        isMoon = moon;
    }

    public void addField(String key, int value) {
        logger.debug("{} with the value of {} added to {} ",key,value,this.getName());
        String translatedKey = Localisation.translate(key);
        if (translatedKey == null) {
            logger.error("key {} was null",key);
            return;
        }
        catData.put(Localisation.translate(key), value);
    }

    public int getVal(String key) {
        String regex = "^\\d{3}$";
        if (key.matches(regex)) {
            key = Localisation.translate(key);
        }
        if (!catData.containsKey(key)) return 0;
        return catData.get(key);
    }

    public Map<String, Integer> getAllData() {
        return new HashMap<>(catData);
    }
    public List<MessageEmbed> getEmbeds () {
        List<MessageEmbed> msgEmbeds = new ArrayList<>();
        addHeaderEmbed(msgEmbeds);
        addBuildingsEmbed(msgEmbeds);
        addFleetEmbed(msgEmbeds);
        addDeffEmbed(msgEmbeds);
        addTechEmbed(msgEmbeds);
        return msgEmbeds;
    }
    private void addTechEmbed(List<MessageEmbed> embedList) {
        EmbedBuilder builder = new EmbedBuilder();
        String field ="Tech :microscope:";
        String value = "";
        int count = 0;
        for (String key : techKeys) {
            if (catData.containsKey(key)) {
                value += Localisation.getShortened(key) + " -> " + catData.get(key) + "\t";
                count++;
                if (count % 3 == 0) {
                    value += "\n";
                }
            }
        }

        if (value.trim().isEmpty()) {
            return;
        }
        builder.setColor(Color.BLUE);
        embedList.add(builder.addField(field,value,false).build());
    }
    private void addFleetEmbed(List<MessageEmbed> embedList) {
        EmbedBuilder builder = new EmbedBuilder();
        String field ="Flotte :rocket:";
        String value = "";
        int count = 0;
        for (String key : fleetKeys) {
            if (catData.containsKey(key)) {
                value += Localisation.getShortened(key) + " -> " + catData.get(key) + "\t";
                count++;
                if (count % 3 == 0) {
                    value += "\n";
                }
            }
        }
        if (value.trim().isEmpty()) {
            return;
        }
        builder.setColor(Color.RED);
        embedList.add(builder.addField(field,value,false).build());
    }
    private void addHeaderEmbed(List<MessageEmbed> embedList) {
        EmbedBuilder builder = new EmbedBuilder();
        String field = "Bericht für" + playerName + ":";
        String value = (isMoon) ? "Mond: " :"Planet: \n";
        value += name +" Koordinaten: "+coordinate;
        embedList.add(builder.addField(field,value, false).build());

    }
    private void addDeffEmbed(List<MessageEmbed> embedList) {
        EmbedBuilder builder = new EmbedBuilder();
        String field ="Deff :pinching_hand:";
        String value = "";
        int count = 0;
        for (String key : deffKeys) {
            if (catData.containsKey(key)) {
                value += Localisation.getShortened(key) + " -> " + catData.get(key) + "\t";
                count++;
                if (count % 3 == 0) {
                    value += "\n";
                }
            }
        }
        if (value.trim().isEmpty()) {
            return;
        }
        builder.setColor(Color.ORANGE);
        embedList.add(builder.addField(field,value,false).build());
    }
    private void addBuildingsEmbed(List<MessageEmbed> embedList) {
        EmbedBuilder builder = new EmbedBuilder();
        String field ="Gebäude :houses:";
        String value = "";

        for (String key : relevantBuildingKeys) {
            if (catData.containsKey(key)) {
                if ("Sensorphalanx".equals(key)) {
                    value += ":telescope:";
                }
                value += Localisation.getShortened(key) +" -> "+catData.get(key)+"\n";
            }
        }
        if (value.trim().isEmpty()) {
            return;
        }
        builder.setColor(Color.GREEN);
        embedList.add(builder.addField(field,value,false).build());
    }

    public EmbedBuilder getFormatted () {
        EmbedBuilder builder = new EmbedBuilder();
        String field = "Gebäude:";
        String value ="";
        builder.setTitle("Spionage Bericht von "+playerName+" "+coordinate.toString());
        for (String key : relevantBuildingKeys) {
            if (catData.containsKey(key)) {
                value = value + key +" = "+catData.get(key)+"\n";
            }
        }
        builder.setColor(Color.WHITE);
        builder.addField(field,value,false);
        field = "Forschung:";
        value = "";
        for (String key : techKeys) {
            if (catData.containsKey(key)) {
                value = value + key +" = "+catData.get(key)+"\n";
            }
        }
        builder.setColor(Color.magenta);
        builder.addField(field,value,false);
        field = "Schiffe:";
        value = "";
        for (String key : fleetKeys) {
            if (catData.containsKey(key)) {
                value = value + key +" = "+catData.get(key)+"\n";
            }
        }
        builder.setColor(Color.BLUE);
        builder.addField(field,value,false);
        field = "Verteidigung:";
        value = "";
        for (String key : deffKeys) {
            if (catData.containsKey(key)) {
                value = value + key +" = "+catData.get(key)+"\n";
            }
        }
        builder.setColor(Color.red);
        builder.addField(field,value,false);
        return builder;
    }

    @Override
    public String toString() {
        return "SpyRep{" +
               "name='" + name + '\'' +
               ", coordinate=" + coordinate +
               ", isMoon=" + isMoon +
               ", player='" + playerName + '\'' +
               ", dataCount" + catData.size() +
               '}';
    }
}