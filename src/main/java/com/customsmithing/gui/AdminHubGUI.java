package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.admin.Diagnostics;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Hub de administración: todo lo de admin en un solo menú (`/smithing admin`). */
public class AdminHubGUI extends MenuBase {

    public AdminHubGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, 4, "\u2692 Administración");
    }

    private void command(int slot, Material mat, String name, String usage, String... extra) {
        String[] lore = new String[extra.length + 2];
        System.arraycopy(extra, 0, lore, 0, extra.length);
        lore[extra.length] = Theme.muted("Comando: " + usage);
        lore[extra.length + 1] = Theme.dim("Clic para ver cómo se usa");
        button(slot, icon("menu.editor", mat, Theme.title(name), lore), e -> {
            player.closeInventory();
            player.sendMessage(Theme.accent(name) + Theme.muted(" → ") + Theme.warn(usage));
        });
    }

    @Override
    protected void render() {
        clear();
        // Contenido
        command(10, Material.CRAFTING_TABLE, "Recetas", "/smithing recipe new <id>", Theme.muted("Crear o editar recetas"));
        command(11, Material.SMOKER, "Aleaciones", "/smithing alloy new <id>", Theme.muted("Crear o editar aleaciones"));
        command(12, Material.NETHERITE_INGOT, "Minerales custom", "/smithing mineral new <id> <nombre>  ·  edit <id>", Theme.muted("Se editan en un menú"));
        command(13, Material.WITHER_SKELETON_SKULL, "Jefes", "/smithing boss new <id>", Theme.muted("Crear o editar jefes"));
        button(14, icon("menu.recetas", Material.FILLED_MAP, Theme.title("Árbol de recetas"),
                Theme.muted("Enlazar prerequisitos (Shift+clic padre)")), e -> new RecipeTreeGUI(plugin, player).open(player));
        button(15, icon("menu.planos", Material.WRITABLE_BOOK, Theme.title("Misiones"),
                Theme.muted("Ver misiones y su progreso")), e -> new QuestGUI(plugin, player).open(player));
        button(16, icon("menu.estaciones", Material.SNOWBALL, Theme.title("Estaciones del año"),
                Theme.muted("Temperatura por temporada")), e -> new SeasonConfigGUI(plugin, player).open(player));

        // Herramientas
        button(19, icon("menu.editor", Material.ANVIL, Theme.title("Banco de pruebas"),
                Theme.muted("Kits, estaciones en fila y recetas de prueba")), e -> new TestBenchGUI(plugin, player).open(player));
        button(20, icon("menu.editor", Material.WOODEN_PICKAXE, Theme.title("Catálogo de moldes"),
                Theme.muted("Pedir moldes (gratis para admins)")), e -> new MoldCatalogGUI(plugin, player).open(player));
        button(21, icon("menu.editor", Material.COMPARATOR, Theme.title("Validar datos"),
                Theme.muted("Recetas, árbol y materiales")), e -> {
            player.closeInventory();
            new Diagnostics(plugin).validate().forEach(player::sendMessage);
        });
        button(22, icon("menu.editor", Material.REDSTONE_TORCH, Theme.title("Autoprueba"),
                Theme.muted("Recorre el circuito de cada material")), e -> {
            player.closeInventory();
            new Diagnostics(plugin).selftest().forEach(player::sendMessage);
        });
        button(23, icon("menu.editor", Material.REPEATER, Theme.title("Recargar"),
                Theme.muted("Config, mensajes, skins y datos")), e -> {
            plugin.reloadAll();
            Messages.send(player, "general.recargado");
        });

        // Estado
        button(31, icon("menu.estaciones", Material.BOOK, Theme.title("Estado"),
                Theme.field("Recetas", String.valueOf(plugin.getRecipeManager().getAll().size())),
                Theme.field("Materiales", String.valueOf(plugin.getMaterialRegistry().entries().size())),
                Theme.field("Aleaciones", String.valueOf(plugin.getAlloyManager().getAll().size())),
                Theme.field("Minerales custom", String.valueOf(plugin.getCustomMineralManager().getAll().size())),
                Theme.field("Skins", plugin.getSkinService().isEnabled() ? "activadas" : "desactivadas (iconos vanilla)")), e -> { });
        fillEmpty();
    }
}
