package com.summonforge.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Efectos de poción por nombre corto ("slowness", "strength"...) vía Registry (robusto entre versiones). */
public final class Potions {
    private Potions() {}

    public static void add(LivingEntity t, String effect, int ticks, int amplifier) {
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effect));
        if (type != null) t.addPotionEffect(new PotionEffect(type, Math.max(1, ticks), Math.max(0, amplifier), false, false, true));
    }
}
