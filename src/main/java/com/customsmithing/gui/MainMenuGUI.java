package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MainMenuGUI extends MenuBase {

    private static final String ADMIN_PERMISSION = "customsmithing.admin";

    public MainMenuGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, 3, "\u2692 Herrería RPG");
    }

    @Override
    protected void render() {
        clear();
        button(10, icon("menu.habilidades", Material.EXPERIENCE_BOTTLE, Theme.accent("Árbol de Habilidades"),
                        Theme.muted("Ver tu nivel, XP y elegir especialización")),
                e -> new SkillTreeGUI(plugin, player).open(player));
        button(11, icon("menu.recetas", Material.FILLED_MAP, "\u00a72Árbol de Recetas",
                        Theme.muted("Ve qué recetas desbloquean otras")),
                e -> new RecipeTreeGUI(plugin, player).open(player));
        button(12, icon("menu.ensamblaje", Material.SMITHING_TABLE, Theme.ok("Mesa de Ensamblaje"),
                        Theme.muted("Fabrica objetos con tus materiales procesados")),
                e -> new AssemblyGUI(plugin, player).open(player));
        button(14, icon("menu.planos", Material.WRITTEN_BOOK, Theme.magic("Mesa de Planos"),
                        Theme.muted("Consulta tus investigaciones")),
                e -> new BlueprintTableGUI(plugin, player).open(player));

        button(15, icon("menu.planos", Material.WRITABLE_BOOK, Theme.warn("Misiones"),
                        Theme.muted("Consulta tu progreso y premios")),
                e -> new QuestGUI(plugin, player).open(player));

        button(13, icon("menu.estaciones", Material.ANVIL, Theme.accent("Mejoras de Estación"),
                        Theme.muted("Mejora tus estaciones para más calidad")),
                e -> new StationsGUI(plugin, player).open(player));

        if (player.hasPermission(ADMIN_PERMISSION)) {
            button(16, icon("menu.editor", Material.CRAFTING_TABLE, Theme.title("Editor de Recetas (Admin)"),
                            Theme.muted("Crea recetas nuevas desde el juego"),
                            Theme.muted("Usa: /smithing recipe new <id>")),
                    e -> {
                        player.closeInventory();
                        player.sendMessage(Theme.muted("Usa: /smithing recipe new <id>"));
                    });
            button(26, icon("menu.editor", Material.COMMAND_BLOCK, Theme.title("Administración"),
                            Theme.muted("Hub de admin, banco de pruebas y diagnóstico")),
                    e -> new AdminHubGUI(plugin, player).open(player));
            button(22, icon("menu.estaciones", Material.SNOWBALL, Theme.accent("Estaciones del Año (Admin)"),
                            Theme.muted("Configura temperatura por temporada")),
                    e -> new SeasonConfigGUI(plugin, player).open(player));
        }
    }
}
