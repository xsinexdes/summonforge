package com.customsmithing.data;

import org.bukkit.Material;

/** Herramientas de herrería: las tenazas hacen falta para calentar y el martillo para forjar (si herramientas.activar). */
public enum ToolType {
    TENAZAS("Tenazas de herrero", Material.SHEARS, "el Calentador"),
    MARTILLO("Martillo de herrero", Material.IRON_AXE, "el Yunque");

    private final String label;
    private final Material icon;
    private final String usedAt;

    ToolType(String label, Material icon, String usedAt) {
        this.label = label;
        this.icon = icon;
        this.usedAt = usedAt;
    }

    public String getLabel() { return label; }
    public Material getIcon() { return icon; }
    public String getUsedAt() { return usedAt; }

    public static ToolType parse(String name) {
        if (name == null) return null;
        for (ToolType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
