package org.example.org.main.control.model;

import java.util.HashMap;
import java.util.Map;

public class DataUtil {
    private final static int             MAX_SYSTEMS = 400;

    /*
     * Calculates the range of phalanx takes into account that universe is a circle
     * Returns PhalanxRange data type
     * @param phalanxLevel Level of player phalanx
     * @param start Starting coordinate of player
     */
    public static PhalanxRange calcPhalanxRange(int phalanxLevel, int start) {
        int range = Math.max(1, phalanxLevel * phalanxLevel - 1);

        int lowerEnd = start - range;
        if (lowerEnd < 1) {
            lowerEnd = MAX_SYSTEMS + lowerEnd;
        }

        int upperEnd = start + range;
        if (upperEnd > MAX_SYSTEMS) {
            upperEnd = upperEnd - MAX_SYSTEMS;
        }

        return new PhalanxRange(lowerEnd, upperEnd);
    }

    public static PhalanxRange calcRange(int range, int start) {
        int lowerEnd = start - range;
        if (lowerEnd < 1) {
            lowerEnd = MAX_SYSTEMS + lowerEnd;
        }

        int upperEnd = start + range;
        if (upperEnd > MAX_SYSTEMS) {
            upperEnd = upperEnd - MAX_SYSTEMS;
        }

        return new PhalanxRange(lowerEnd, upperEnd);
    }

    public static boolean isInRange(int range, Coordinate playerCoordinate, Coordinate enemy) {
        if (playerCoordinate.galaxy() != enemy.galaxy()) return false;
        PhalanxRange calculatedRange = calcRange(range, playerCoordinate.system());
        int start = calculatedRange.start();
        int end = calculatedRange.end();

        if (start <= end) {
            return enemy.system() >= start && enemy.system() <= end;
        } else {
            return enemy.system() >= start || enemy.system() <= end;
        }
    }

    public static boolean isInRange(DBInterface.MoonData entry, Coordinate sys) {
        PhalanxRange range = calcPhalanxRange(entry.phalanxLvl(),entry.coordinate().system());
        //System.out.println("system->"+entry.coordinate()+"lvl->"+entry.phalanxLvl()+"->"+range);
        int start = range.start();
        int end = range.end();

        if (start <= end) {
            return sys.system() >= start && sys.system() <= end;
        } else {
            return sys.system() >= start || sys.system() <= end;
        }
    }

    public static String sanitizeColumnName(String columnName) {
        Map<Character, String> sanitizeMap = new HashMap<>();
        sanitizeMap.put('ä', "ae");
        sanitizeMap.put('ö', "oe");
        sanitizeMap.put('ü', "ue");
        sanitizeMap.put(' ', "_");
        sanitizeMap.put('ß', "ss");
        StringBuilder sanitizedString = new StringBuilder();
        for (char c : columnName.toCharArray()) {
            sanitizedString.append(sanitizeMap.getOrDefault(c, String.valueOf(c)));
        }
        return sanitizedString.toString();
    }
}