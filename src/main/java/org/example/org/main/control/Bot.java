package org.example.org.main.control;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.org.main.control.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot extends ListenerAdapter {
    private static Bot instance;
    private static final Logger LOGGER = LoggerFactory.getLogger("Bot");
    private final DBInterface dbInterface = DBInterface.getInstance();
    private JDA jda;
    private final Map<String, Float> conversionMap = new HashMap<>();
    private final Map<String, String> holdValsFrom = new HashMap<>();
    private final Map<String, String> holdValsTo = new HashMap<>();
    private final Map<String, List<SpyRep>> spyMap = new HashMap<>();

    public static void main(String[] args) {
        Bot bot = Bot.getInstance();
    }

    // Bot-Setup
    private Bot() {
        conversionMap.put("m", Float.parseFloat(Pref.getValue("met")));
        conversionMap.put("k", Float.parseFloat(Pref.getValue("krist")));
        conversionMap.put("d", Float.parseFloat(Pref.getValue("deut")));
        try {
            LOGGER.info("starting Bot...");
            jda = JDABuilder.createDefault(Pref.getDcToken())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(this)
                    .build();

            OptionData spyOption = new OptionData(OptionType.STRING, "name", "name des Spielers", true);
            OptionData optionData = new OptionData(OptionType.CHANNEL, "channel", "id", false);
            OptionData resOption = new OptionData(OptionType.INTEGER, "res", "ausgangs res", true);
            OptionData operationOption = new OptionData(OptionType.STRING, "operation", "z.B. k-m", true);
            OptionData phalanxOption = new OptionData(
                    OptionType.STRING, "system", "zu überprüfendes system", true);

            jda.updateCommands()
                    .addCommands(
                            Commands.slash("ping", "pongs").addOptions(optionData),
                            Commands.slash("phalanx", "überprüft ob das System in Reichweite eines " +
                                                      "Phalanx-Scanners ist")
                                    .addOptions(phalanxOption),
                            Commands.slash("res", "Beispiel von met zu krist = 10000, m-k")
                                    .addOptions(resOption, operationOption),
                            Commands.slash("spy", "wähle den spio bericht eines spielers")
                                    .addOptions(spyOption)
                    ).queue();

            jda.awaitReady();
            LOGGER.info("Bot started and ready!");
        } catch (InterruptedException e) {
            LOGGER.error("failed to start Bot");
            System.exit(1);
        }
    }

    public static Bot getInstance() {
        if (instance == null) instance = new Bot();
        return instance;
    }

    // Befehl wird verarbeitet
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        switch (command) {
            case "ping" -> handlePing(event);
            case "phalanx" -> handlePhalanx(event);
            case "res" -> handleRes(event);
            case "spy" -> handleZahl(event); // Zeigt Spionageberichte
        }
    }

    // Antwort für /ping
    private void handlePing(SlashCommandInteractionEvent event) {
        event.reply("pong").queue();
    }

    // Antwort für /phalanx
    private void handlePhalanx(SlashCommandInteractionEvent event) {
        //event.deferReply().queue();
        Coordinate coordinate;
        EmbedBuilder embed = new EmbedBuilder();
        String field = "Phalanx sensoren in Reichweite:\n\n";
        String coordStr = event.getOption("system").getAsString();
        try {
            coordinate = Coordinate.toCoordinate(coordStr);
            if (coordinate.system() > 400) throw new Exception();
        } catch (Exception e) {
            event.reply(coordStr + " ist kein gültiges Format für system: x:xxx").queue();
            return;
        }

        for (DBInterface.MoonData moon : dbInterface.phalanxList) {
            if (!DataUtil.isInRange(moon, coordinate)) continue;
            String alli = (moon.alliance() != null) ? moon.alliance() : "";
            field = field + moon.name() + "-" + alli;
            String value = moon.coordinate() + " -> Phalanx Stufe " + moon.phalanxLvl();
            embed.setColor(Color.BLUE);
            embed.addField(field, value, false);
            field = "";
        }

        if (!embed.isEmpty()) {
            event.replyEmbeds(embed.build()).queue();
        } else {
            event.reply("keine Phalanx in Reichweite").queue();
        }
    }

    private void handleZahl(SlashCommandInteractionEvent event) {
        String playerName = event.getOption("name").getAsString();
        List<SpyRep> reps = dbInterface.getSpyreports(playerName);
        if (reps == null) {
            event.reply("kein Spieler mit dem Namen " + playerName + "gefunden").queue();
            return;
        }

        spyMap.put(playerName, reps);
        List<SelectOption> options = new ArrayList<>();
        StringSelectMenu.Builder menubuilder = StringSelectMenu.create("planetChooser")
                .setPlaceholder("Wähle einen Planeten");

        for (int i = 0; i < reps.size(); i++) {
            SpyRep rep = reps.get(i);
            String koordinaten = rep.getCoordinate().toString();
            String moon = (rep.isMoon()) ? " - Mond" : "";
            SelectOption option = SelectOption.of(koordinaten + moon, i + "-" + playerName);
            options.add(option);
        }

        menubuilder.addOptions(options);
        StringSelectMenu dropMenu = menubuilder.build();
        event.reply("Bitte wähle einen Planeten:")
                .addActionRow(dropMenu)
                .queue();
    }

    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String command = event.getComponentId();
        if (!"planetChooser".equals(command)) return;

        var option = event.getSelectedOptions().getFirst().getValue();
        String name = option.substring(option.indexOf("-") + 1);
        int pos = Integer.parseInt(option.substring(0, option.indexOf("-")));
        SpyRep rep = spyMap.get(name).get(pos);
        event.editMessage("Spionagebericht")
                .setEmbeds(rep.getEmbeds())
                .setComponents() // Entfernt alle Komponenten
                .queue();
        //event.replyEmbeds(rep.getFormatted().build()).queue();
    }

    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("modal")) {
            String numberStr = event.getValue("number input").getAsString();
            try {
                int number = Integer.parseInt(numberStr);
                convert(number, event);
            } catch (NumberFormatException e) {
                event.reply("Fehler: Bitte gib eine gültige Zahl ein").queue();
            }
        }
    }

    private void convert(int val, ModalInteractionEvent event) {
        // Ressourcenumrechnung
        float sourceRatio = conversionMap.get(holdValsFrom.get(event.getUser().getName()));
        float targetRatio = conversionMap.get(holdValsTo.get(event.getUser().getName()));
        float baseVal = val / sourceRatio;
        float result = baseVal * targetRatio;
        event.reply(val + " = " + result).queue();
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        String emoji = event.getReaction().getEmoji().getName();
        event.getChannel().sendMessage("69 ist größer als " + emoji).queue();
    }

    // rechnet materialien nach Marktkurs aus
    private void handleRes(SlashCommandInteractionEvent event) {
        // Sicherstellen, dass die Option "operation" existiert
        if (event.getOption("operation") == null) {
            event.reply("Keine Operation angegeben").queue();
            return;
        }

        String operation = event.getOption("operation").getAsString();
        // Überprüfen, ob das Trennzeichen vorhanden ist
        if (!operation.contains("-")) {
            event.reply("Fehlerhafte Eingabe: Operation muss im Format 'von-nach' sein").queue();
            return;
        }

        String[] operationParts = operation.split("-");
        // Überprüfen, ob genau zwei Teile vorhanden sind
        if (operationParts.length != 2) {
            event.reply("Fehlerhafte Eingabe: Operation muss genau ein '-' enthalten").queue();
            return;
        }

        // Überprüfen, ob beide Teile in der Konversionstabelle vorhanden sind
        if (!conversionMap.containsKey(operationParts[0]) || !conversionMap.containsKey(operationParts[1])) {
            event.reply("Fehlerhafte Eingabe für Umrechnung: " + operation +
                        " (Werte " + operationParts[0] + " oder " + operationParts[1] + " nicht bekannt)").queue();
            return;
        }

        // Sicherstellen, dass die Option "res" existiert
        if (event.getOption("res") == null) {
            event.reply("Kein Wert für die Umrechnung angegeben").queue();
            return;
        }

        float sourceRatio = conversionMap.get(operationParts[0]);
        float targetRatio = conversionMap.get(operationParts[1]);
        int inputValue = event.getOption("res").getAsInt();

        // Die korrekte Formel für die Umrechnung:
        // Zuerst Input in die Basiseinheit (z.B. d für "Diamanten") umwandeln
        // Dann von der Basiseinheit in die Zieleinheit umwandeln
        float baseValue = inputValue / sourceRatio;
        float result = baseValue * targetRatio;

        // Formatierung für bessere Lesbarkeit
        String formattedResult = String.format("%.2f", result);
        event.reply(inputValue + " " + operationParts[0] + " = " + formattedResult + " " + operationParts[1]).queue();
    }
}