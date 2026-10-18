package com.summonforge.model;

import com.summonforge.core.SummonForge;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class VisualManager implements Listener {
    private final SummonForge plugin;
    private final Map<UUID, EntityVisual> visuals = new HashMap<>();
    private BukkitTask task;

    public VisualManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 4L, 4L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        visuals.values().forEach(EntityVisual::close);
        visuals.clear();
    }

    public EntityVisual track(LivingEntity e, String modelId, AnimationSet anims, String particle) {
        EntityVisual v = EntityVisual.create(plugin, e, modelId, anims, particle);
        visuals.put(e.getUniqueId(), v);
        return v;
    }

    public EntityVisual get(UUID entityId) {
        return visuals.get(entityId);
    }

    public void release(UUID entityId, boolean death) {
        EntityVisual v = visuals.remove(entityId);
        if (v == null) return;
        if (death) v.playDeathAndClose();
        else v.close();
    }

    private void tick() {
        Iterator<Map.Entry<UUID, EntityVisual>> it = visuals.entrySet().iterator();
        while (it.hasNext()) {
            EntityVisual v = it.next().getValue();
            if (!v.entity().isValid()) {
                v.close();
                it.remove();
            } else {
                v.tick();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        EntityVisual v = visuals.get(e.getEntity().getUniqueId());
        if (v != null) v.play("hurt", 400);
    }
}
