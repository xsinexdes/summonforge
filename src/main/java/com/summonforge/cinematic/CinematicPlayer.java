package com.summonforge.cinematic;

import com.summonforge.core.SummonForge;
import com.summonforge.util.Fx;
import com.summonforge.util.Potions;
import com.summonforge.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Reproduce una cinemática paso a paso para un grupo de jugadores. */
public final class CinematicPlayer {
    private final SummonForge plugin;

    public CinematicPlayer(SummonForge plugin) {
        this.plugin = plugin;
    }

    public boolean play(String id, Collection<? extends Player> viewers, Location focus) {
        CinematicDef def = plugin.cinematicStore().get(id);
        if (def == null || viewers.isEmpty()) return false;
        step(def, 0, new ArrayList<>(viewers), focus);
        return true;
    }

    private void step(CinematicDef def, int index, List<Player> viewers, Location focus) {
        if (index >= def.steps.size()) return;
        String[] p = def.steps.get(index).split(";", -1);
        String type = p[0].trim().toUpperCase();
        long delay = 0;
        switch (type) {
            case "TITLE" -> {
                long ticks = p.length > 3 ? parse(p[3], 40) : 40;
                Title t = Title.title(Text.mm(arg(p, 1)), Text.mm(arg(p, 2)),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(ticks * 50L), Duration.ofMillis(300)));
                for (Player v : viewers) if (v.isOnline()) v.showTitle(t);
                delay = ticks;
            }
            case "SOUND" -> {
                for (Player v : viewers) if (v.isOnline()) v.playSound(v.getLocation(), arg(p, 1), 1f, 1f);
            }
            case "MESSAGE" -> {
                for (Player v : viewers) if (v.isOnline()) v.sendMessage(Text.mm(arg(p, 1)));
            }
            case "WAIT" -> delay = parse(arg(p, 1), 20);
            case "FREEZE" -> {
                int ticks = (int) parse(arg(p, 1), 40);
                for (Player v : viewers) if (v.isOnline()) Potions.add(v, "slowness", ticks, 10);
            }
            case "PARTICLES" -> {
                if (focus != null && focus.getWorld() != null) {
                    Fx.spawn(focus.getWorld(), arg(p, 1), focus.clone().add(0, 1, 0), (int) parse(arg(p, 2), 20), 1, 1, 1, 0.05);
                }
            }
            default -> plugin.getLogger().warning("Paso de cinemática desconocido en " + def.id + ": " + type);
        }
        if (delay <= 0) {
            step(def, index + 1, viewers, focus);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> step(def, index + 1, viewers, focus), delay);
        }
    }

    private static String arg(String[] p, int i) {
        return i < p.length ? p[i] : "";
    }

    private static long parse(String s, long def) {
        try {
            return Math.max(0, Long.parseLong(s.trim()));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
