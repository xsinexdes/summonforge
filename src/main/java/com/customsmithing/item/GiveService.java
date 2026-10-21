package com.customsmithing.item;

import com.customsmithing.CustomSmithing;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Única forma de entregar items a un jugador. Lo que no cabe en el inventario
 * NO se pierde: se suelta a sus pies, a su nombre unos segundos, y se le avisa.
 * Debe llamarse desde el hilo principal.
 */
public final class GiveService {

    private final CustomSmithing plugin;

    public GiveService(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    /** @return cuántos items quedaron en el suelo porque no cabían */
    public int give(Player player, ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return 0;
        return giveAll(player, java.util.List.of(item));
    }

    /** @return cuántos items quedaron en el suelo porque no cabían */
    public int giveAll(Player player, Collection<ItemStack> items) {
        int dropped = 0;
        // Un jugador muerto pierde su inventario justo después del evento de muerte:
        // lo que se le devuelva en ese momento va al suelo (sin dueño) para poder recuperarlo.
        boolean dead = player.isDead();
        for (ItemStack original : items) {
            if (original == null || original.getType().isAir() || original.getAmount() <= 0) continue;
            if (dead) {
                dropAtFeet(player, original.clone(), false);
                continue;
            }
            Map<Integer, ItemStack> leftover = new HashMap<>(player.getInventory().addItem(original.clone()));
            for (ItemStack extra : leftover.values()) {
                dropAtFeet(player, extra, true);
                dropped += extra.getAmount();
            }
        }
        if (dropped > 0) {
            Messages.send(player, "entrega.inventario_lleno", "%cantidad%", String.valueOf(dropped));
        }
        plugin.debug("Entrega a " + player.getName() + ": " + items.size() + " stack(s), " + dropped + " al suelo");
        return dropped;
    }

    private void dropAtFeet(Player player, ItemStack stack, boolean exclusive) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;
        Item dropped = loc.getWorld().dropItem(loc, stack);
        if (!exclusive) return;
        UUID owner = player.getUniqueId();
        dropped.setOwner(owner);

        long seconds = Math.max(0, plugin.getConfig().getLong("entrega.segundos_exclusivo", 15));
        // En onDisable el plugin ya figura como deshabilitado y no se pueden crear tareas.
        if (seconds > 0 && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (dropped.isValid()) dropped.setOwner(null);
            }, seconds * 20L);
        } else if (seconds == 0) {
            dropped.setOwner(null);
        }
    }
}
