package com.summonforge.model;

import com.summonforge.core.SummonForge;
import com.summonforge.util.Fx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.logging.Level;

/** Apariencia de una entidad: modelo BetterModel con animaciones por estado, o fallback de partículas. */
public final class EntityVisual {
    private final SummonForge plugin;
    private final LivingEntity entity;
    private final AnimationSet anims;
    private final ModelHandle handle;
    private final String particle;
    private final boolean hideBase;
    private String current = "";
    private long holdUntil;
    private Location last;

    private EntityVisual(SummonForge plugin, LivingEntity entity, AnimationSet anims, ModelHandle handle, String particle, boolean hideBase) {
        this.hideBase = hideBase;
        this.plugin = plugin;
        this.entity = entity;
        this.anims = anims;
        this.handle = handle;
        this.particle = particle;
    }

    public static EntityVisual create(SummonForge plugin, LivingEntity e, String modelId, AnimationSet anims, String particle, boolean hideBase) {
        ModelHandle h = null;
        if (modelId != null && !modelId.isBlank() && plugin.model().available()) {
            try {
                h = plugin.model().attach(e, modelId);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Fallo al adjuntar el modelo '" + modelId + "'", t);
            }
            if (h == null) plugin.getLogger().warning("Modelo no encontrado en BetterModel: " + modelId + " (se usa fallback)");
        }
        if (h != null && hideBase) e.setInvisible(true);
        EntityVisual v = new EntityVisual(plugin, e, anims, h, particle, hideBase);
        v.play("spawn", 1200);
        return v;
    }

    public LivingEntity entity() {
        return entity;
    }

    public boolean hasModel() {
        return handle != null && !handle.isClosed();
    }

    /** Reproduce la animación del estado y evita que el ciclo idle/walk la pise durante holdMs. */
    public void play(String state, long holdMs) {
        if (!hasModel()) return;
        String a = anims.get(state);
        if (a.isEmpty()) return;
        try {
            handle.play(a);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Animación fallida: " + a, t);
            return;
        }
        current = state;
        holdUntil = System.currentTimeMillis() + holdMs;
    }

    public void tick() {
        if (!entity.isValid()) return;
        if (hasModel()) {
            Location now = entity.getLocation();
            boolean moving = last != null && last.getWorld() == now.getWorld() && last.distanceSquared(now) > 0.01;
            last = now;
            if (System.currentTimeMillis() >= holdUntil) {
                String st = moving ? "walk" : "idle";
                if (!st.equals(current)) play(st, 0);
            }
        } else {
            if (hideBase && entity.isInvisible()) entity.setInvisible(false);
            Fx.spawn(entity.getWorld(), particle, entity.getLocation().add(0, entity.getHeight() / 2, 0), 2, 0.3, 0.4, 0.3, 0.01);
        }
    }

    /** Reproduce una animación por nombre real (vista previa). */
    public void playRaw(String animation) {
        if (!hasModel() || animation == null || animation.isEmpty()) return;
        try {
            handle.play(animation);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Animación fallida: " + animation, t);
        }
    }

    public void playDeathAndClose() {
        play("death", 5000);
        if (handle != null) Bukkit.getScheduler().runTaskLater(plugin, this::close, 40L);
    }

    public void close() {
        if (handle == null) return;
        try {
            handle.close();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "Error al cerrar el modelo", t);
        }
    }
}
