package com.summonforge.boss;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.ability.Shape;
import com.summonforge.api.BossDeathEvent;
import com.summonforge.api.BossSpawnEvent;
import com.summonforge.core.Keys;
import com.summonforge.data.PlayerData;
import com.summonforge.core.SummonForge;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemFactory;
import com.summonforge.model.EntityVisual;
import com.summonforge.util.Attrs;
import com.summonforge.util.Enums;
import com.summonforge.util.Fx;
import com.summonforge.util.Tags;
import com.summonforge.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** Bosses: spawn (manual y por punto fijo), IA de habilidades, barra, ranking de daño, drops y respawn. */
public final class BossManager implements Listener {
    private static final class Active {
        final BossDef def;
        final LivingEntity entity;
        final Location anchor;
        final BossBar bar;
        final Set<UUID> viewers = new HashSet<>();
        final Map<UUID, Double> damage = new HashMap<>();
        int nextCast;
        int phase = 0;
        boolean enraged;
        final int spawnTick = Bukkit.getCurrentTick();
        final java.util.List<UUID> adds = new ArrayList<>();
        final java.util.List<String> abilities = new ArrayList<>();
        long lastSeen = System.currentTimeMillis();

        Active(BossDef def, LivingEntity entity, Location anchor, BossBar bar) {
            this.def = def;
            this.entity = entity;
            this.anchor = anchor;
            this.bar = bar;
            this.abilities.addAll(def.abilities);
        }
    }

    private final SummonForge plugin;
    private final Map<UUID, Active> byEntity = new HashMap<>();
    private final Map<String, UUID> byBoss = new HashMap<>();
    private final Map<String, Long> respawnAt = new HashMap<>();
    private BukkitTask aiTask;
    private BukkitTask spawnTask;

