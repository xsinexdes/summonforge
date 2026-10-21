package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Quality;
import com.customsmithing.data.SmithingKeys;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

/**
 * Los objetos forjados con mas calidad se rompen mas lento de verdad:
 * cada vez que el juego intenta bajarle durabilidad, hay una probabilidad
 * (segun la calidad) de que el golpe/uso "no cuente" para el desgaste.
 */
public class DurabilityListener implements Listener {

    private final CustomSmithing plugin;
    private final Random random = new Random();

    public DurabilityListener(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(PlayerItemDamageEvent event) {
        if (!plugin.getConfig().getBoolean("durabilidad.activar", true)) return;
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null) return; // algunos items no tienen meta
        Quality quality = Quality.parse(meta.getPersistentDataContainer().get(SmithingKeys.QUALITY, PersistentDataType.STRING));
        if (quality == null) return;

        double negateChance = plugin.getConfig().getDouble("durabilidad.probabilidad_negar_dano_" + quality.getConfigKey(), 0);
        if (random.nextDouble() * 100 < negateChance) {
            event.setCancelled(true);
        }
    }
}
