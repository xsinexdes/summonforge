package com.summonforge.item;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Objeto especial. Sesión 1: otorga una invocación al usar (clic derecho). */
public final class ItemDef implements Definition {
    public final String id;
    public String name;
    public Material material = Material.PAPER;
    /** Líneas de lore separadas por '|'. */
    public String lore = "";
    public int customModelData = 0;
    public String summonId = "";

    public ItemDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return material; }

    public static final Codec<ItemDef> CODEC = new Codec<>() {
        @Override
        public ItemDef read(String id, ConfigurationSection s) {
            ItemDef d = new ItemDef(id);
            d.name = s.getString("name", id);
            d.material = Enums.material(s.getString("material"), Material.PAPER);
            d.lore = s.getString("lore", "");
            d.customModelData = s.getInt("custom-model-data", 0);
            d.summonId = s.getString("summon", "");
            return d;
        }

        @Override
        public void write(ItemDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("material", d.material.name());
            s.set("lore", d.lore);
            s.set("custom-model-data", d.customModelData);
            s.set("summon", d.summonId);
        }
    };
}
