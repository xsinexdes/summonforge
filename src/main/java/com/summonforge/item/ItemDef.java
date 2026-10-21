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
    public ItemKind kind = ItemKind.SUMMON;
    public String summonId = "";
    /** Solo POWER: duración del buff, fatiga posterior y enfriamiento (segundos). */
    public int buffSeconds = 30;
    public int fatigueSeconds = 20;
    public int cooldownSeconds = 60;
    /** Sobreuso: a los N usos seguidos (la fatiga baja 1 por minuto) aplica ceguera/náuseas y pierde corazones. */
    /** RUNE: estadística y % que potencia. */
    public RuneStat runeStat = RuneStat.DAMAGE;
    public double runeValue = 10;
    /** AWAKEN: requisitos (kills de la invocación y sincronización mínima %). */
    public int requireKills = 0;
    public int requireSync = 0;
    public int overuseAt = 3;
    public int heartLoss = 0;
    public int heartLossMinutes = 30;

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
            d.kind = Enums.parse(ItemKind.class, s.getString("kind"), ItemKind.SUMMON);
            d.summonId = s.getString("summon", "");
            d.buffSeconds = s.getInt("buff-seconds", 30);
            d.fatigueSeconds = s.getInt("fatigue-seconds", 20);
            d.cooldownSeconds = s.getInt("cooldown-seconds", 60);
            d.runeStat = Enums.parse(RuneStat.class, s.getString("rune-stat"), RuneStat.DAMAGE);
            d.runeValue = s.getDouble("rune-value", 10);
            d.requireKills = s.getInt("require-kills", 0);
            d.requireSync = s.getInt("require-sync", 0);
            d.overuseAt = s.getInt("overuse-at", 3);
            d.heartLoss = s.getInt("heart-loss", 0);
            d.heartLossMinutes = s.getInt("heart-loss-minutes", 30);
            return d;
        }

        @Override
        public void write(ItemDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("material", d.material.name());
            s.set("lore", d.lore);
            s.set("custom-model-data", d.customModelData);
            s.set("kind", d.kind.name());
            s.set("summon", d.summonId);
            s.set("buff-seconds", d.buffSeconds);
            s.set("fatigue-seconds", d.fatigueSeconds);
            s.set("cooldown-seconds", d.cooldownSeconds);
            s.set("rune-stat", d.runeStat.name());
            s.set("rune-value", d.runeValue);
            s.set("require-kills", d.requireKills);
            s.set("require-sync", d.requireSync);
            s.set("overuse-at", d.overuseAt);
            s.set("heart-loss", d.heartLoss);
            s.set("heart-loss-minutes", d.heartLossMinutes);
        }
    };
}
