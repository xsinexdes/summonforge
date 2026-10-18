package com.summonforge.dimension;

import com.summonforge.core.SummonForge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Dimensión "limbo" (mundo vacío con plataforma). Envía jugadores temporalmente y los devuelve. */
public final class LimboManager implements Listener {
    private static final String WORLD = "sf_limbo";

    private final SummonForge plugin;
    private final Map<UUID, Location> returns = new HashMap<>();
    private World world;

    public LimboManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        for (UUID id : new ArrayList<>(returns.keySet())) back(id);
    }

    public boolean isInLimbo(UUID id) {
        return returns.containsKey(id);
    }

    private World world() {
        if (world != null) return world;
        try {
            world = Bukkit.getWorld(WORLD);
            if (world == null) {
                world = new WorldCreator(WORLD).generator(new VoidGenerator()).environment(World.Environment.NORMAL).createWorld();
                if (world != null) {
                    for (int x = -8; x <= 8; x++) {
                        for (int z = -8; z <= 8; z++) world.getBlockAt(x, 64, z).setType(Material.OBSIDIAN);
                    }
                    world.setSpawnLocation(0, 65, 0);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear el mundo limbo", e);
        }
        return world;
    }

    /** @return false si ya está en el limbo o no se pudo crear el mundo. */
    public boolean send(Player p, int seconds) {
        if (returns.containsKey(p.getUniqueId())) return false;
        World w = world();
        if (w == null) return false;
        returns.put(p.getUniqueId(), p.getLocation());
        p.teleport(new Location(w, 0.5, 65, 0.5));
        UUID id = p.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> back(id), Math.max(1, seconds) * 20L);
        return true;
    }

    public void back(UUID id) {
        Location l = returns.remove(id);
        Player p = Bukkit.getPlayer(id);
        if (p != null && l != null && l.getWorld() != null) p.teleport(l);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        back(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        returns.remove(e.getEntity().getUniqueId());
    }
}
