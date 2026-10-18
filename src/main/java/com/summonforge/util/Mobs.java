package com.summonforge.util;

import org.bukkit.entity.EntityType;

import java.util.List;

/** Mobs base permitidos (no se queman al sol ni se transforman en el overworld). */
public final class Mobs {
    public static final List<EntityType> ALLOWED = List.of(
            EntityType.IRON_GOLEM, EntityType.HUSK, EntityType.VINDICATOR, EntityType.RAVAGER, EntityType.WITHER_SKELETON,
            EntityType.HORSE, EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE);

    public static final List<EntityType> HOSTILE = List.of(
            EntityType.HUSK, EntityType.VINDICATOR, EntityType.WITHER_SKELETON, EntityType.RAVAGER);

    private Mobs() {}
}
