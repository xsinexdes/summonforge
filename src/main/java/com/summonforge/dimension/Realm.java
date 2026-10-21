package com.summonforge.dimension;

import org.bukkit.Material;

/** Dimensiones vacías propias. LIMBO: Kamui. ILLUSION: Tsukuyomi/genjutsu. ARENA: duelos y dominios. */
public enum Realm {
    LIMBO("sf_limbo", Material.OBSIDIAN),
    ILLUSION("sf_illusion", Material.RED_CONCRETE),
    ARENA("sf_arena", Material.POLISHED_BLACKSTONE);

    public final String world;
    public final Material floor;

    Realm(String world, Material floor) {
        this.world = world;
        this.floor = floor;
    }
}
