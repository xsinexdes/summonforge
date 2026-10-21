package com.customsmithing.util;

import java.util.Locale;

/** Utilidades de texto compartidas (antes copiadas en cada clase). Lógica pura. */
public final class Text {

    private Text() {
    }

    /** "IRON_INGOT" -> "Iron Ingot". Tolera nombres vacíos o con guiones bajos repetidos. */
    public static String prettify(String enumName) {
        if (enumName == null || enumName.isBlank()) return "?";
        StringBuilder sb = new StringBuilder();
        for (String part : enumName.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    /** 3725 -> "1 h 2 min"; 45 -> "45 s"; 0 o negativo -> "0 s". */
    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "0 s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return m > 0 ? h + " h " + m + " min" : h + " h";
        if (m > 0) return s > 0 && m < 10 ? m + " min " + s + " s" : m + " min";
        return s + " s";
    }

    /** Redondea a 1 decimal. */
    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Formatea con 1 decimal y punto decimal, sin depender del idioma del servidor. */
    public static String fmt1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
