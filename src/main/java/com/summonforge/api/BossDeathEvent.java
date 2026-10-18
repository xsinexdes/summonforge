package com.summonforge.api;

import com.summonforge.boss.BossDef;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;
import java.util.UUID;

/** Se lanza cuando un boss muere. winner = jugador con más daño (puede ser null). */
public final class BossDeathEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final BossDef def;
    private final Player winner;
    private final Map<UUID, Double> damage;

    public BossDeathEvent(BossDef def, Player winner, Map<UUID, Double> damage) {
        this.def = def;
        this.winner = winner;
        this.damage = damage;
    }

    public BossDef getDefinition() { return def; }
    public Player getWinner() { return winner; }
    public Map<UUID, Double> getDamage() { return damage; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
