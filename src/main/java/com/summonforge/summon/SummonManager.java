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
import com.summonforge.util.ChunkKeeper;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemFactory;
import com.summonforge.item.ItemKind;
import com.summonforge.item.RuneStat;
import com.summonforge.util.Fx;
import com.summonforge.util.Potions;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Ciclo de vida de las invocaciones Compañero: spawn, IA, habilidades, nivel, muerte y limpieza. */
public final class SummonManager implements Listener {
    public static final class Active {
        final UUID owner;
        LivingEntity entity;
        Location lastLoc;
        double lastRatio = 1;
        int restores;
        final ChunkKeeper chunk = new ChunkKeeper();
        final List<UUID> squad = new ArrayList<>();
        final SummonDef def;
        final int level;
        final boolean preview;
        int previewUntil;
        double power = 1;
        int syncAcc;
        int perfectUntil;
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
    private final Set<UUID> squadIds = new HashSet<>();
    private final Map<UUID, Active> byEntity = new HashMap<>();
    private final Map<UUID, UUID> byOwner = new HashMap<>();
    private final Map<UUID, Integer> reviveAt = new HashMap<>();
    private final Map<UUID, Long> lastInput = new HashMap<>();
    private BukkitTask task;
    private int hudTick;

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
        List<String> pool = pool(pd, def);
        List<String> out = new ArrayList<>();
        for (String id : pd.equipped) if (pool.contains(id) && !out.contains(id)) out.add(id);
        if (out.isEmpty()) {
            int max = plugin.getConfig().getInt("summon.max-equipped", 3);
            for (String id : pool) {
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
        return byEntity.containsKey(entityId) || squadIds.contains(entityId);
    }

    /** Habilidades disponibles: las de la invocación y, si ha despertado, las de despertar. */
    public List<String> pool(PlayerData pd, SummonDef def) {
        List<String> out = new ArrayList<>(def.abilities);
        if (pd.awakening >= 1) {
            for (String id : def.awakenAbilities) if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    /** Suma (en fracción) de las runas equipadas que potencian esa estadística. */
    public double runeBonus(PlayerData pd, RuneStat stat) {
        double sum = 0;
        for (String id : pd.runes) {
            ItemDef d = plugin.itemStore().get(id);
            if (d != null && d.kind == ItemKind.RUNE && d.runeStat == stat) sum += d.runeValue / 100.0;
        }
        return sum;
    }

    public String stageName(int level) {
        List<String> names = plugin.getConfig().getStringList("awakening-stages");
        if (level <= 0 || names.isEmpty()) return "-";
        return names.get(Math.min(level, names.size()) - 1);
    }

    private boolean takeItem(Player p, String itemId) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null && itemId.equals(ItemFactory.idOf(s))) {
                s.setAmount(s.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /** Evoluciona a una rama: pasa a nivel 1 con talentos reiniciados (conserva lealtad, despertar, fusión y runas). */
    public boolean evolve(Player p, String targetId) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        SummonDef cur = plugin.summonStore().get(pd.summonId);
        SummonDef next = plugin.summonStore().get(targetId);
        if (cur == null || next == null || !cur.evolutions.contains(targetId)) return false;
        if (pd.level < cur.evolveLevel) {
            plugin.messages().send(p, "evolve-level", "n", String.valueOf(cur.evolveLevel));
            return false;
        }
        if (!cur.evolveItem.isEmpty() && !takeItem(p, cur.evolveItem)) {
            ItemDef need = plugin.itemStore().get(cur.evolveItem);
            plugin.messages().send(p, "evolve-item", "item", need != null ? need.name : cur.evolveItem);
            return false;
        }
        dismiss(p.getUniqueId());
        pd.summonId = next.id;
        pd.level = 1;
        pd.talents.clear();
        pd.equipped.clear();
        plugin.players().save(pd);
        plugin.messages().send(p, "evolved", "name", next.name);
        Fx.spawn(p.getWorld(), "END_ROD", p.getLocation().add(0, 1, 0), 60, 0.7, 1, 0.7, 0.1);
        Fx.sound(p.getLocation(), "entity.player.levelup", 1f, 0.5f);
        return true;
    }

    private void startPerfect(Active a, Player owner, int now) {
        int ticks = plugin.getConfig().getInt("summon.perfect-seconds", 30) * 20;
        a.perfectUntil = now + ticks;
        Potions.add(owner, "strength", ticks, 1);
        Potions.add(owner, "speed", ticks, 1);
        plugin.messages().send(owner, "perfect-start");
        Fx.spawn(owner.getWorld(), "END_ROD", owner.getLocation().add(0, 1, 0), 60, 0.7, 1, 0.7, 0.1);
        Fx.sound(owner.getLocation(), "block.beacon.activate", 1f, 1.5f);
    }

    /** Costo del Modo Perfecto: sincronización a 20 % y agotamiento del dueño. */
    private void endPerfect(Active a, Player owner) {
        a.perfectUntil = 0;
        plugin.players().get(a.owner).sync = 20;
        Potions.add(owner, "weakness", 300, 0);
        Potions.add(owner, "slowness", 300, 1);
        plugin.messages().send(owner, "perfect-end");
    }

    public double maxMana(PlayerData pd) {
        return (plugin.getConfig().getDouble("summon.mana-base", 100) + pd.level * plugin.getConfig().getDouble("summon.mana-per-level", 10))
                * (1 + runeBonus(pd, RuneStat.MANA));
    }

    /** Marca un objetivo para la invocación del jugador (modo "Objetivo marcado"). */
    public boolean mark(Player p, LivingEntity target) {
        Active a = activeOf(p.getUniqueId());
        if (a == null || Tags.isSummon(target) || target.getUniqueId().equals(p.getUniqueId())) return false;
        a.marked = target.getUniqueId();
        return true;
    }

    /** Lanza la primera habilidad "ultimate" equipada. */
    public void castUltimate(Player p) {
        Active a = activeOf(p.getUniqueId());
        if (a == null) return;
        List<String> load = loadout(p.getUniqueId(), a.def);
        for (int i = 0; i < load.size(); i++) {
            AbilityDef d = plugin.abilityStore().get(load.get(i));
            if (d != null && d.ultimate) {
                castSlot(p, i);
                return;
            }
        }
        plugin.messages().send(p, "no-ultimate");
    }

    private LivingEntity markedOf(Active a, Mob mob) {
        if (a.marked == null) return null;
        Entity m = Bukkit.getEntity(a.marked);
        if (m instanceof LivingEntity le && le.isValid() && !le.isDead() && le.getWorld() == mob.getWorld()
                && le.getLocation().distanceSquared(mob.getLocation()) < 900) return le;
        a.marked = null;
        return null;
    }

    private net.kyori.adventure.text.Component hudText(Active a, PlayerData pd) {
        StringBuilder sb = new StringBuilder("<aqua>Maná <white>").append((int) pd.mana).append('/').append((int) maxMana(pd)).append(" <dark_gray>|");
        for (String id : loadout(a.owner, a.def)) {
            AbilityDef d = plugin.abilityStore().get(id);
            if (d == null) continue;
            int rem = plugin.abilities().remainingTicks(a.entity.getUniqueId(), id);
            String n = Text.plain(Text.mm(d.name));
            if (n.length() > 10) n = n.substring(0, 10);
            sb.append(' ').append(rem == 0 ? "<green>" : "<red>").append(Text.escape(n)).append(rem == 0 ? " ✔" : " " + (rem / 20 + 1) + "s");
        }
        return Text.mm(sb.toString());
    }

    /** Si Paper recreó la entidad (p. ej. al cruzar de dimensión), adopta la nueva instancia con el mismo UUID. */
    private void refreshEntity(Active a) {
        LivingEntity e = a.entity;
        if (e.isValid() || e.isDead()) return;
        Entity found = Bukkit.getEntity(e.getUniqueId());
        if (found instanceof LivingEntity fl && fl.isValid() && fl != e) {
            a.entity = fl;
            plugin.visuals().release(fl.getUniqueId(), false);
            plugin.visuals().track(fl, a.def.modelId, a.def.anims, a.def.particle, a.def.hideBase);
        }
    }

    private void removeSquad(Active a) {
        for (UUID id : a.squad) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
            squadIds.remove(id);
        }
        a.squad.clear();
    }

    /** Los mobs del escuadrón atacan lo que ataca el líder y, si no, siguen al dueño. */
    private void squadTick(Active a, Player owner) {
        if (a.squad.isEmpty()) return;
        LivingEntity leaderTarget = a.entity instanceof Mob lm ? lm.getTarget() : null;
        a.squad.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            boolean gone = e == null || !e.isValid();
            if (gone) squadIds.remove(id);
            return gone;
        });
        for (UUID id : a.squad) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof Mob m)) continue;
            if (m.getWorld() != owner.getWorld() || m.getLocation().distanceSquared(owner.getLocation()) > 1600) {
                m.teleport(a.entity.getLocation());
                continue;
            }
            if (leaderTarget != null && leaderTarget.isValid()) {
                m.setTarget(leaderTarget);
                SquadSpawner.castFor(plugin, m, leaderTarget, owner);
            } else {
                m.setTarget(null);
                if (m.getLocation().distanceSquared(owner.getLocation()) > 64) m.getPathfinder().moveTo(owner.getLocation(), 1.2);
            }
        }
    }

    /** La invocación desapareció sin morir: se recrea junto al dueño con la misma vida. */
    private void restoreSummon(Active a) {
        Player owner = Bukkit.getPlayer(a.owner);
        UUID oldId = a.entity.getUniqueId();
        byEntity.remove(oldId);
        byOwner.remove(a.owner, oldId);
        plugin.visuals().release(oldId, false);
        plugin.abilities().clear(oldId);
        a.chunk.release(plugin);
        removeSquad(a);
        if (owner == null || !owner.isOnline()) return;
        plugin.getLogger().warning("La invocación de " + owner.getName() + " desapareció sin morir; se restaura (" + (a.restores + 1) + "/3).");
        Active n = spawn(owner, a.def, a.level, false);
        if (n == null) return;
        n.restores = a.restores + 1;
        n.marked = a.marked;
        n.syncAcc = a.syncAcc;
        double max = Attrs.maxHealth(n.entity);
        n.entity.setHealth(Math.max(1, Math.min(max, max * a.lastRatio)));
    }

    private double powerOf(Active a) {
        PlayerData pd = plugin.players().get(a.owner);
        return a.power * (1 + pd.sync / 100.0 * 0.25 + pd.awakening * 0.10 + (a.perfectUntil > 0 ? 0.40 : 0));
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
        PlayerData mpd = plugin.players().get(p.getUniqueId());
        int cost = d.effectiveMana();
        if (mpd.mana < cost) {
            plugin.messages().send(p, "no-mana", "cost", String.valueOf(cost));
            return;
        }
        CastContext cc = new CastContext(a.entity, p, a.entity.getEyeLocation(), p.getEyeLocation().getDirection(), powerOf(a));
        if (plugin.abilities().cast(cc, d, false)) {
            mpd.mana -= cost;
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
        double fus = 1 + pd.fusion / 100.0;
        hpMul = (hpMul + runeBonus(pd, RuneStat.HEALTH)) * fus;
        dmgMul = (dmgMul + runeBonus(pd, RuneStat.DAMAGE)) * fus;
        spdMul = spdMul + runeBonus(pd, RuneStat.SPEED);
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
        plugin.visuals().track(le, def.modelId, def.anims, def.particle, def.hideBase);
        if (def.squadEnabled) {
            List<UUID> ids = SquadSpawner.spawn(plugin, le, owner.getUniqueId(), def.squad);
            a.squad.addAll(ids);
            squadIds.addAll(ids);
        }
        return a;
    }

    private void remove(Active a, boolean death) {
        a.chunk.release(plugin);
        removeSquad(a);
        byEntity.remove(a.entity.getUniqueId());
        if (!a.preview) byOwner.remove(a.owner, a.entity.getUniqueId());
        plugin.visuals().release(a.entity.getUniqueId(), death);
        plugin.abilities().clear(a.entity.getUniqueId());
        if (!death && a.entity.isValid()) a.entity.remove();
    }

    // ---------- tick / IA ----------

    private void tick() {
        int now = Bukkit.getCurrentTick();
        hudTick += 4;
        boolean hud = hudTick >= 20;
        if (hud) hudTick = 0;
        for (Active a : new ArrayList<>(byEntity.values())) {
            refreshEntity(a);
            if (!a.entity.isDead() && !a.entity.isValid() && !a.preview && a.restores < 3 && a.lastLoc != null) {
                restoreSummon(a);
                continue;
            }
            if (!a.entity.isValid() || a.entity.isDead()) {
                remove(a, false);
                continue;
            }
            a.lastLoc = a.entity.getLocation();
            a.lastRatio = Math.max(0.01, a.entity.getHealth() / Attrs.maxHealth(a.entity));
            a.chunk.hold(plugin, a.lastLoc);
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
            squadTick(a, owner);
            if (a.perfectUntil > 0 && now >= a.perfectUntil) endPerfect(a, owner);
            a.syncAcc += 4;
            if (a.syncAcc >= 200) {
                a.syncAcc = 0;
                PlayerData spd = plugin.players().get(a.owner);
                if (spd.sync < 100) spd.sync++;
                if (spd.sync >= 100 && a.perfectUntil == 0) startPerfect(a, owner, now);
            }
            if (hud) {
                PlayerData mpd = plugin.players().get(a.owner);
                mpd.mana = Math.min(maxMana(mpd), mpd.mana + plugin.getConfig().getDouble("summon.mana-regen", 3));
                owner.sendActionBar(hudText(a, mpd));
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
        LivingEntity target = switch (mode) {
            case FOLLOW -> null;
            case MARKED -> markedOf(a, mob);
            case DEFENSIVE, STAY -> pickTarget(a, mob, 6);
            default -> pickTarget(a, mob, 14);
        };
        mob.setTarget(target);
        if (target == null) {
            if (a.def.type == SummonType.COMPANION && mode != SummonMode.STAY && mob.getLocation().distanceSquared(owner.getLocation()) > 36) {
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
            plugin.players().get(a.owner).mana -= d.effectiveMana();
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
            if (plugin.players().get(a.owner).mana < d.effectiveMana()) continue;
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
        pd.kills++;
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
