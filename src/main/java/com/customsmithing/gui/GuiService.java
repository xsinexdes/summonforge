package com.customsmithing.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Cierre ordenado de menús abiertos: desconexión, muerte, recarga y apagado.
 * Importante: durante onDisable el plugin ya figura como deshabilitado y Bukkit NO entrega
 * eventos a sus listeners, así que el cierre debe llamar a {@code handleClose} directamente.
 * Como {@code handleClose} es idempotente, no pasa nada si el evento de cierre también corre.
 */
public final class GuiService {

    private GuiService() {
    }

    /** @return true si el jugador tenía un menú del plugin abierto */
    public static boolean cleanup(Player player, boolean closeWindow) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof SmithingGUIHolder holder)) return false;
        holder.handleClose(player);
        if (closeWindow) player.closeInventory();
        return true;
    }

    public static void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanup(player, true);
        }
    }
}