    public BossManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        aiTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnTick, 40L, 40L);
    }

    public void stop() {
        if (aiTask != null) aiTask.cancel();
        if (spawnTask != null) spawnTask.cancel();
        aiTask = null;
        spawnTask = null;
        for (Active a : new ArrayList<>(byEntity.values())) cleanup(a, false);
    }

    private void cinematic(String id, Location focus) {
        if (id == null || id.isEmpty() || focus.getWorld() == null) return;
        List<Player> viewers = new ArrayList<>();
        for (Player p : focus.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(focus) < 64 * 64) viewers.add(p);
        }
        plugin.cinematics().play(id, viewers, focus);
    }

    public int count() {
        return byEntity.size();
    }

    public boolean isTracked(UUID entityId) {
        return byEntity.containsKey(entityId);
    }

    public boolean isAlive(String bossId) {
        return byBoss.containsKey(bossId);
    }

    public int kill(String bossId) {
        int n = 0;
        for (Active a : new ArrayList<>(byEntity.values())) {
            if (bossId == null || a.def.id.equals(bossId)) {
                cleanup(a, false);
                n++;
            }
        }
        return n;
    }

    /** @return false si ya hay uno vivo o se alcanzó el límite global. */
    public boolean spawn(BossDef def, Location loc) {
        if (byBoss.containsKey(def.id) || byEntity.size() >= plugin.getConfig().getInt("limits.max-bosses", 20)) return false;
        BossSpawnEvent spawnEvent = new BossSpawnEvent(def, loc);
        Bukkit.getPluginManager().callEvent(spawnEvent);
        if (spawnEvent.isCancelled()) return false;
        Entity ent = loc.getWorld().spawnEntity(loc, def.baseMob);
        if (!(ent instanceof LivingEntity le)) {
            ent.remove();
            return false;
        }
        le.setPersistent(false);
        le.setRemoveWhenFarAway(false);
        le.setCanPickupItems(false);
        Attrs.set(le, "max_health", def.health);
        le.setHealth(Math.min(Attrs.maxHealth(le), def.health));
        Attrs.set(le, "attack_damage", def.damage);
        Attrs.set(le, "armor", def.armor);
        Attrs.set(le, "movement_speed", def.speed);
        Attrs.set(le, "scale", def.scale);
        Attrs.set(le, "knockback_resistance", def.knockbackResist);
        Attrs.set(le, "follow_range", Math.max(16, def.detectRange));
        le.customName(Text.mm(def.name));
        le.setCustomNameVisible(true);
        le.getPersistentDataContainer().set(Keys.BOSS, PersistentDataType.STRING, def.id);
        if (def.shield > 0) le.setAbsorptionAmount(def.shield);

        BossBar bar = BossBar.bossBar(Text.mm(def.name), 1f,
                Enums.parse(BossBar.Color.class, def.barColor, BossBar.Color.RED), BossBar.Overlay.PROGRESS);
        Active a = new Active(def, le, loc.clone(), bar);
        byEntity.put(le.getUniqueId(), a);
        byBoss.put(def.id, le.getUniqueId());
        plugin.visuals().track(le, def.modelId, def.anims, def.particle);

        cinematic(def.introCinematic, loc);
        if (plugin.getConfig().getBoolean("boss.announce-spawn", true)) {
            Bukkit.broadcast(Text.mm(plugin.messages().raw("prefix")
                    + plugin.messages().raw("boss-announce", "name", def.name, "world", loc.getWorld().getName())));
        }
        return true;
    }

    // ---------- tick ----------

    private void tick() {
        int now = Bukkit.getCurrentTick();
        for (Active a : new ArrayList<>(byEntity.values())) {
            LivingEntity e = a.entity;
            if (!e.isValid() || e.isDead()) {
                cleanup(a, false);
                respawnAt.put(a.def.id, System.currentTimeMillis() + a.def.respawnMinutes * 60_000L);
                continue;
            }
            updateBar(a);
            checkPhases(a);
            checkEnrage(a, now);
            if (!(e instanceof Mob mob)) continue;

            Player target = nearestPlayer(a);
            mob.setTarget(target);
            if (target != null) a.lastSeen = System.currentTimeMillis();

            boolean far = e.getWorld() == a.anchor.getWorld()
                    && e.getLocation().distanceSquared(a.anchor) > a.def.leashRadius * a.def.leashRadius;
            if (far || (target == null && System.currentTimeMillis() - a.lastSeen > 15_000)) {
                e.teleport(a.anchor);
                e.setHealth(Attrs.maxHealth(e));
                a.damage.clear();
                a.lastSeen = System.currentTimeMillis();
                continue;
            }
            if (target == null || now < a.nextCast || a.abilities.isEmpty()) continue;

            double dist = e.getLocation().distance(target.getLocation());
            AbilityDef d = pickAbility(a, dist);
            if (d == null) continue;
            Vector dir = target.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(e.getEyeLocation().toVector());
            if (dir.lengthSquared() < 1.0E-4) continue;
            CastContext cc = new CastContext(e, null, e.getLocation().add(0, 1, 0), dir.normalize(), 1.0);
            if (plugin.abilities().cast(cc, d, false)) {
                a.nextCast = now + a.def.abilityIntervalTicks;
                EntityVisual v = plugin.visuals().get(e.getUniqueId());
                if (v != null) v.play("attack", 800);
            }
        }
    }

    private void checkEnrage(Active a, int now) {
        if (a.enraged || a.def.enrageSeconds <= 0 || now - a.spawnTick < a.def.enrageSeconds * 20) return;
        a.enraged = true;
        LivingEntity e = a.entity;
        Attrs.set(e, "attack_damage", Attrs.get(e, "attack_damage", a.def.damage) * a.def.enrageDmgMult);
        Attrs.set(e, "movement_speed", Attrs.get(e, "movement_speed", a.def.speed) * a.def.enrageSpeedMult);
        Fx.spawn(e.getWorld(), "EXPLOSION", e.getLocation().add(0, 1, 0), 3, 0.8, 0.8, 0.8, 0);
        Fx.sound(e.getLocation(), "entity.wither.spawn", 2f, 0.7f);
        for (UUID id : a.viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) plugin.messages().send(p, "boss-enrage", "name", a.def.name);
        }
    }

    private void spawnAdds(Active a, Phase ph) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < ph.addsCount && a.adds.size() < 20; i++) {
            Location l = a.entity.getLocation().clone().add(r.nextDouble(-3, 3), 0, r.nextDouble(-3, 3));
            Entity x = a.entity.getWorld().spawnEntity(l, ph.addsMob);
            if (x instanceof LivingEntity le) {
                le.setPersistent(false);
                le.setRemoveWhenFarAway(false);
                le.customName(Text.mm("<gray>Esbirro"));
                a.adds.add(le.getUniqueId());
            } else {
                x.remove();
            }
        }
    }

    private void checkPhases(Active a) {
        double ratio = a.entity.getHealth() / Attrs.maxHealth(a.entity) * 100.0;
        while (a.phase < a.def.phases.length) {
            Phase ph = a.def.phases[a.phase];
            if (ph.pct <= 0) {
                a.phase++;
                continue;
            }
            if (ratio > ph.pct) break;
            a.phase++;
            enterPhase(a, ph);
        }
    }

    private void enterPhase(Active a, Phase ph) {
        LivingEntity e = a.entity;
        Attrs.set(e, "attack_damage", Attrs.get(e, "attack_damage", a.def.damage) * ph.dmgMult);
        Attrs.set(e, "movement_speed", Attrs.get(e, "movement_speed", a.def.speed) * ph.speedMult);
        for (String id : ph.abilities) if (!a.abilities.contains(id)) a.abilities.add(id);
        a.nextCast = 0;
        if (ph.refillShield && a.def.shield > 0) e.setAbsorptionAmount(a.def.shield);
        spawnAdds(a, ph);
        Fx.spawn(e.getWorld(), "EXPLOSION", e.getLocation().add(0, 1, 0), 3, 0.8, 0.8, 0.8, 0);
        Fx.sound(e.getLocation(), "entity.ender_dragon.growl", 2f, 0.8f);
        if (!ph.message.isBlank()) {
            for (UUID id : a.viewers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.sendMessage(Text.mm(ph.message));
            }
        }
    }

    private Player nearestPlayer(Active a) {
        Player best = null;
        double bestD = a.def.detectRange * a.def.detectRange;
        for (Player p : a.entity.getWorld().getPlayers()) {
            if (p.isDead() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            double dd = p.getLocation().distanceSquared(a.entity.getLocation());
            if (dd < bestD) {
                bestD = dd;
                best = p;
            }
        }
        return best;
    }

    private AbilityDef pickAbility(Active a, double dist) {
        List<AbilityDef> ok = new ArrayList<>();
        for (String id : a.abilities) {
            AbilityDef d = plugin.abilityStore().get(id);
            if (d == null || !plugin.abilities().ready(a.entity.getUniqueId(), d.id)) continue;
            if (d.shape == Shape.CIRCLE ? dist > d.radius + 2 : dist > d.range) continue;
            ok.add(d);
        }
        return ok.isEmpty() ? null : ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
    }

    private void updateBar(Active a) {
        double max = Attrs.maxHealth(a.entity);
        a.bar.progress((float) Math.max(0.0, Math.min(1.0, a.entity.getHealth() / max)));
        for (Player p : a.entity.getWorld().getPlayers()) {
            boolean near = p.getLocation().distanceSquared(a.entity.getLocation()) < 64 * 64;
            if (near && a.viewers.add(p.getUniqueId())) p.showBossBar(a.bar);
            else if (!near && a.viewers.remove(p.getUniqueId())) p.hideBossBar(a.bar);
        }
    }

    private void spawnTick() {
        long now = System.currentTimeMillis();
        for (BossDef d : plugin.bossStore().all()) {
            if (!d.spawnEnabled || d.spawnWorld.isBlank() || byBoss.containsKey(d.id)) continue;
            if (now < respawnAt.getOrDefault(d.id, 0L)) continue;
            World w = Bukkit.getWorld(d.spawnWorld);
            if (w == null) continue;
            Location l = new Location(w, d.spawnX, d.spawnY, d.spawnZ);
            if (!w.isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) continue;
            spawn(d, l);
        }
    }

    private void cleanup(Active a, boolean death) {
        byEntity.remove(a.entity.getUniqueId());
        byBoss.remove(a.def.id, a.entity.getUniqueId());
        for (UUID id : a.viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.hideBossBar(a.bar);
        }
        a.viewers.clear();
        for (UUID id : a.adds) {
            Entity x = Bukkit.getEntity(id);
            if (x != null) x.remove();
        }
        a.adds.clear();
        plugin.visuals().release(a.entity.getUniqueId(), death);
        plugin.abilities().clear(a.entity.getUniqueId());
        if (!death && a.entity.isValid()) a.entity.remove();
    }

    // ---------- eventos ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Active a = byEntity.get(e.getEntity().getUniqueId());
        if (a == null) return;
        Player p = Tags.resolvePlayer(e.getDamager());
        if (p != null) a.damage.merge(p.getUniqueId(), e.getFinalDamage(), Double::sum);
    }

    /** Punto débil: los proyectiles que aciertan a la altura de la cabeza hacen más daño. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeadshot(EntityDamageByEntityEvent e) {
        Active a = byEntity.get(e.getEntity().getUniqueId());
        if (a == null || a.def.headshotMult <= 1 || !(e.getDamager() instanceof Projectile pr)) return;
        double headY = a.entity.getEyeLocation().getY();
        if (pr.getLocation().getY() >= headY - a.entity.getHeight() * 0.25) {
            e.setDamage(e.getDamage() * a.def.headshotMult);
            Fx.spawn(a.entity.getWorld(), "CRIT", a.entity.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Active a = byEntity.get(e.getEntity().getUniqueId());
        if (a == null) return;
        e.getDrops().clear();
        List<Map.Entry<UUID, Double>> rank = new ArrayList<>(a.damage.entrySet());
        rank.sort(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()));
        String top = rank.stream().limit(3).map(en -> {
            Player p = Bukkit.getPlayer(en.getKey());
            return (p != null ? p.getName() : "?") + " (" + Math.round(en.getValue()) + ")";
        }).collect(Collectors.joining(", "));
        if (top.isEmpty()) top = "-";
        for (Map.Entry<UUID, Double> en : rank) {
            Player p = Bukkit.getPlayer(en.getKey());
            if (p != null) plugin.messages().send(p, "boss-killed", "name", a.def.name, "top", top);
        }
        Player winner = rank.isEmpty() ? null : Bukkit.getPlayer(rank.get(0).getKey());
        for (UUID id : a.damage.keySet()) {
            PlayerData pd = plugin.players().get(id);
            pd.bossKills.merge(a.def.id, 1, Integer::sum);
            plugin.players().save(pd);
        }
        Bukkit.getPluginManager().callEvent(new BossDeathEvent(a.def, winner, a.damage));
        cinematic(a.def.outroCinematic, e.getEntity().getLocation());
        rollDrop(a, e.getEntity().getLocation(), winner);
        respawnAt.put(a.def.id, System.currentTimeMillis() + a.def.respawnMinutes * 60_000L);
        cleanup(a, true);
    }

    private void rollDrop(Active a, Location loc, Player winner) {
        if (a.def.dropItemId.isEmpty()) return;
        ItemDef idef = plugin.itemStore().get(a.def.dropItemId);
        if (idef == null || ThreadLocalRandom.current().nextDouble() * 100.0 >= a.def.dropChance) return;
        Item it = loc.getWorld().dropItem(loc.clone().add(0, 0.5, 0), ItemFactory.create(idef));
        it.setGlowing(true);
        if (winner != null) {
            it.setOwner(winner.getUniqueId());
            long secs = plugin.getConfig().getLong("drops.protect-seconds", 30);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (it.isValid()) it.setOwner(null);
            }, secs * 20L);
            Bukkit.broadcast(Text.mm(plugin.messages().raw("prefix") + plugin.messages().raw("boss-drop",
                    "player", winner.getName(), "item", idef.name, "boss", a.def.name)));
        }
    }
}
