package com.summonforge.trigger;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.api.BossDeathEvent;
import com.summonforge.api.SummonSpawnEvent;
import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.summon.SummonManager;
import com.summonforge.util.Attrs;
import com.summonforge.util.Tags;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Evalúa las reglas SI/ENTONCES definidas por el admin. Todas actúan sobre el dueño de la invocación. */
public final class TriggerEngine implements Listener {
    private final SummonForge plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public TriggerEngine(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        cooldowns.clear();
    }

    private void fire(TriggerEvent ev, Player owner, double value) {
        long now = System.currentTimeMillis();
        for (TriggerDef d : plugin.triggerStore().all()) {
            if (!d.enabled || d.event != ev) continue;
            if (ev == TriggerEvent.OWNER_LOW_HEALTH && value > d.value) continue;
            if (ThreadLocalRandom.current().nextDouble() * 100.0 >= d.chance) continue;
            String key = d.id + ":" + owner.getUniqueId();
            Long until = cooldowns.get(key);
            if (until != null && now < until) continue;
            cooldowns.put(key, now + d.cooldownSeconds * 1000L);
            execute(d, owner);
        }
    }

    public void execute(TriggerDef d, Player owner) {
        switch (d.action) {
            case CAST_ABILITY -> {
                AbilityDef ab = plugin.abilityStore().get(d.param);
                if (ab == null) return;
                SummonManager.Active a = plugin.summons().activeOf(owner.getUniqueId());
                LivingEntity caster = a != null ? a.entity() : owner;
                plugin.abilities().cast(new CastContext(caster, owner, caster.getEyeLocation(),
                        owner.getEyeLocation().getDirection(), 1.0), ab, false);
            }
            case APPLY_STATUS -> plugin.statuses().apply(owner, d.param, 100, 1, null);
            case HEAL_OWNER -> {
                double amount = number(d.param, 4);
                owner.setHealth(Math.min(Attrs.maxHealth(owner), owner.getHealth() + amount));
            }
            case GIVE_MANA -> {
                PlayerData pd = plugin.players().get(owner.getUniqueId());
                pd.mana = Math.min(plugin.summons().maxMana(pd), pd.mana + number(d.param, 20));
            }
            case MESSAGE -> owner.sendMessage(Text.mm(d.param));
        }
    }

    private static double number(String s, double def) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || plugin.summons().activeOf(p.getUniqueId()) == null) return;
        fire(TriggerEvent.OWNER_HURT, p, 0);
        double pct = (p.getHealth() - e.getFinalDamage()) / Attrs.maxHealth(p) * 100.0;
        fire(TriggerEvent.OWNER_LOW_HEALTH, p, pct);
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (!(e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent be)) return;
        Entity damager = be.getDamager();
        if (!plugin.summons().isTracked(damager.getUniqueId())) return;
        UUID owner = Tags.summonOwner(damager);
        Player p = owner == null ? null : Bukkit.getPlayer(owner);
        if (p != null) fire(TriggerEvent.SUMMON_KILL, p, 0);
    }

    @EventHandler
    public void onBossDeath(BossDeathEvent e) {
        for (UUID id : e.getDamage().keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) fire(TriggerEvent.BOSS_KILL, p, 0);
        }
    }

    @EventHandler
    public void onSummon(SummonSpawnEvent e) {
        if (e.isPreview() || e.isCancelled()) return;
        Player p = e.getOwner();
        Bukkit.getScheduler().runTask(plugin, () -> fire(TriggerEvent.SUMMON_SPAWN, p, 0));
    }
}
