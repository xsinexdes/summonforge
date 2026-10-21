package com.summonforge.summon;

/** Definiciones que pueden traer un escuadrón de mobs extra (invocaciones y bosses). El admin decide cuáles. */
public interface Squadded {
    boolean squadEnabled();

    void squadEnabled(boolean enabled);

    SquadEntry[] squad();
}
