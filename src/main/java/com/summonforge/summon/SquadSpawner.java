package com.summonforge.summon;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.ability.Effect;
import com.summonforge.ability.Shape;
import com.summonforge.core.Keys;
import com.summonforge.core.SummonForge;
import com.summonforge.util.Attrs;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Crea los mobs extra del escuadrón (de una invocación o de la escolta de un boss) alrededor del líder. */
public final class SquadSpawner {
    private SquadSpawner() {}

    /** @param ownerId dueño si es una invocación (los marca como aliados); null para la escolta de un boss. */
    public static List<UUID> spawn(SummonForge plugin, LivingEntity leader, UUID ownerId, SquadEntry[] entries) {
        List<UUID> out = new ArrayList<>();
        int cap = plugin.getConfig().getInt("limits.max-squad-members", 12);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (SquadEntry se : entries) {
            for (int i = 0; i < se.count && out.size() < cap; i++) {
                Location l = leader.getLocation().clone().add(r.nextDouble(-2.5, 2.5), 0, r.nextDouble(-2.5, 2.5));
                SummonDef sd = se.summonId.isEmpty() ? null : plugin.summonStore().get(se.summonId);
                EntityType type = sd != null ? sd.baseMob : se.type;
                Entity e = l.getWorld().spawnEntity(l, type);
                if (!(e instanceof LivingEntity le)) {
                    e.remove();
                    continue;
                }
                le.setPersistent(false);
                le.setRemoveWhenFarAway(false);
                le.setCanPickupItems(false);
                if (sd != null) {
                    // Esbirro = una de tus invocaciones: usa SUS stats (x vida / x daño de la línea), su modelo y sus habilidades.
                    double hp = Math.max(4, sd.health * se.hpMul);
                    Attrs.set(le, "max_health", hp);
                    le.setHealth(Math.min(Attrs.maxHealth(le), hp));
                    Attrs.set(le, "attack_damage", Math.max(1, sd.damage * se.dmgMul));
                    Attrs.set(le, "movement_speed", sd.speed);
                    Attrs.set(le, "scale", sd.scale);
                    le.getPersistentDataContainer().set(Keys.SUMMON_ID, PersistentDataType.STRING, sd.id);
                    le.customName(Text.mm(se.name.isBlank() ? sd.name : se.name));
                    le.setCustomNameVisible(true);
                    plugin.visuals().track(le, sd.modelId, sd.anims, sd.particle, sd.hideBase);
                } else {
                    double hp = Math.max(4, Attrs.maxHealth(leader) * se.hpMul);
                    Attrs.set(le, "max_health", hp);
                    le.setHealth(Math.min(Attrs.maxHealth(le), hp));
                    Attrs.set(le, "attack_damage", Math.max(1, Attrs.get(leader, "attack_damage", 5) * se.dmgMul));
                    if (!se.name.isBlank()) {
                        le.customName(Text.mm(se.name));
                        le.setCustomNameVisible(true);
                    }
                }
                if (ownerId != null) {
                    le.getPersistentDataContainer().set(Keys.SUMMON_OWNER, PersistentDataType.STRING, ownerId.toString());
                    if (le instanceof IronGolem g) g.setPlayerCreated(true);
                }
                out.add(le.getUniqueId());
            }
        }
        return out;
    }

    /** Las invocaciones elegidas como esbirros usan las habilidades de su definición contra el objetivo. */
    public static void castFor(SummonForge plugin, Mob m, LivingEntity target, Player owner) {
        String sid = m.getPersistentDataContainer().get(Keys.SUMMON_ID, PersistentDataType.STRING);
        if (sid == null || target == null || !target.isValid() || m.getWorld() != target.getWorld()) return;
        SummonDef sd = plugin.summonStore().get(sid);
        if (sd == null || sd.abilities.isEmpty()) return;
        double dist = m.getLocation().distance(target.getLocation());
        for (String id : sd.abilities) {
            AbilityDef d = plugin.abilityStore().get(id);
            if (d == null || d.effect == Effect.HEAL || !plugin.abilities().ready(m.getUniqueId(), d.id)) continue;
            if (d.shape == Shape.CIRCLE ? dist > d.radius + 1.5 : dist > d.range) continue;
            Vector dir = target.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(m.getEyeLocation().toVector());
            if (dir.lengthSquared() < 1.0E-4) return;
            if (plugin.abilities().cast(new CastContext(m, owner, m.getEyeLocation(), dir.normalize(), 1.0), d, false)) return;
        }
    }

    public static void remove(List<UUID> ids) {
        for (UUID id : ids) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        ids.clear();
    }
}
