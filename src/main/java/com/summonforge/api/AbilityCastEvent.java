package com.summonforge.api;

import com.summonforge.ability.AbilityDef;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se lanza antes de ejecutar una habilidad (owner puede ser null si la lanza un boss). Cancelable. */
public final class AbilityCastEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final LivingEntity caster;
    private final Player owner;
    private final AbilityDef ability;
    private boolean cancelled;

    public AbilityCastEvent(LivingEntity caster, Player owner, AbilityDef ability) {
        this.caster = caster;
        this.owner = owner;
        this.ability = ability;
    }

    public LivingEntity getCaster() { return caster; }
    public Player getOwner() { return owner; }
    public AbilityDef getAbility() { return ability; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
