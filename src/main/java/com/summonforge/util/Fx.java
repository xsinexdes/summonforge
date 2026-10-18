package com.summonforge.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Partículas, sonidos y formas básicas. Todo por nombre (string) para ser robusto entre versiones. */
public final class Fx {
    public static final List<String> PARTICLES = List.of(
            "FLAME", "SOUL_FIRE_FLAME", "CRIT", "ELECTRIC_SPARK", "SNOWFLAKE", "CLOUD", "END_ROD",
            "WITCH", "SMOKE", "EXPLOSION", "SWEEP_ATTACK", "HEART", "PORTAL", "ENCHANT");

    private static final Map<String, Particle> CACHE = new ConcurrentHashMap<>();

    private Fx() {}

    public static Particle particle(String name) {
        return CACHE.computeIfAbsent(name == null ? "" : name, n -> {
            try {
                return Particle.valueOf(n.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return Particle.FLAME;
            }
        });
    }

    public static void spawn(World w, String particle, Location l, int count, double dx, double dy, double dz, double speed) {
        w.spawnParticle(particle(particle), l, count, dx, dy, dz, speed);
    }

    public static void sound(Location l, String key, float volume, float pitch) {
        if (key == null || key.isBlank() || l.getWorld() == null) return;
        l.getWorld().playSound(l, key, volume, pitch);
    }

    public static Vector forward(Location loc) {
        Vector v = loc.getDirection().setY(0);
        if (v.lengthSquared() < 1.0E-4) v = new Vector(1, 0, 0);
        return v.normalize();
    }

    public static void ring(World w, Location c, double r, String particle) {
        int n = Math.max(16, (int) (r * 8));
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            spawn(w, particle, c.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r), 1, 0, 0, 0, 0);
        }
    }

    public static void line(World w, Location o, Vector dir, double len, String particle) {
        Vector d = dir.clone().normalize();
        for (double t = 0.5; t <= len; t += 0.6) {
            spawn(w, particle, o.clone().add(d.clone().multiply(t)), 1, 0, 0, 0, 0);
        }
    }

    public static void cone(World w, Location o, Vector dir, double range, double angle, String particle) {
        Vector flat = dir.clone().setY(0);
        if (flat.lengthSquared() < 1.0E-4) flat = new Vector(1, 0, 0);
        flat.normalize();
        double half = Math.toRadians(angle / 2);
        int n = Math.max(4, (int) (angle / 12));
        for (int i = 0; i <= n; i++) {
            double a = -half + 2 * half * i / n;
            Vector v = flat.clone().rotateAroundY(a).multiply(range);
            spawn(w, particle, o.clone().add(v).add(0, 0.2, 0), 1, 0, 0, 0, 0);
        }
        line(w, o, flat.clone().rotateAroundY(-half), range, particle);
        line(w, o, flat.clone().rotateAroundY(half), range, particle);
    }
}
