package com.summonforge.model;

import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Set;

/** Modo sin BetterModel: las entidades usan el fallback de partículas. */
public final class NullModelAdapter implements ModelAdapter {
    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Set<String> modelKeys() {
        return Set.of();
    }

    @Override
    public List<String> animations(String modelId) {
        return List.of();
    }

    @Override
    public ModelHandle attach(LivingEntity entity, String modelId) {
        return null;
    }
}
