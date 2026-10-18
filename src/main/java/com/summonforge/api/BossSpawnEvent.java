package com.summonforge.api;

import com.summonforge.boss.BossDef;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se lanza antes de que aparezca un boss. Cancelable. */
public final class BossSpawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final BossDef def;
    private final Location location;
    private boolean cancelled;

    public BossSpawnEvent(BossDef def, Location location) {
        this.def = def;
        this.location = location;
    }

    public BossDef getDefinition() { return def; }
    public Location getLocation() { return location; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
