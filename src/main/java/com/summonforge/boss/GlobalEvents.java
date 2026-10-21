package com.summonforge.boss;

import com.summonforge.core.SummonForge;
import com.summonforge.util.Fx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Evento global programado: cada N minutos aparece uno de los bosses configurados cerca de un jugador al azar. */
public final class GlobalEvents {
    private final SummonForge plugin;
    private BukkitTask task;

    public GlobalEvents(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("events.enabled", false)) return;
        long period = Math.max(1, plugin.getConfig().getLong("events.interval-minutes", 60)) * 1200L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::fire, period, period);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    private void fire() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.size() < plugin.getConfig().getInt("events.min-players", 1)) return;
        List<BossDef> pool = new ArrayList<>();
        for (String id : plugin.getConfig().getStringList("events.bosses")) {
            BossDef d = plugin.bossStore().get(id);
            if (d != null && !plugin.bosses().isAlive(id)) pool.add(d);
        }
        if (pool.isEmpty()) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Player target = online.get(r.nextInt(online.size()));
        Location l = target.getLocation().add(Fx.forward(target.getLocation()).multiply(10));
        plugin.bosses().spawn(pool.get(r.nextInt(pool.size())), l);
    }
}
