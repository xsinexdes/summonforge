package com.summonforge.util;

import com.summonforge.core.Keys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/** Lectura de las etiquetas PDC con las que marcamos nuestras entidades. */
public final class Tags {
    private Tags() {}

    public static String bossId(Entity e) {
        return e.getPersistentDataContainer().get(Keys.BOSS, PersistentDataType.STRING);
    }

    public static boolean isBoss(Entity e) {
        return e != null && bossId(e) != null;
    }

    public static UUID summonOwner(Entity e) {
        if (e == null) return null;
        String s = e.getPersistentDataContainer().get(Keys.SUMMON_OWNER, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String summonId(Entity e) {
        return e == null ? null : e.getPersistentDataContainer().get(Keys.SUMMON_ID, PersistentDataType.STRING);
    }

    public static boolean isSummon(Entity e) {
        return summonOwner(e) != null;
    }

    /** Jugador responsable de un daño: jugador directo, tirador de proyectil o dueño de la invocación. */
    public static Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player ps) return ps;
        UUID owner = summonOwner(damager);
        return owner == null ? null : Bukkit.getPlayer(owner);
    }
}
