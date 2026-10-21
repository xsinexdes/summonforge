package com.summonforge.api;

import com.summonforge.summon.SummonDef;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se lanza antes de aparecer la invocación de un jugador (también las de prueba). Cancelable. */
public final class SummonSpawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player owner;
    private final SummonDef def;
    private final boolean preview;
    private boolean cancelled;

    public SummonSpawnEvent(Player owner, SummonDef def, boolean preview) {
        this.owner = owner;
        this.def = def;
        this.preview = preview;
    }

    public Player getOwner() { return owner; }
    public SummonDef getDefinition() { return def; }
    public boolean isPreview() { return preview; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
