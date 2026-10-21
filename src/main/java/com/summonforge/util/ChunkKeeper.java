package com.summonforge.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * Mantiene cargado el chunk donde está una entidad no persistente. Una entidad con setPersistent(false) se pierde
 * si su chunk se descarga (por eso bosses/invocaciones "desaparecían"): este ticket lo evita.
 */
public final class ChunkKeeper {
    private Chunk held;

    public void hold(Plugin plugin, Location where) {
        if (where == null || where.getWorld() == null) return;
        Chunk c = where.getChunk();
        if (held != null && held.getX() == c.getX() && held.getZ() == c.getZ() && held.getWorld().equals(c.getWorld())) return;
        Chunk old = held;
        c.addPluginChunkTicket(plugin);
        held = c;
        if (old != null) old.removePluginChunkTicket(plugin);
    }

    public void release(Plugin plugin) {
        if (held != null) held.removePluginChunkTicket(plugin);
        held = null;
    }
}
