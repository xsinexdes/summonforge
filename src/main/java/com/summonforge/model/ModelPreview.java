package com.summonforge.model;

import com.summonforge.core.Keys;
import com.summonforge.core.SummonForge;
import com.summonforge.util.Fx;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/** Muestra un modelo 15 segundos frente al jugador, recorriendo todas sus animaciones. */
public final class ModelPreview {
    private ModelPreview() {}

    public static void show(SummonForge plugin, Player p, String modelId) {
        if (!plugin.model().available()) {
            plugin.messages().send(p, "model-unavailable");
            return;
        }
        Location l = p.getLocation().add(Fx.forward(p.getLocation()).multiply(4));
        Entity ent = l.getWorld().spawnEntity(l, EntityType.HUSK);
        if (!(ent instanceof Mob m)) {
            ent.remove();
            return;
        }
        m.setAI(false);
        m.setPersistent(false);
        m.setSilent(true);
        m.setInvulnerable(true);
        m.getPersistentDataContainer().set(Keys.DUMMY, PersistentDataType.BYTE, (byte) 1);
        List<String> anims = plugin.model().animations(modelId);
        AnimationSet set = new AnimationSet();
        set.autoFill(anims);
        EntityVisual v = EntityVisual.create(plugin, m, modelId, set, "FLAME", false);
        plugin.messages().send(p, "preview-started", "model", modelId, "n", String.valueOf(anims.size()));
        new BukkitRunnable() {
            int t = 0;
            int i = 0;

            @Override
            public void run() {
                if (t >= 300 || !m.isValid()) {
                    v.close();
                    m.remove();
                    cancel();
                    return;
                }
                if (!anims.isEmpty() && t % 40 == 0) v.playRaw(anims.get(i++ % anims.size()));
                t += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
