package com.summonforge.model;

import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Set;

/** Capa de aislamiento sobre BetterModel: si cambia su API solo se toca la implementación. */
public interface ModelAdapter {
    boolean available();

    Set<String> modelKeys();

    List<String> animations(String modelId);

    /** @return handle o null si el modelo no existe. */
    ModelHandle attach(LivingEntity entity, String modelId);
}
