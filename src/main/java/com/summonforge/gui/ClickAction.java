package com.summonforge.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

@FunctionalInterface
public interface ClickAction {
    void run(Player player, ClickType click);
}
