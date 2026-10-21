package com.summonforge.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * Acceso a atributos por nombre corto ("max_health", "attack_damage"...) vía Registry.
 * Evita depender de si el enum se llama GENERIC_MAX_HEALTH (<=1.21.1) o MAX_HEALTH (>=1.21.2).
 */
public final class Attrs {
    private Attrs() {}

    public static Attribute find(String shortName) {
        Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + shortName));
        if (a == null) a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(shortName));
        return a;
    }

    public static void set(LivingEntity e, String shortName, double value) {
        Attribute a = find(shortName);
        if (a == null) return;
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(value);
    }

    public static double get(LivingEntity e, String shortName, double def) {
        Attribute a = find(shortName);
        if (a == null) return def;
        AttributeInstance i = e.getAttribute(a);
        return i == null ? def : i.getValue();
    }

    public static double maxHealth(LivingEntity e) {
        return get(e, "max_health", 20.0);
    }
}
