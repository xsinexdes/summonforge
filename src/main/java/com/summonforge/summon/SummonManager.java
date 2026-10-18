package com.summonforge.summon;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.ability.Effect;
import com.summonforge.ability.Shape;
import com.summonforge.api.SummonSpawnEvent;
import com.summonforge.core.Keys;
import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.model.EntityVisual;
import com.summonforge.util.Attrs;
import com.summonforge.util.Fx;
import com.summonforge.util.Tags;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Ciclo de vida de las invocaciones Compañero: spawn, IA, habilidades, nivel, muerte y limpieza. */
public final class SummonManager implements Listener {
    public static final class Active {
        final UUID owner;
        final LivingEntity entity;
        final SummonDef def;
        final int level;
        final boolean preview;
        int previewUntil;
        double power = 1;
        int syncAcc;
        final int spawnTick = Bukkit.getCurrentTick();
        int nextCast;
        UUID marked;

        Active(UUID owner, LivingEntity entity, SummonDef def, int level, boolean preview) {
            this.owner = owner;
            this.entity = entity;
            this.def = def;
            this.level = level;
            this.preview = preview;
        }

        public LivingEntity entity() { return entity; }
        public SummonDef def() { return def; }
        public int level() { return level; }
    }

    private final SummonForge plugin;
    private final Map<UUID, Active> byEntity = new HashMap<>();
    private final Map<UUID, UUID> byOwner = new HashMap<>();
    private final Map<UUID, Integer> reviveAt = new HashMap<>();
    private final Map<UUID, Long> lastInput = new HashMap<>();
    private BukkitTask task;

    public SummonManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(new SummonInput(plugin, this), plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 4L, 4L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (Active a : new ArrayList<>(byEntity.values())) remove(a, false);
    }

    public Active activeOf(UUID owner) {
        UUID id = byOwner.get(owner);
        return id == null ? null : byEntity.get(id);
    }

    /** Anti-spam de entrada por jugador. */
    public boolean throttle(UUID player, long minMs) {
        long now = System.currentTimeMillis();
        Long last = lastInput.get(player);
        if (last != null && now - last < minMs) return false;
        lastInput.put(player, now);
        return true;
    }

    /** Habilidades que realmente usa: las equipadas por el jugador, o las primeras N si no ha elegido. */
    public List<String> loadout(UUID owner, SummonDef def) {
        PlayerData pd = plugin.players().get(owner);
        List<String> out = new ArrayList<>();
        for (String id : pd.equipped) if (def.abilities.contains(id) && !out.contains(id)) out.add(id);
        if (out.isEmpty()) {
            int max = plugin.getConfig().getInt("summon.max-equipped", 3);
            for (String id : def.abilities) {
                if (out.size() >= max) break;
                out.add(id);
            }
        }
        return out;
    }

    public int count() {
        return byEntity.size();
    }

    public boolean isTracked(UUID entityId) {
        return byEntity.containsKey(entityId);
    }

    private double powerOf(Active a) {
        return a.power * (1 + plugin.players().get(a.owner).sync / 100.0 * 0.25);
    }

    private int cooldownOf(SummonDef def) {
        return def.cooldownSeconds > 0 ? def.cooldownSeconds : plugin.getConfig().getInt("summon.revive-cooldown-seconds", 30);
    }

    public double levelScale(SummonDef def, int level) {
        return 1 + def.perLevelPct * (level - 1);
    }

    // ---------- acciones del jugador ----------

    public void toggle(Player p) {
        if (activeOf(p.getUniqueId()) != null) {
            dismiss(p.getUniqueId());
            plugin.messages().send(p, "summon-dismissed");
        } else {
            summon(p);
        }
    }

