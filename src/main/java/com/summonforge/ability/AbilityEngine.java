package com.summonforge.ability;

import com.summonforge.api.AbilityCastEvent;
import com.summonforge.core.SummonForge;
import com.summonforge.damage.DamageContext;
import com.summonforge.damage.DamagePipeline;
import com.summonforge.util.Attrs;
import com.summonforge.util.Fx;
import com.summonforge.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Motor genérico: ejecuta una AbilityDef (forma + efecto + visual) con cooldown y telegrafiado. */
public final class AbilityEngine {
    private final SummonForge plugin;
    private final DamagePipeline damage;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private BukkitTask pruneTask;
    private int projectiles;

    public AbilityEngine(SummonForge plugin, DamagePipeline damage) {
        this.plugin = plugin;
        this.damage = damage;
    }

    public void start() {
        pruneTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int now = Bukkit.getCurrentTick();
            cooldowns.values().removeIf(until -> until <= now);
        }, 600L, 600L);
    }

    public void stop() {
        if (pruneTask != null) pruneTask.cancel();
        pruneTask = null;
        cooldowns.clear();
    }

    public int remainingTicks(UUID caster, String abilityId) {
        Integer until = cooldowns.get(caster + ":" + abilityId);
        return until == null ? 0 : Math.max(0, until - Bukkit.getCurrentTick());
    }

    public int projectileCount() {
        return projectiles;
    }

    public boolean ready(UUID caster, String abilityId) {
        return remainingTicks(caster, abilityId) == 0;
    }

    public void clear(UUID caster) {
        cooldowns.keySet().removeIf(k -> k.startsWith(caster + ":"));
    }

    /** @return true si se lanzó (false si estaba en cooldown). */
    public boolean cast(CastContext c, AbilityDef d, boolean ignoreCooldown) {
        UUID id = c.caster().getUniqueId();
        if (!ignoreCooldown && !ready(id, d.id)) return false;
        AbilityCastEvent ev = new AbilityCastEvent(c.caster(), c.owner(), d);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;
        cooldowns.put(id + ":" + d.id, Bukkit.getCurrentTick() + d.cooldownTicks);
        if (d.warnTicks > 0) {
            preview(c, d);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (c.caster().isValid()) run(c, d);
            }, d.warnTicks);
        } else {
            run(c, d);
        }
        return true;
    }

    // ---------- telegrafiado ----------

    private void preview(CastContext c, AbilityDef d) {
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= d.warnTicks || !c.caster().isValid()) {
                    cancel();
                    return;
                }
                World w = c.origin().getWorld();
                switch (d.shape) {
                    case CIRCLE -> Fx.ring(w, c.caster().getLocation(), d.radius, "CRIT");
                    case CONE -> Fx.cone(w, c.origin(), c.direction(), d.range, d.angle, "CRIT");
                    case LINE, PROJECTILE -> Fx.line(w, c.origin(), c.direction(), d.range, "CRIT");
                }
                t += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    // ---------- ejecución ----------

    private void run(CastContext c, AbilityDef d) {
        World w = c.origin().getWorld();
        if (w == null) return;
        Fx.sound(c.origin(), d.sound, 1f, 1f);
        if (!d.special.isEmpty() && plugin.specials().run(d.special, c, d)) return;
        switch (d.shape) {
            case CIRCLE -> {
                Location center = c.caster().getLocation();
                Fx.ring(w, center, d.radius, d.particle);
                Fx.spawn(w, d.particle, center.clone().add(0, 1, 0), 20, d.radius / 3, 0.5, d.radius / 3, 0.02);
                hitAll(c, d, w.getNearbyLivingEntities(center, d.radius, d.radius, d.radius), center);
            }
            case CONE -> {
                Fx.cone(w, c.origin(), c.direction(), d.range, d.angle, d.particle);
                Set<LivingEntity> targets = new LinkedHashSet<>();
                for (LivingEntity le : w.getNearbyLivingEntities(c.origin(), d.range, d.range, d.range)) {
                    Vector to = le.getLocation().add(0, le.getHeight() / 2, 0).toVector().subtract(c.origin().toVector());
                    if (to.lengthSquared() < 1.0E-4 || to.length() > d.range) continue;
                    if (Math.toDegrees(c.direction().angle(to)) <= d.angle / 2) targets.add(le);
                }
                hitAll(c, d, targets, c.origin());
            }
            case LINE -> {
                Vector dir = c.direction().clone().normalize();
                Set<LivingEntity> targets = new LinkedHashSet<>();
                for (double t = 0.5; t <= d.range; t += 0.5) {
                    Location p = c.origin().clone().add(dir.clone().multiply(t));
                    if (!p.getBlock().isPassable()) break;
                    Fx.spawn(w, d.particle, p, 2, 0.05, 0.05, 0.05, 0);
                    targets.addAll(w.getNearbyLivingEntities(p, 1.0, 1.0, 1.0));
                }
                hitAll(c, d, targets, c.origin());
            }
            case PROJECTILE -> projectile(c, d);
        }
    }

    private void hitAll(CastContext c, AbilityDef d, Collection<LivingEntity> targets, Location from) {
        for (LivingEntity t : targets) hit(c, d, t, from);
    }

    private void hit(CastContext c, AbilityDef d, LivingEntity t, Location from) {
        if (t instanceof ArmorStand || t.isDead()) return;
        switch (d.effect) {
            case DAMAGE -> {
                if (t == c.caster()) return;
                double dealt = damage.apply(new DamageContext(c.caster(), c.owner(), t, d.damage * c.scale(), d.element, d.id));
                if (dealt > 0 && d.knockback > 0) push(t, from, d.knockback);
                if (dealt > 0 && !d.status.isEmpty()) plugin.statuses().apply(t, d.status, d.statusTicks, d.statusStacks, c.caster());
            }
            case STATUS -> {
                if (t != c.caster() && !ally(c, t) && plugin.damage().canHit(new DamageContext(c.caster(), c.owner(), t, 1, d.element, d.id))) {
                    plugin.statuses().apply(t, d.status, d.statusTicks, d.statusStacks, c.caster());
                }
            }
            case HEAL -> {
                if (!ally(c, t)) return;
                double max = Attrs.maxHealth(t);
                t.setHealth(Math.min(max, t.getHealth() + d.damage * c.scale()));
                Fx.spawn(t.getWorld(), "HEART", t.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0);
            }
            case LIMBO -> {
                if (t instanceof Player pl && t != c.caster() && !ally(c, t)
                        && damage.canHit(new DamageContext(c.caster(), c.owner(), t, 1, d.element, d.id))) {
                    plugin.limbo().send(pl, Math.max(1, d.statusTicks / 20));
                }
            }
            case PHASE -> {
                if (t == c.caster() || (c.owner() != null && t == c.owner())) phase(t, d.statusTicks);
            }
            case PUSH -> {
                if (t != c.caster() && !ally(c, t)) push(t, from, d.knockback);
            }
            case PULL -> {
                if (t != c.caster() && !ally(c, t)) push(t, from, -d.knockback);
            }
        }
    }

    private void phase(LivingEntity t, int ticks) {
        if (t.isInvulnerable()) return;
        t.setInvulnerable(true);
        Fx.spawn(t.getWorld(), "PORTAL", t.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.2);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (t.isValid()) t.setInvulnerable(false);
        }, Math.max(1, ticks));
    }

    public boolean ally(CastContext c, LivingEntity t) {
        if (t == c.caster()) return true;
        if (c.owner() != null) {
            if (t.getUniqueId().equals(c.owner().getUniqueId())) return true;
            UUID so = Tags.summonOwner(t);
            return so != null && so.equals(c.owner().getUniqueId());
        }
        return Tags.isBoss(c.caster()) && Tags.isBoss(t);
    }

    private void push(LivingEntity t, Location from, double power) {
        Vector v = t.getLocation().toVector().subtract(from.toVector());
        v.setY(0);
        if (v.lengthSquared() < 1.0E-4) return;
        v.normalize().multiply(power);
        v.setY(power > 0 ? 0.25 : 0.1);
        if (Double.isNaN(v.getX()) || Double.isNaN(v.getZ())) return;
        t.setVelocity(t.getVelocity().add(v));
    }

    private void projectile(CastContext c, AbilityDef d) {
        if (projectiles >= plugin.getConfig().getInt("limits.max-projectiles", 80)) return;
        projectiles++;
        Location pos = c.origin().clone();
        Vector dir = c.direction().clone().normalize();
        double speed = Math.max(0.2, d.speed);
        World w = pos.getWorld();
        new BukkitRunnable() {
            double travelled = 0;

            @Override
            public void run() {
                int steps = (int) Math.ceil(speed / 0.5);
                double step = speed / steps;
                for (int i = 0; i < steps; i++) {
                    pos.add(dir.clone().multiply(step));
                    travelled += step;
                    Fx.spawn(w, d.particle, pos, 2, 0.05, 0.05, 0.05, 0);
                    boolean blocked = !pos.getBlock().isPassable();
                    LivingEntity victim = null;
                    for (LivingEntity le : w.getNearbyLivingEntities(pos, 0.8, 0.8, 0.8)) {
                        if (le instanceof ArmorStand || ally(c, le)) continue;
                        victim = le;
                        break;
                    }
                    if (blocked || victim != null || travelled >= d.range) {
                        explode(c, d, pos, victim);
                        cancel();
                        projectiles--;
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode(CastContext c, AbilityDef d, Location pos, LivingEntity victim) {
        World w = pos.getWorld();
        Fx.spawn(w, "EXPLOSION", pos, 1, 0, 0, 0, 0);
        Fx.sound(pos, d.sound, 1f, 1.2f);
        if (d.radius > 0) {
            hitAll(c, d, w.getNearbyLivingEntities(pos, d.radius, d.radius, d.radius), pos);
        } else if (victim != null) {
            hit(c, d, victim, pos);
        }
    }
}
