package com.customsmithing.migration;

import com.customsmithing.util.IdValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migración de ids antiguos (mayúsculas, espacios, tildes...) a ids válidos. Es lógica pura sobre líneas
 * de texto, para conservar los comentarios y el orden del YAML tal cual. Probada con JUnit.
 */
public final class IdMigration {

    private IdMigration() {
    }

    /**
     * Plan de renombrado: solo incluye los ids que NO son válidos. Los ids válidos quedan reservados para
     * que un id normalizado nunca choque con uno existente (se añade _2, _3...).
     */
    public static Map<String, String> plan(Collection<String> ids) {
        Set<String> used = new HashSet<>();
        for (String id : ids) {
            if (IdValidator.isValid(id)) used.add(id);
        }
        Map<String, String> plan = new LinkedHashMap<>();
        for (String id : ids) {
            if (IdValidator.isValid(id)) continue;
            String base = IdValidator.normalize(id);
            if (base == null) base = "migrado";
            String candidate = base;
            int n = 2;
            while (used.contains(candidate)) {
                String suffix = "_" + n++;
                String trimmed = base.length() + suffix.length() > IdValidator.MAX_LENGTH
                        ? base.substring(0, IdValidator.MAX_LENGTH - suffix.length()) : base;
                candidate = trimmed + suffix;
            }
            used.add(candidate);
            plan.put(id, candidate);
        }
        return plan;
    }

    private static boolean isBlankOrComment(String line) {
        String t = line.trim();
        return t.isEmpty() || t.startsWith("#");
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static String unquote(String s) {
        String t = s.trim();
        if (t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    /** Renombra las claves hijas directas (indentación de 2 espacios) de la sección {@code rootKey}. */
    public static List<String> renameChildKeys(List<String> lines, String rootKey, Map<String, String> mapping) {
        List<String> out = new ArrayList<>(lines.size());
        boolean inRoot = false;
        Pattern child = Pattern.compile("^ {2}([^ #:][^:]*?):(.*)$");
        for (String line : lines) {
            if (!isBlankOrComment(line) && indentOf(line) == 0) {
                inRoot = line.trim().equals(rootKey + ":");
                out.add(line);
                continue;
            }
            if (inRoot && indentOf(line) == 2) {
                Matcher m = child.matcher(line);
                if (m.matches()) {
                    String key = unquote(m.group(1));
                    String replacement = mapping.get(key);
                    if (replacement != null) {
                        out.add("  " + replacement + ":" + m.group(2));
                        continue;
                    }
                }
            }
            out.add(line);
        }
        return out;
    }

    /** Reemplaza el valor de las líneas {@code <clave>: valor} cuando el valor está en el mapa. */
    public static List<String> replaceScalar(List<String> lines, String key, Map<String, String> mapping) {
        Pattern p = Pattern.compile("^(\\s*)" + Pattern.quote(key) + ":\\s*(\"[^\"]*\"|'[^']*'|[^#]*?)(\\s*#.*)?$");
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String replacement = mapping.get(unquote(m.group(2)));
                if (replacement != null) {
                    out.add(m.group(1) + key + ": " + replacement + (m.group(3) == null ? "" : m.group(3)));
                    continue;
                }
            }
            out.add(line);
        }
        return out;
    }

    /** Reemplaza los elementos ({@code - valor}) de la lista que sigue a la línea {@code <listKey>:}. */
    public static List<String> replaceListItems(List<String> lines, String listKey, Map<String, String> mapping) {
        Pattern item = Pattern.compile("^(\\s*-\\s*)(\"[^\"]*\"|'[^']*'|[^#]*?)(\\s*#.*)?$");
        List<String> out = new ArrayList<>(lines.size());
        boolean inList = false;
        for (String line : lines) {
            if (line.trim().equals(listKey + ":")) {
                inList = true;
                out.add(line);
                continue;
            }
            if (inList) {
                Matcher m = item.matcher(line);
                if (m.matches()) {
                    String replacement = mapping.get(unquote(m.group(2)));
                    out.add(replacement != null ? m.group(1) + replacement + (m.group(3) == null ? "" : m.group(3)) : line);
                    continue;
                }
                inList = false;
            }
            out.add(line);
        }
        return out;
    }
}
