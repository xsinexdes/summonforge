package com.summonforge.status;

import com.summonforge.core.SummonForge;
import com.summonforge.damage.DamageContext;
import com.summonforge.util.Fx;
import com.summonforge.util.Tags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Aplica estados con duración y stacks; el daño continuo pasa por el DamagePipeline (1 tick por segundo). */
public final class StatusEngine implements Listener {
    private static final class Inst {
        int until;
        int stacks;
        UUID source;
    }

    private final SummonForge plugin;
    private final Map<UUID, Map<String, Inst>> active = new HashMap<>();
    private BukkitTask task;

    public StatusEngine(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        active.clear();
    }

    public int count() {
        return active.size();
    }

    public boolean has(LivingEntity e, String statusId) {
        Map<String, Inst> m = active.get(e.getUniqueId());
        return m != null && m.containsKey(statusId);
    }

    public void apply(LivingEntity target, String statusId, int ticks, int stacks, LivingEntity source) {
        if (statusId == null || statusId.isEmpty() || target instanceof ArmorStand || !target.isValid()) return;
        StatusDef def = plugin.statusStore().get(statusId);
        if (def == null) return;
        Map<String, Inst> m = active.computeIfAbsent(target.getUniqueId(), k -> new HashMap<>());
        Inst in = m.computeIfAbsent(statusId, k -> new Inst());
        in.stacks = Math.min(def.maxStacks, in.stacks + Math.max(1, stacks));
        in.until = Bukkit.getCurrentTick() + Math.max(20, ticks);
        if (source != null) in.source = source.getUniqueId();
        potions(target, def, ticks);
        Location l = target.getLocation().add(0, target.getHeight() + 0.2, 0);
        Fx.spawn(target.getWorld(), def.particle, l, 6, 0.3, 0.2, 0.3, 0.02);
    }

    private void potions(LivingEntity t, StatusDef def, int ticks) {
        int dur = Math.max(20, ticks);
        if (def.slowAmp >= 0) add(t, "slowness", dur, def.slowAmp);
        if (def.stun) add(t, "slowness", dur, 6);
        if (def.weakAmp >= 0) add(t, "weakness", dur, def.weakAmp);
    }

    private void add(LivingEntity t, String effect, int dur, int amp) {
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(effect));
        if (type != null) t.addPotionEffect(new PotionEffect(type, dur, amp, false, false, true));
    }

    private void tick() {
        int now = Bukkit.getCurrentTick();
        Iterator<Map.Entry<UUID, Map<String, Inst>>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Map<String, Inst>> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (!(ent instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                it.remove();
                continue;
            }
            Iterator<Map.Entry<String, Inst>> sit = en.getValue().entrySet().iterator();
            while (sit.hasNext()) {
                Map.Entry<String, Inst> se = sit.next();
                StatusDef def = plugin.statusStore().get(se.getKey());
                Inst in = se.getValue();
                if (def == null || now >= in.until) {
                    sit.remove();
                    continue;
                }
                if (def.dotDamage > 0) dot(target, def, in);
                Fx.spawn(target.getWorld(), def.particle, target.getLocation().add(0, target.getHeight() / 2, 0), 3, 0.3, 0.4, 0.3, 0.01);
            }
            if (en.getValue().isEmpty()) it.remove();
        }
    }

    private void dot(LivingEntity target, StatusDef def, Inst in) {
        double dmg = def.dotDamage * in.stacks;
        Entity src = in.source == null ? null : Bukkit.getEntity(in.source);
        if (src instanceof LivingEntity attacker && attacker.isValid()) {
            Player owner = Tags.resolvePlayer(attacker);
            plugin.damage().apply(new DamageContext(attacker, owner, target, dmg, def.element, "status:" + def.id));
        } else {
            target.damage(dmg);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        active.remove(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        active.remove(e.getPlayer().getUniqueId());
    }
}
