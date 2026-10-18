package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cancela todo movimiento de ítems en nuestros menús, valida permiso y limita el spam de clics. */
public final class MenuManager implements Listener {
    private final SummonForge plugin;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public MenuManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof AbstractMenu menu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != top) return;
        long now = System.currentTimeMillis();
        Long last = lastClick.get(p.getUniqueId());
        if (last != null && now - last < 100) return;
        lastClick.put(p.getUniqueId(), now);
        if (!p.hasPermission(menu.permission())) {
            p.closeInventory();
            plugin.messages().send(p, "no-permission");
            return;
        }
        int slot = e.getSlot();
        var click = e.getClick();
        Bukkit.getScheduler().runTask(plugin, () -> menu.click(slot, click));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof AbstractMenu) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastClick.remove(e.getPlayer().getUniqueId());
    }
}
