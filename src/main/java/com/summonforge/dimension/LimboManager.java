package com.summonforge.dimension;

import com.summonforge.core.SummonForge;
import com.summonforge.damage.DamageContext;
import com.summonforge.stats.Element;
import com.summonforge.util.Potions;
import com.summonforge.util.Tags;
import com.summonforge.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Dimensiones temporales (mundos vacíos con plataforma). Envía jugadores Y mobs a una dimensión por un tiempo,
 * con daño continuo opcional ("matar ahí"), y los devuelve al terminar o antes si muere el último enemigo / el lanzador.
 */
public final class LimboManager implements Listener {
    private static final class Group {
        UUID caster;
        Realm realm;
        final Set<UUID> members = new HashSet<>();
        final Set<UUID> enemies = new HashSet<>();
        BukkitTask dot;
        boolean closed;
    }

    private final SummonForge plugin;
    private final Map<UUID, Location> returns = new HashMap<>();
    private final Map<UUID, Group> groups = new HashMap<>();
    private final Map<Realm, World> worlds = new EnumMap<>(Realm.class);

    public LimboManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        for (Group g : new HashSet<>(groups.values())) close(g);
        for (UUID id : new ArrayList<>(returns.keySet())) back(id);
    }

    public boolean isInRealm(UUID id) {
        return returns.containsKey(id);
    }

    private World world(Realm r) {
        World w = worlds.get(r);
        if (w != null) return w;
        try {
            w = Bukkit.getWorld(r.world);
            if (w == null) {
                w = new WorldCreator(r.world).generator(new VoidGenerator()).environment(World.Environment.NORMAL).createWorld();
                if (w != null) {
                    for (int x = -10; x <= 10; x++) {
                        for (int z = -10; z <= 10; z++) w.getBlockAt(x, 64, z).setType(r.floor);
                    }
                    w.setSpawnLocation(0, 65, 0);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear el mundo " + r.world, e);
        }
        if (w != null) worlds.put(r, w);
        return w;
    }

    /** Envía solo a un jugador al limbo (efecto LIMBO). */
    public boolean send(Player p, int seconds) {
        return enter(null, false, List.of(p), Realm.LIMBO, seconds, 0, Element.NONE, "limbo");
    }

    /**
     * @param caster      quien lanza (atribuye el daño continuo; puede ser null)
     * @param bringCaster si el lanzador también viaja a la dimensión (duelo)
     * @param dot         daño por segundo a los enemigos dentro (0 = ninguno)
     * @return false si no se pudo enviar a nadie
     */
    public boolean enter(LivingEntity caster, boolean bringCaster, List<? extends LivingEntity> enemies, Realm realm,
                         int seconds, double dot, Element element, String source) {
        World w = world(realm);
        if (w == null) return false;
        Group g = new Group();
        g.realm = realm;
        List<LivingEntity> all = new ArrayList<>();
        if (caster != null) {
            g.caster = caster.getUniqueId();
            if (bringCaster && !returns.containsKey(caster.getUniqueId())) all.add(caster);
        }
        for (LivingEntity e : enemies) {
            if (e != caster && e.isValid() && !returns.containsKey(e.getUniqueId())) {
                all.add(e);
                g.enemies.add(e.getUniqueId());
            }
        }
        if (g.enemies.isEmpty()) return false;
        int n = all.size();
        for (int i = 0; i < n; i++) {
            LivingEntity e = all.get(i);
            double ang = 2 * Math.PI * i / n;
            double r = n == 1 ? 0 : 5;
            Location l = new Location(w, 0.5 + Math.cos(ang) * r, 65, 0.5 + Math.sin(ang) * r);
            l.setDirection(new Vector(-Math.cos(ang) - 0.01, 0, -Math.sin(ang)));
            returns.put(e.getUniqueId(), e.getLocation());
            groups.put(e.getUniqueId(), g);
            g.members.add(e.getUniqueId());
            e.teleport(l);
            if (realm == Realm.ILLUSION && e instanceof Player p && g.enemies.contains(p.getUniqueId())) {
                p.showTitle(Title.title(Text.mm("<dark_red>Tsukuyomi"), Text.mm("<gray>Estás atrapado en una ilusión"),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> close(g), Math.max(1, seconds) * 20L);
        if (dot > 0 || realm == Realm.ILLUSION) {
            g.dot = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickGroup(g, caster, dot, element, source), 20L, 20L);
        }
        return true;
    }

    private void tickGroup(Group g, LivingEntity caster, double dot, Element element, String source) {
        if (g.closed) return;
        Player owner = caster == null ? null : Tags.resolvePlayer(caster);
        for (UUID id : new ArrayList<>(g.enemies)) {
            Entity ent = Bukkit.getEntity(id);
            if (!(ent instanceof LivingEntity le) || !le.isValid()) continue;
            if (dot > 0 && caster != null && caster.isValid()) {
                plugin.damage().apply(new DamageContext(caster, owner, le, dot, element, source));
            }
            if (g.realm == Realm.ILLUSION) {
                Potions.add(le, "blindness", 40, 0);
                Potions.add(le, "nausea", 60, 0);
            }
        }
    }

    private void close(Group g) {
        if (g.closed) return;
        g.closed = true;
        if (g.dot != null) g.dot.cancel();
        for (UUID id : new ArrayList<>(g.members)) {
            groups.remove(id);
            back(id);
        }
        g.members.clear();
    }

    private void back(UUID id) {
        Location l = returns.remove(id);
        Entity e = Bukkit.getEntity(id);
        if (e != null && e.isValid() && l != null && l.getWorld() != null) e.teleport(l);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        returns.remove(id);
        Group g = groups.remove(id);
        if (g == null) return;
        g.members.remove(id);
        g.enemies.remove(id);
        if (id.equals(g.caster) || g.enemies.isEmpty()) Bukkit.getScheduler().runTask(plugin, () -> close(g));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        back(id);
        Group g = groups.remove(id);
        if (g == null) return;
        g.members.remove(id);
        g.enemies.remove(id);
        if (id.equals(g.caster) || g.enemies.isEmpty()) close(g);
    }

    /** Si alguien entra y está en una dimensión sin registro (crash/reinicio), lo saca al mundo principal. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        for (Realm r : Realm.values()) {
            if (p.getWorld().getName().equals(r.world) && !returns.containsKey(p.getUniqueId())) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                return;
            }
        }
    }
}
