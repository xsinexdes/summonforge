package com.customsmithing.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Blindaje anti-robo / anti-duplicación de todos los menús del plugin.
 *
 * Arriba (el menú): todo se cancela, salvo en slots "interactivos" y solo con acciones simples.
 * Abajo (inventario del jugador): se permite agarrar items con el mouse (los editores lo usan),
 * pero se bloquea shift-clic, doble clic (recoger al cursor), clonar y clics de creativo,
 * porque podrían arrastrar items del menú o colar items dentro de él.
 * Arrastrar (drag) sobre el menú se cancela siempre.
 */
public class GUIListener implements Listener {

    /** Acciones permitidas dentro de un slot interactivo. Todo lo demás se cancela. */
    private static final Set<InventoryAction> INTERACTIVE_ALLOWED = EnumSet.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE, InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.HOTBAR_SWAP);

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof SmithingGUIHolder holder)) return;

        InventoryAction action = event.getAction();
        ClickType click = event.getClick();

        // Pueden recoger items del menú hacia el cursor (o crearlos): bloqueados siempre.
        if (action == InventoryAction.COLLECT_TO_CURSOR || action == InventoryAction.CLONE_STACK
                || click == ClickType.DOUBLE_CLICK || click == ClickType.CREATIVE) {
            event.setCancelled(true);
            return;
        }

        int raw = event.getRawSlot();
        boolean inTop = raw >= 0 && raw < top.getSize();

        if (!inTop) {
            // Inventario propio: el shift-clic mandaría el item al menú.
            if (event.isShiftClick() || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        if (holder.isInteractiveSlot(raw)) {
            if (!INTERACTIVE_ALLOWED.contains(action)) {
                event.setCancelled(true);
                return;
            }
            holder.handleClick(event);
            return;
        }

        event.setCancelled(true);
        holder.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof SmithingGUIHolder)) return;
        int topSize = top.getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SmithingGUIHolder holder
                && event.getPlayer() instanceof Player player) {
            holder.handleClose(player);
        }
    }
}
