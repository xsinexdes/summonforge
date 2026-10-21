package com.customsmithing.util;

/**
 * Paleta "forja oscura": naranja brasa (principal), azul acero (acento), grises para texto
 * secundario. Todo color de menú o mensaje sale de aquí, no de códigos pegados en cada clase.
 * Lógica pura (sin Bukkit).
 */
public final class Theme {

    private static final String S = "\u00a7";

    public static final String PRIMARY = S + "6";   // naranja brasa
    public static final String ACCENT = S + "b";    // azul acero
    public static final String OK = S + "a";
    public static final String BAD = S + "c";
    public static final String WARN = S + "e";
    public static final String MAGIC = S + "d";
    public static final String MUTED = S + "7";
    public static final String DIM = S + "8";
    public static final String WHITE = S + "f";
    public static final String BOLD = S + "l";

    private Theme() {
    }

    public static String title(String text) { return PRIMARY + BOLD + text; }
    public static String accent(String text) { return ACCENT + text; }
    public static String ok(String text) { return OK + text; }
    public static String bad(String text) { return BAD + text; }
    public static String warn(String text) { return WARN + text; }
    public static String magic(String text) { return MAGIC + text; }
    public static String muted(String text) { return MUTED + text; }
    public static String dim(String text) { return DIM + text; }

    /** Línea de lore con etiqueta gris y valor blanco: "Nivel: 5". */
    public static String field(String label, String value) { return MUTED + label + ": " + WHITE + value; }
}
