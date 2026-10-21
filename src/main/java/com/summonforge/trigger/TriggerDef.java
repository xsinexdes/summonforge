package com.summonforge.trigger;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/** Regla "SI [evento] ENTONCES [acción]" creada con botones, sin escribir código. */
public final class TriggerDef implements Definition {
    public final String id;
    public String name;
    public Material icon = Material.COMMAND_BLOCK;
    public boolean enabled = true;
    public TriggerEvent event = TriggerEvent.OWNER_HURT;
    /** Probabilidad 0-100 de que la regla se dispare cuando ocurre el evento. */
    public double chance = 100;
    /** Umbral (vida %) para OWNER_LOW_HEALTH. */
    public double value = 30;
    public TriggerAction action = TriggerAction.MESSAGE;
    public String param = "";
    public int cooldownSeconds = 30;

    public TriggerDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return icon; }

    public static final Codec<TriggerDef> CODEC = new Codec<>() {
        @Override
        public TriggerDef read(String id, ConfigurationSection s) {
            TriggerDef d = new TriggerDef(id);
            d.name = s.getString("name", id);
            d.icon = Enums.material(s.getString("icon"), Material.COMMAND_BLOCK);
            d.enabled = s.getBoolean("enabled", true);
            d.event = Enums.parse(TriggerEvent.class, s.getString("event"), TriggerEvent.OWNER_HURT);
            d.chance = s.getDouble("chance", 100);
            d.value = s.getDouble("value", 30);
            d.action = Enums.parse(TriggerAction.class, s.getString("action"), TriggerAction.MESSAGE);
            d.param = s.getString("param", "");
            d.cooldownSeconds = s.getInt("cooldown-seconds", 30);
            return d;
        }

        @Override
        public void write(TriggerDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("icon", d.icon.name());
            s.set("enabled", d.enabled);
            s.set("event", d.event.name());
            s.set("chance", d.chance);
            s.set("value", d.value);
            s.set("action", d.action.name());
            s.set("param", d.param);
            s.set("cooldown-seconds", d.cooldownSeconds);
        }
    };
}