    public boolean summon(Player p) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        SummonDef def = plugin.summonStore().get(pd.summonId);
        if (def == null) {
            plugin.messages().send(p, "summon-none");
            return false;
        }
        if (activeOf(p.getUniqueId()) != null) return false;
        Integer until = reviveAt.get(p.getUniqueId());
        if (until != null && until > Bukkit.getCurrentTick()) {
            plugin.messages().send(p, "summon-revive-wait", "s", String.valueOf((until - Bukkit.getCurrentTick()) / 20));
            return false;
        }
        Active a = spawn(p, def, pd.level, false);
        if (a == null) return false;
        plugin.messages().send(p, "summon-spawned", "name", def.name, "level", String.valueOf(pd.level));
        return true;
    }

    public void dismiss(UUID owner) {
        Active a = activeOf(owner);
        if (a != null) remove(a, false);
    }

    /** Invocación temporal de prueba (botón "Probar"): no toca los datos del jugador. */
    public void preview(Player p, SummonDef def) {
        Active a = spawn(p, def, 1, true);
        if (a != null) a.previewUntil = Bukkit.getCurrentTick() + 400;
    }

    public void grant(Player p, SummonDef def) {
        dismiss(p.getUniqueId());
        PlayerData pd = plugin.players().get(p.getUniqueId());
        pd.summonId = def.id;
        pd.level = 1;
        plugin.players().save(pd);
        plugin.messages().send(p, "summon-got", "name", def.name);
    }

    public void upgrade(Player p) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        SummonDef def = plugin.summonStore().get(pd.summonId);
        if (def == null) {
            plugin.messages().send(p, "summon-none");
            return;
        }
        if (pd.level >= PlayerData.MAX_LEVEL) {
            plugin.messages().send(p, "summon-max");
            return;
        }
        int cost = def.upgradeXpLevels * pd.level;
        if (p.getLevel() < cost) {
            plugin.messages().send(p, "summon-no-xp", "cost", String.valueOf(cost));
            return;
        }
        p.setLevel(p.getLevel() - cost);
        pd.level++;
        plugin.players().save(pd);
        plugin.messages().send(p, "summon-levelup", "level", String.valueOf(pd.level));
        if (activeOf(p.getUniqueId()) != null) {
            dismiss(p.getUniqueId());
            summon(p);
        }
    }

    public void castSlot(Player p, int index) {
        Active a = activeOf(p.getUniqueId());
        if (a == null) {
            plugin.messages().send(p, "summon-not-active");
            return;
        }
        List<String> load = loadout(p.getUniqueId(), a.def);
        if (index < 0 || index >= load.size()) {
            plugin.messages().send(p, "ability-missing");
            return;
        }
        AbilityDef d = plugin.abilityStore().get(load.get(index));
        if (d == null) {
            plugin.messages().send(p, "ability-missing");
            return;
        }
        if (a.entity.getWorld() != p.getWorld() || a.entity.getLocation().distanceSquared(p.getLocation()) > 1600) return;
        int rem = plugin.abilities().remainingTicks(a.entity.getUniqueId(), d.id);
        if (rem > 0) {
            plugin.messages().send(p, "ability-cooldown", "s", String.format("%.1f", rem / 20.0));
            return;
        }
        CastContext cc = new CastContext(a.entity, p, a.entity.getEyeLocation(), p.getEyeLocation().getDirection(), powerOf(a));
        if (plugin.abilities().cast(cc, d, false)) {
            EntityVisual v = plugin.visuals().get(a.entity.getUniqueId());
            if (v != null) v.play("attack", 600);
        }
    }

    // ---------- spawn / remove ----------

    private Active spawn(Player owner, SummonDef def, int level, boolean preview) {
        if (!preview) {
            SummonSpawnEvent ev = new SummonSpawnEvent(owner, def, false);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) return null;
        }
        Location loc;
        if (def.type == SummonType.AURA) {
            loc = owner.getLocation().clone();
        } else if (def.type == SummonType.FREE) {
            org.bukkit.block.Block b = owner.getTargetBlockExact(20);
            loc = b != null ? b.getLocation().add(0.5, 1, 0.5) : owner.getLocation().clone().add(Fx.forward(owner.getLocation()).multiply(6));
        } else {
            loc = owner.getLocation().clone().add(Fx.forward(owner.getLocation()).multiply(2.5));
        }
        org.bukkit.entity.EntityType bodyType = def.baseMob;
        if (def.type == SummonType.MOUNT && bodyType != org.bukkit.entity.EntityType.HORSE
                && bodyType != org.bukkit.entity.EntityType.SKELETON_HORSE && bodyType != org.bukkit.entity.EntityType.ZOMBIE_HORSE) {
            bodyType = org.bukkit.entity.EntityType.HORSE;
        }
        Entity ent = loc.getWorld().spawnEntity(loc, bodyType);
        if (!(ent instanceof LivingEntity le)) {
            ent.remove();
            return null;
        }
        double scale = levelScale(def, level);
        le.setPersistent(false);
        le.setRemoveWhenFarAway(false);
        le.setCanPickupItems(false);
        PlayerData pd = plugin.players().get(owner.getUniqueId());
        double loyal = 1 + Math.min(10, pd.loyalty / 100) * 0.02;
        double hpMul = 1 + Talent.VITALIDAD.perPoint * pd.talents.getOrDefault(Talent.VITALIDAD.name(), 0);
        double dmgMul = (1 + Talent.FUERZA.perPoint * pd.talents.getOrDefault(Talent.FUERZA.name(), 0)) * loyal;
        double spdMul = 1 + Talent.CELERIDAD.perPoint * pd.talents.getOrDefault(Talent.CELERIDAD.name(), 0);
        Attrs.set(le, "max_health", def.health * scale * hpMul);
        le.setHealth(Math.min(Attrs.maxHealth(le), def.health * scale * hpMul));
        Attrs.set(le, "attack_damage", def.damage * scale * dmgMul);
        Attrs.set(le, "movement_speed", def.speed * spdMul);
        Attrs.set(le, "scale", def.scale);
        Attrs.set(le, "follow_range", 32);
        if (le instanceof IronGolem g) g.setPlayerCreated(true);
        if (def.type == SummonType.AURA) {
            le.setInvulnerable(true);
            le.setCollidable(false);
        }
        if (le instanceof AbstractHorse h && def.type == SummonType.MOUNT) {
            h.setTamed(true);
            h.setOwner(owner);
            h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            Attrs.set(le, "jump_strength", 0.8);
            h.addPassenger(owner);
        }
        le.customName(Text.mm(def.name + " <gray>[Nv. " + level + "]"));
        le.setCustomNameVisible(true);
        le.getPersistentDataContainer().set(Keys.SUMMON_OWNER, PersistentDataType.STRING, owner.getUniqueId().toString());
        le.getPersistentDataContainer().set(Keys.SUMMON_ID, PersistentDataType.STRING, def.id);

        Active a = new Active(owner.getUniqueId(), le, def, level, preview);
        a.power = scale * dmgMul;
        byEntity.put(le.getUniqueId(), a);
        if (!preview) byOwner.put(owner.getUniqueId(), le.getUniqueId());
        plugin.visuals().track(le, def.modelId, def.anims, def.particle);
        return a;
    }

    private void remove(Active a, boolean death) {
        byEntity.remove(a.entity.getUniqueId());
        if (!a.preview) byOwner.remove(a.owner, a.entity.getUniqueId());
        plugin.visuals().release(a.entity.getUniqueId(), death);
        plugin.abilities().clear(a.entity.getUniqueId());
        if (!death && a.entity.isValid()) a.entity.remove();
    }

    // ---------- tick / IA ----------

    private void tick() {
        int now = Bukkit.getCurrentTick();
        for (Active a : new ArrayList<>(byEntity.values())) {
            if (!a.entity.isValid() || a.entity.isDead()) {
                remove(a, false);
                continue;
            }
            if (a.preview) {
                if (now >= a.previewUntil) remove(a, false);
                continue;
            }
            Player owner = Bukkit.getPlayer(a.owner);
            if (owner == null || !owner.isOnline() || owner.isDead()) {
                remove(a, false);
                continue;
            }
            if (a.def.durationSeconds > 0 && now - a.spawnTick >= a.def.durationSeconds * 20) {
                int cd = cooldownOf(a.def);
                remove(a, false);
                reviveAt.put(a.owner, now + cd * 20);
                plugin.messages().send(owner, "summon-expired", "s", String.valueOf(cd));
                continue;
            }
            ai(a, owner, now);
            a.syncAcc += 4;
            if (a.syncAcc >= 200) {
                a.syncAcc = 0;
                PlayerData spd = plugin.players().get(a.owner);
                if (spd.sync < 100) spd.sync++;
            }
        }
    }

    private void ai(Active a, Player owner, int now) {
        if (!(a.entity instanceof Mob mob)) return;
        boolean free = a.def.type == SummonType.FREE;
        if (free) {
            if (owner.getWorld() != mob.getWorld()) {
                remove(a, false);
                return;
            }
        } else if (owner.getWorld() != mob.getWorld() || mob.getLocation().distanceSquared(owner.getLocation()) > 900) {
            mob.teleport(owner.getLocation());
            return;
        }
        if (a.def.type == SummonType.AURA && mob.getLocation().distanceSquared(owner.getLocation()) > 0.5) {
            mob.teleport(owner.getLocation());
        }
        if (a.def.type == SummonType.MOUNT) {
            if (!mob.getPassengers().contains(owner) && mob.getLocation().distanceSquared(owner.getLocation()) > 36) {
                mob.getPathfinder().moveTo(owner.getLocation(), 1.2);
            }
            return;
        }
        SummonMode mode = plugin.players().get(a.owner).mode;
        LivingEntity target = mode == SummonMode.PASSIVE ? null : pickTarget(a, mob, mode == SummonMode.DEFENSIVE ? 6 : 14);
        mob.setTarget(target);
        if (target == null) {
            if (a.def.type == SummonType.COMPANION && mob.getLocation().distanceSquared(owner.getLocation()) > 36) {
                mob.getPathfinder().moveTo(owner.getLocation(), 1.2);
            }
            return;
        }
        List<String> load = loadout(a.owner, a.def);
        if (now < a.nextCast || load.isEmpty()) return;
        double dist = mob.getLocation().distance(target.getLocation());
        AbilityDef d = pickAbility(a, owner, dist, load);
        if (d == null) return;
        Vector dir = target.getLocation().add(0, target.getHeight() / 2, 0).toVector().subtract(mob.getEyeLocation().toVector());
        if (dir.lengthSquared() < 1.0E-4) return;
        CastContext cc = new CastContext(mob, owner, mob.getEyeLocation(), dir.normalize(), powerOf(a));
        if (plugin.abilities().cast(cc, d, false)) {
            a.nextCast = now + a.def.castIntervalTicks;
            EntityVisual v = plugin.visuals().get(mob.getUniqueId());
            if (v != null) v.play("attack", 600);
        }
    }

    private LivingEntity pickTarget(Active a, Mob mob, double radius) {
        if (a.marked != null) {
            Entity m = Bukkit.getEntity(a.marked);
            if (m instanceof LivingEntity le && le.isValid() && !le.isDead() && le.getWorld() == mob.getWorld()
                    && le.getLocation().distanceSquared(mob.getLocation()) < 900) return le;
            a.marked = null;
        }
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mob.getNearbyEntities(radius, 8, radius)) {
            if (!(e instanceof LivingEntity le) || !le.isValid() || le.isDead() || Tags.isSummon(le)) continue;
            if (!(le instanceof Monster) && !Tags.isBoss(le)) continue;
            double dd = le.getLocation().distanceSquared(mob.getLocation());
            if (dd < bestD) {
                bestD = dd;
                best = le;
            }
        }
        return best;
    }

    private AbilityDef pickAbility(Active a, Player owner, double dist, List<String> load) {
        List<AbilityDef> ok = new ArrayList<>();
        for (String id : load) {
            AbilityDef d = plugin.abilityStore().get(id);
            if (d == null || !plugin.abilities().ready(a.entity.getUniqueId(), d.id)) continue;
            if (d.effect == Effect.HEAL) {
                boolean hurt = owner.getHealth() < Attrs.maxHealth(owner) * 0.7
                        || a.entity.getHealth() < Attrs.maxHealth(a.entity) * 0.7;
                if (!hurt) continue;
            } else if (d.shape == Shape.CIRCLE ? dist > d.radius + 1.5 : dist > d.range) {
                continue;
            }
            ok.add(d);
        }
        return ok.isEmpty() ? null : ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
    }

    // ---------- eventos ----------

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        LivingEntity t = e.getTarget();
        if (t == null) return;
        UUID owner = Tags.summonOwner(e.getEntity());
        if (owner != null) {
            if (t instanceof Player || owner.equals(Tags.summonOwner(t))) e.setCancelled(true);
        } else if (Tags.isBoss(e.getEntity()) && Tags.isBoss(t)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Player p = damager instanceof Player pl ? pl
                : (damager instanceof Projectile pr && pr.getShooter() instanceof Player ps ? ps : null);
        if (p != null && e.getEntity() instanceof LivingEntity victim && !Tags.isSummon(victim)) {
            Active a = activeOf(p.getUniqueId());
            if (a != null) a.marked = victim.getUniqueId();
        }
        Active s = byEntity.get(damager.getUniqueId());
        if (s != null) {
            EntityVisual v = plugin.visuals().get(s.entity.getUniqueId());
            if (v != null) v.play("attack", 600);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOwnerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Active a = activeOf(p.getUniqueId());
        if (a == null || a.def.type != SummonType.AURA || a.def.ownerDamageReduction <= 0) return;
        e.setDamage(e.getDamage() * (1 - Math.min(90, a.def.ownerDamageReduction) / 100.0));
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player || Tags.isSummon(dead)) return;
        if (!(dead.getLastDamageCause() instanceof EntityDamageByEntityEvent be)) return;
        Active a = byEntity.get(be.getDamager().getUniqueId());
        if (a == null || a.preview) return;
        PlayerData pd = plugin.players().get(a.owner);
        int before = pd.loyalty / 100;
        pd.loyalty += Tags.isBoss(dead) ? 25 : 1;
        int now = pd.loyalty / 100;
        Player o = Bukkit.getPlayer(a.owner);
        if (now > before && now <= 10 && o != null) plugin.messages().send(o, "loyalty-up", "level", String.valueOf(now));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Active a = byEntity.get(e.getEntity().getUniqueId());
        if (a == null) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        remove(a, true);
        if (a.preview) return;
        int cd = cooldownOf(a.def);
        reviveAt.put(a.owner, Bukkit.getCurrentTick() + cd * 20);
        plugin.players().get(a.owner).sync /= 2;
        Player o = Bukkit.getPlayer(a.owner);
        if (o != null) plugin.messages().send(o, "summon-dead", "s", String.valueOf(cd));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dismiss(e.getPlayer().getUniqueId());
        lastInput.remove(e.getPlayer().getUniqueId());
    }
}
