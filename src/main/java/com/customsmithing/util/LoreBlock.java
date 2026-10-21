package com.customsmithing.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Reemplaza SOLO el bloque de lore propio del plugin y conserva todo lo demás (firma del herrero,
 * texto de otros plugins...). El bloque se marca con códigos de color invisibles al inicio de su
 * primera línea y al final de la última. Lógica pura (sin Bukkit), probada con JUnit.
 */
public final class LoreBlock {

    public static final String START = "\u00a7r\u00a70\u00a7r";
    public static final String END = "\u00a7r\u00a71\u00a7r";

    private LoreBlock() {
    }

    /**
     * @param existing         lore actual del item (puede ser null)
     * @param block            líneas nuevas del bloque propio (sin marcas)
     * @param existingIsLegacy true si el lore actual es de un item antiguo, creado ANTES de las marcas:
     *                         entero es del plugin, así que se reemplaza completo
     */
    public static List<String> apply(List<String> existing, List<String> block, boolean existingIsLegacy) {
        List<String> wrapped = wrap(block);
        List<String> base = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
        if (existingIsLegacy) return wrapped;

        int start = indexOfStart(base);
        if (start >= 0) {
            int end = indexOfEnd(base, start);
            if (end >= 0) {
                List<String> result = new ArrayList<>(base.subList(0, start));
                result.addAll(wrapped);
                result.addAll(base.subList(end + 1, base.size()));
                return result;
            }
        }
        base.addAll(wrapped);
        return base;
    }

    public static boolean hasBlock(List<String> lore) {
        return lore != null && indexOfStart(lore) >= 0;
    }

    private static List<String> wrap(List<String> block) {
        List<String> wrapped = new ArrayList<>(block);
        if (wrapped.isEmpty()) wrapped.add("");
        wrapped.set(0, START + wrapped.get(0));
        int last = wrapped.size() - 1;
        wrapped.set(last, wrapped.get(last) + END);
        return wrapped;
    }

    private static int indexOfStart(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).startsWith(START)) return i;
        }
        return -1;
    }

    private static int indexOfEnd(List<String> lore, int from) {
        for (int i = from; i < lore.size(); i++) {
            if (lore.get(i).endsWith(END)) return i;
        }
        return -1;
    }
}
