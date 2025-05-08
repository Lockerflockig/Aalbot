package org.example.org.main.control.model;

public record PhalanxRange(
        int start,
        int end
) {
    public static PhalanxRange dummy () {
        return new PhalanxRange(0,0);
    }
}
