package com.summonforge.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.entity.BaseEntity;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implementación sobre la API de BetterModel (bettermodel-bukkit-api 2.2.0).
 * Firmas tomadas de su documentación, NO verificadas con javap (ver _PROGRESO/API_NOTAS.md).
 * Solo se instancia si el plugin BetterModel está cargado.
 */
public final class BetterModelAdapter implements ModelAdapter {
    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Set<String> modelKeys() {
        return new TreeSet<>(BetterModel.modelKeys());
    }

    @Override
    public List<String> animations(String modelId) {
        return BetterModel.model(modelId)
                .map(r -> (List<String>) new ArrayList<>(r.animations().keySet()))
                .orElse(List.of());
    }

    @Override
    public ModelHandle attach(LivingEntity entity, String modelId) {
        return BetterModel.model(modelId)
                .map(r -> (ModelHandle) new Handle(r.getOrCreate(BaseEntity.of(entity))))
                .orElse(null);
    }

    private record Handle(EntityTracker tracker) implements ModelHandle {
        @Override
        public void play(String animation) {
            tracker.animate(animation);
        }

        @Override
        public void close() {
            tracker.close();
        }

        @Override
        public boolean isClosed() {
            return tracker.isClosed();
        }
    }
}
