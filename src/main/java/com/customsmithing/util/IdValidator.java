package com.customsmithing.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Única puerta para validar ids que se guardan en YAML o se usan como nombre
 * de archivo (recetas, aleaciones, minerales, jefes, misiones, estructuras).
 * Formato permitido: [a-z0-9_]{1,32}.
 *
 * Es lógica pura (sin Bukkit) para poder probarla con JUnit.
 */
public final class IdValidator {

    public static final int MAX_LENGTH = 32;

    private static final Pattern VALID = Pattern.compile("[a-z0-9_]{1," + MAX_LENGTH + "}");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-/\\\\]+");
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9_]");
    private static final Pattern MULTI_UNDERSCORE = Pattern.compile("_{2,}");

    private IdValidator() {
    }

    public static boolean isValid(String id) {
        return id != null && VALID.matcher(id).matches();
    }

    /** Motivo legible por el que el id no es válido, o null si lo es. */
    public static String problem(String id) {
        if (id == null || id.isEmpty()) return "el id está vacío";
        if (id.length() > MAX_LENGTH) return "tiene más de " + MAX_LENGTH + " caracteres";
        if (!VALID.matcher(id).matches()) {
            return "solo se permiten letras minúsculas sin tildes, números y guion bajo (_)";
        }
        return null;
    }

    /**
     * Intenta convertir un texto cualquiera en un id válido (quita tildes, pasa a
     * minúsculas, cambia espacios/puntos/guiones/barras por "_" y descarta el resto).
     *
     * @return el id normalizado, o null si no queda nada utilizable
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD);
        s = DIACRITICS.matcher(s).replaceAll("");
        s = s.toLowerCase(Locale.ROOT);
        s = SEPARATORS.matcher(s).replaceAll("_");
        s = INVALID_CHARS.matcher(s).replaceAll("");
        s = MULTI_UNDERSCORE.matcher(s).replaceAll("_");
        s = trimUnderscores(s);
        if (s.length() > MAX_LENGTH) s = trimUnderscores(s.substring(0, MAX_LENGTH));
        return s.isEmpty() ? null : s;
    }

    private static String trimUnderscores(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '_') start++;
        while (end > start && s.charAt(end - 1) == '_') end--;
        return s.substring(start, end);
    }
}
