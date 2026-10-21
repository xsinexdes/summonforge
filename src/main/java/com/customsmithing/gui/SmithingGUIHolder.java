package com.customsmithing.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Todo menú del plugin implementa esto. {@link GUIListener} cancela SIEMPRE los clics en la parte
 * de arriba del menú (y los arrastres, doble clic, etc.) y luego llama a {@link #handleClick}.
 * La única excepción son los slots devueltos por {@link #isInteractiveSlot}, donde el jugador
 * puede dejar y recoger items reales (por ejemplo, la Mesa de Encantamiento).
 */
public interface SmithingGUIHolder extends InventoryHolder {

    void handleClick(InventoryClickEvent event);

    /**
     * Se llama al cerrar el menú, pero también al desconectarse, morir, recargar el plugin o
     * apagar el servidor. DEBE ser idempotente: puede ejecutarse más de una vez para el mismo menú
     * (vacía lo que devuelves ANTES de entregarlo).
     */
    default void handleClose(Player player) {}

    default void open(Player player) {
        player.openInventory(getInventory());
    }

    /** Slots (índice del inventario de arriba) donde el jugador puede poner/sacar items reales. */
    default boolean isInteractiveSlot(int rawSlot) {
        return false;
    }
}
