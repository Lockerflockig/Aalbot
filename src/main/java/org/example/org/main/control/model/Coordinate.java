package org.example.org.main.control.model;

public record Coordinate(
        int galaxy,
        int system,
        int entryNr
) {

    public String toString() {
        return galaxy + ":" + system + ":" + entryNr;
    }

    public String getGalAndSysOnly () {
        return galaxy +":"+system;
    }

    public static Coordinate toCoordinate(String system,int entryNr) {
        String[] parts = system.split(":");
        return new Coordinate(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                entryNr
        );
    }
    public static Coordinate toCoordinate(String coordinate) {
        String[] parts = coordinate.split(":");
        if (parts.length>2) {
            return new Coordinate(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        } else if (parts.length > 1) {
            return new Coordinate(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    1
                    );
        } else if (parts.length > 0) {
            return new Coordinate(
                    Integer.parseInt(parts[0]),
                    1,1
            );
        }
        return null;
    }
}