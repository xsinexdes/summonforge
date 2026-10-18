package com.summonforge.admin;

import com.summonforge.ability.AbilityDef;
import com.summonforge.boss.BossDef;
import com.summonforge.core.Keys;
import com.summonforge.core.SummonForge;
import com.summonforge.item.ItemDef;
import com.summonforge.summon.SummonDef;
import com.summonforge.util.Attrs;
import com.summonforge.util.Tags;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Herramientas de administración: validador, estado/rendimiento, rescate de entidades huérfanas y dummy de DPS. */
public final class AdminTools implements Listener {
    private final SummonForge plugin;
    private final Map<UUID, Deque<double[]>> hits = new HashMap<>();

    public AdminTools(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        hits.clear();
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity e : w.getLivingEntities()) {
                if (isDummy(e)) e.remove();
            }
        }
    }

    // ---------- validador ----------

    public void validate(CommandSender to) {
        List<String> issues = new ArrayList<>();
        boolean models = plugin.model().available();
        for (SummonDef s : plugin.summonStore().all()) {
            for (String id : s.abilities) if (!plugin.abilityStore().has(id)) issues.add("Invocación " + s.id + ": habilidad inexistente '" + id + "'");
            if (models && !s.modelId.isEmpty() && !plugin.model().modelKeys().contains(s.modelId)) issues.add("Invocación " + s.id + ": modelo inexistente '" + s.modelId + "'");
            if (s.abilities.isEmpty()) issues.add("Invocación " + s.id + ": sin habilidades");
        }
        for (BossDef b : plugin.bossStore().all()) {
            for (String id : b.abilities) if (!plugin.abilityStore().has(id)) issues.add("Boss " + b.id + ": habilidad inexistente '" + id + "'");
            for (int i = 0; i < b.phases.length; i++) {
                for (String id : b.phases[i].abilities) if (!plugin.abilityStore().has(id)) issues.add("Boss " + b.id + " fase " + (i + 2) + ": habilidad inexistente '" + id + "'");
            }
            if (!b.dropItemId.isEmpty() && !plugin.itemStore().has(b.dropItemId)) issues.add("Boss " + b.id + ": objeto de drop inexistente '" + b.dropItemId + "'");
            if (models && !b.modelId.isEmpty() && !plugin.model().modelKeys().contains(b.modelId)) issues.add("Boss " + b.id + ": modelo inexistente '" + b.modelId + "'");
            if (b.spawnEnabled && org.bukkit.Bukkit.getWorld(b.spawnWorld) == null) issues.add("Boss " + b.id + ": mundo de spawn inexistente '" + b.spawnWorld + "'");
            if (b.abilities.isEmpty()) issues.add("Boss " + b.id + ": sin habilidades");
        }
        for (ItemDef i : plugin.itemStore().all()) {
            if (!i.summonId.isEmpty() && !plugin.summonStore().has(i.summonId)) issues.add("Objeto " + i.id + ": invocación inexistente '" + i.summonId + "'");
        }
        for (AbilityDef a : plugin.abilityStore().all()) {
            if (!a.status.isEmpty() && !plugin.statusStore().has(a.status)) issues.add("Habilidad " + a.id + ": estado inexistente '" + a.status + "'");
        }
        if (issues.isEmpty()) {
            to.sendMessage(Text.mm("<green>Validación correcta: sin referencias rotas."));
            return;
        }
        to.sendMessage(Text.mm("<yellow>Problemas encontrados: <white>" + issues.size()));
        for (int i = 0; i < Math.min(20, issues.size()); i++) to.sendMessage(Text.mm("<gray>- " + Text.escape(issues.get(i))));
        if (issues.size() > 20) to.sendMessage(Text.mm("<gray>... y " + (issues.size() - 20) + " más."));
    }

    // ---------- estado / rendimiento ----------

    public void status(CommandSender to) {
        to.sendMessage(Text.mm("<gradient:#8e44ad:#3498db>SummonForge</gradient> <gray>- estado"));
        to.sendMessage(Text.mm("<gray>Definiciones: habilidades <white>" + plugin.abilityStore().size()
                + "<gray>, invocaciones <white>" + plugin.summonStore().size()
                + "<gray>, bosses <white>" + plugin.bossStore().size()
                + "<gray>, objetos <white>" + plugin.itemStore().size()
                + "<gray>, estados <white>" + plugin.statusStore().size()));
        to.sendMessage(Text.mm("<gray>Activos: invocaciones <white>" + plugin.summons().count()
                + "<gray>, bosses <white>" + plugin.bosses().count()
                + "<gray>, proyectiles <white>" + plugin.abilities().projectileCount()
                + "<gray>, objetivos con estados <white>" + plugin.statuses().count()));
        to.sendMessage(Text.mm("<gray>BetterModel: " + (plugin.model().available()
                ? "<green>activo (" + plugin.model().modelKeys().size() + " modelos)" : "<red>no disponible")));
    }

    // ---------- rescate ----------

    /** Elimina entidades marcadas por el plugin que ya no controla ningún manager. */
    public int rescue() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity e : w.getLivingEntities()) {
                boolean orphanBoss = Tags.isBoss(e) && !plugin.bosses().isTracked(e.getUniqueId());
                boolean orphanSummon = Tags.isSummon(e) && !plugin.summons().isTracked(e.getUniqueId());
                if (orphanBoss || orphanSummon) {
                    e.remove();
                    n++;
                }
            }
        }
        return n;
    }

    // ---------- dummy ----------

    private boolean isDummy(Entity e) {
        return e.getPersistentDataContainer().has(Keys.DUMMY, PersistentDataType.BYTE);
    }

    public void spawnDummy(Player p) {
        Location l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(4));
        Entity ent = p.getWorld().spawnEntity(l, EntityType.HUSK);
        if (!(ent instanceof Mob m)) {
            ent.remove();
            return;
        }
        m.setAI(false);
        m.setPersistent(false);
        m.setRemoveWhenFarAway(false);
        Attrs.set(m, "max_health", 2048);
        m.setHealth(2048);
        m.customName(Text.mm("<gray>Dummy de pruebas"));
        m.setCustomNameVisible(true);
        m.getPersistentDataContainer().set(Keys.DUMMY, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity dummy) || !isDummy(dummy)) return;
        Player p = Tags.resolvePlayer(e.getDamager());
        if (p == null) return;
        long now = System.currentTimeMillis();
        Deque<double[]> q = hits.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
        q.addLast(new double[]{now, e.getFinalDamage()});
        while (!q.isEmpty() && now - (long) q.peekFirst()[0] > 5000) q.removeFirst();
        double sum = 0;
        for (double[] h : q) sum += h[1];
        p.sendActionBar(Text.mm("<yellow>Golpe: <white>" + Math.round(e.getFinalDamage()) + " <gray>| <yellow>DPS (5 s): <white>" + Math.round(sum / 5.0)));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (dummy.isValid()) dummy.setHealth(Attrs.maxHealth(dummy));
        });
    }
}
