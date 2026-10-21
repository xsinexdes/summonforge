package com.customsmithing.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Huella de un item: material + los datos de meta que importan para decir
 * "este es el mismo item" (nombre propio, CustomModelData y etiqueta del plugin).
 * Lógica pura (sin Bukkit): {@link ItemRef} la rellena a partir de un ItemStack real.
 *
 * Regla de coincidencia (única en todo el plugin):
 *  - Si la referencia tiene etiqueta (p. ej. un mineral custom), basta con que el material
 *    y la etiqueta coincidan: renombrar el mineral en la config no invalida los items ya creados.
 *  - Si no tiene etiqueta, el item real tampoco puede tenerla, y el nombre y el CustomModelData
 *    deben ser idénticos. Así un "IRON_INGOT" normal NO acepta uno renombrado ni uno custom.
 */
public final class ItemSignature {

    private final String material;
    private final Integer modelData;
    private final String name;
    private final String tag;

    public ItemSignature(String material, Integer modelData, String name, String tag) {
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("falta el material");
        }
        this.material = material;
        this.modelData = modelData;
        this.name = blankToNull(name);
        this.tag = blankToNull(tag);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public String getMaterial() { return material; }
    public Integer getModelData() { return modelData; }
    public String getName() { return name; }
    public String getTag() { return tag; }

    /** ¿El item real descrito por {@code actual} cumple con esta referencia? */
    public boolean matches(ItemSignature actual) {
        if (actual == null || !material.equals(actual.material)) return false;
        if (tag != null) return tag.equals(actual.tag);
        if (actual.tag != null) return false;
        return Objects.equals(modelData, actual.modelData) && Objects.equals(name, actual.name);
    }

    /** Claves en español, listas para escribir en YAML. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", material);
        if (modelData != null) map.put("modelo", modelData);
        if (name != null) map.put("nombre", name);
        if (tag != null) map.put("etiqueta", tag);
        return map;
    }

    /** @throws IllegalArgumentException con un motivo legible si el mapa no sirve */
    public static ItemSignature fromMap(Map<?, ?> map) {
        Object mat = map.get("material");
        if (!(mat instanceof String matName) || matName.isBlank()) {
            throw new IllegalArgumentException("falta 'material'");
        }
        Integer model = null;
        Object rawModel = map.get("modelo");
        if (rawModel instanceof Number n) {
            model = n.intValue();
        } else if (rawModel instanceof String s && !s.isBlank()) {
            try {
                model = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'modelo' debe ser un número entero (leído: " + s + ")");
            }
        } else if (rawModel != null) {
            throw new IllegalArgumentException("'modelo' debe ser un número entero");
        }
        Object rawName = map.get("nombre");
        Object rawTag = map.get("etiqueta");
        return new ItemSignature(matName.trim().toUpperCase(java.util.Locale.ROOT), model,
                rawName == null ? null : rawName.toString(),
                rawTag == null ? null : rawTag.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemSignature other)) return false;
        return material.equals(other.material) && Objects.equals(modelData, other.modelData)
                && Objects.equals(name, other.name) && Objects.equals(tag, other.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, modelData, name, tag);
    }

    @Override
    public String toString() {
        return toMap().toString();
    }
}
