package com.customsmithing.commands;

import com.customsmithing.CustomSmithing;
import com.customsmithing.gui.BlueprintTableGUI;
import com.customsmithing.gui.RecipeTreeGUI;
import com.customsmithing.gui.SkillTreeGUI;
import com.customsmithing.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuickGuiCommand implements CommandExecutor {

    public enum Type { SKILLS, RECIPE_TREE, BLUEPRINTS, MOLDS }

    private final CustomSmithing plugin;
    private final Type type;

    public QuickGuiCommand(CustomSmithing plugin, Type type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "general.solo_jugadores");
            return true;
        }
        if (!player.hasPermission("customsmithing.use")) {
            Messages.send(player, "general.sin_permiso");
            return true;
        }

        switch (type) {
            case SKILLS -> new SkillTreeGUI(plugin, player).open(player);
            case RECIPE_TREE -> new RecipeTreeGUI(plugin, player).open(player);
            case BLUEPRINTS -> new BlueprintTableGUI(plugin, player).open(player);
            case MOLDS -> new com.customsmithing.gui.MoldCatalogGUI(plugin, player).open(player);
        }
        return true;
    }
}
