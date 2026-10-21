package com.customsmithing.item;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.data.ToolType;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Theme;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Tenazas y martillo con usos limitados. Solo se exigen si herramientas.activar está en true. */
public final class Tools {

    private Tools() {
    }

    public static boolean enabled(CustomSmithing plugin) {
        return plugin.getConfig().getBoolean("herramientas.activar", false);
    }

    public static int defaultUses(CustomSmithing plugin) {
        return Math.max(1, plugin.getConfig().getInt("herramientas.usos", 60));
    }

    public static ItemStack create(CustomSmithing plugin, ToolType type, int uses) {
        ItemStack tool = new ItemStack(type.getIcon());
        ItemMeta meta = tool.getItemMeta();
        meta.setDisplayName(Theme.warn(type.getLabel()));
        meta.setLore(List.of(
                Theme.muted("Hace falta tenerla en el inventario para"),
                Theme.muted("trabajar en " + type.getUsedAt() + "."),
                " ",
                Theme.field("Usos restantes", String.valueOf(uses))));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SmithingKeys.TOOL_TYPE, PersistentDataType.STRING, type.name());
        pdc.set(SmithingKeys.TOOL_USES, PersistentDataType.INTEGER, uses);
        tool.setItemMeta(meta);
        return tool;
    }

    public static ToolType typeOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || stack.getItemMeta() == null) return null;
        return ToolType.parse(stack.getItemMeta().getPersistentDataContainer().get(SmithingKeys.TOOL_TYPE, PersistentDataType.STRING));
    }

    private static int findSlot(Player player, ToolType type) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (typeOf(contents[i]) == type) return i;
        }
        return -1;
    }

    /** ¿Puede trabajar? Si las herramientas están desactivadas, siempre. Si falta, avisa al jugador. */
    public static boolean available(CustomSmithing plugin, Player player, ToolType type) {
        if (!enabled(plugin) || findSlot(player, type) >= 0) return true;
        Messages.send(player, "herramientas.falta", "%herramienta%", type.getLabel(), "%estacion%", type.getUsedAt());
        return false;
    }

    /** Gasta un uso (y la rompe al llegar a 0). Se llama justo antes de empezar el trabajo, ya validado todo lo demás. */
    public static void wear(CustomSmithing plugin, Player player, ToolType type) {
        if (!enabled(plugin)) return;
        int slot = findSlot(player, type);
        if (slot < 0) return;
        ItemStack tool = player.getInventory().getItem(slot);
        ItemMeta meta = tool.getItemMeta();
        int left = meta.getPersistentDataContainer().getOrDefault(SmithingKeys.TOOL_USES, PersistentDataType.INTEGER, defaultUses(plugin)) - 1;
        if (left <= 0) {
            player.getInventory().setItem(slot, null);
            Messages.send(player, "herramientas.rota", "%herramienta%", type.getLabel());
        } else {
            player.getInventory().setItem(slot, create(plugin, type, left));
        }
    }
}
