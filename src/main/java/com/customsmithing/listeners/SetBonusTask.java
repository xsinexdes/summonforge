package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Quality;
import com.customsmithing.data.SmithingKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Si el jugador tiene puestas las 4 piezas de armadura y TODAS son objetos
 * forjados con este plugin (casco+peto+pantalon+botas), recibe un bonus
 * extra mientras las tenga puestas. El bonus escala con la calidad
 * promedio del set.
 */
public class SetBonusTask {

    private final CustomSmithing plugin;
    private int taskId = -1;

    public SetBonusTask(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 100L, 100L);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("set_bonus.activar", true)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inv = player.getInventory();
            ItemStack[] armor = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};

            boolean fullCustomSet = true;
            double qualitySum = 0;
            for (ItemStack piece : armor) {
                if (piece == null || !piece.hasItemMeta()) { fullCustomSet = false; break; }
                String q = piece.getItemMeta().getPersistentDataContainer().get(SmithingKeys.QUALITY, PersistentDataType.STRING);
                if (q == null) { fullCustomSet = false; break; }
                Quality parsed = Quality.parse(q);
                if (parsed == null) { fullCustomSet = false; break; }
                qualitySum += parsed.ordinal();
            }

            if (!fullCustomSet) continue;

            double avgQualityTier = qualitySum / 4.0;
            int amplifier = (int) Math.round(avgQualityTier / 1.5);
            int regenAmp = Math.max(0, Math.min(2, amplifier));

            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 130, regenAmp, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 130, 0, true, false, false));
        }
    }
}
