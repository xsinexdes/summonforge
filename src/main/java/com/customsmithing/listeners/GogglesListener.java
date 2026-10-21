package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.gui.GogglesGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GogglesListener implements Listener {

    private final CustomSmithing plugin;

    public GogglesListener(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = event.getItem();
        if (hand == null || !hand.hasItemMeta()) return;
        if (!hand.getItemMeta().getPersistentDataContainer().has(SmithingKeys.GOGGLES_ITEM, PersistentDataType.BYTE)) return;

        event.setCancelled(true);
        new GogglesGUI(plugin, event.getPlayer()).open(event.getPlayer());
    }
}
