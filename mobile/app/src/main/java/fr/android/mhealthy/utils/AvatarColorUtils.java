package fr.android.mhealthy.utils;

import android.graphics.Color;

public class AvatarColorUtils {

    private static final int BRIGHTNESS_THRESHOLD = 200; // Adjust this value as needed

    public static int generateColorFromString(String input) {
        // Generate a hash code from the input string
        int hash = input.hashCode();

        // Use the hash code to create RGB values
        int r = (hash & 0xFF0000) >> 16; // Red
        int g = (hash & 0x00FF00) >> 8;  // Green
        int b = (hash & 0x0000FF);       // Blue

        // Ensure the values are in the range of 0-255
        r = Math.abs(r) % 256;
        g = Math.abs(g) % 256;
        b = Math.abs(b) % 256;

        // Calculate brightness
        int brightness = (r + g + b) / 3;

        // If the brightness is above the threshold, darken the color
        if (brightness > BRIGHTNESS_THRESHOLD) {
            r = (int) (r * 0.5);
            g = (int) (g * 0.5);
            b = (int) (b * 0.5);
        }

        // Return the color
        return Color.rgb(r, g, b);
    }
}
