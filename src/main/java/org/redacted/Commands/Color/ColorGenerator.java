package org.redacted.Commands.Color;

import java.awt.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ColorGenerator {

    private static final Set<String> DISALLOWED_COLORS = new HashSet<>();

    static {
        // Add disallowed colors to the set
        DISALLOWED_COLORS.add("#5944b9");
        DISALLOWED_COLORS.add("#9c27b0");
        DISALLOWED_COLORS.add("#673ab7");
        DISALLOWED_COLORS.add("#cb0f02");
        DISALLOWED_COLORS.add("#f44336");
        DISALLOWED_COLORS.add("#3f51b5");
    }

    public static Color getRandomColor() {
        Random random = new Random();
        String color;
        do {
            color = String.format("#%06x", random.nextInt(0xFFFFFF + 1));
        } while (DISALLOWED_COLORS.contains(color));
        return Color.decode(color);
    }
}
