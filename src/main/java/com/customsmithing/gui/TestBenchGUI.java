package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.admin.TestBench;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Banco de pruebas (admin): kits por estación, estaciones en fila y recetas de prueba. */
public class TestBenchGUI extends MenuBase {

    private final TestBench bench;

    public TestBenchGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, 4, "\u2697 Banco de Pruebas");
        this.bench = plugin.getTestBench();
    }

    private void kit(int slot, Material icon, String name, String description, TestBench.Kit kit) {
        button(slot, icon("menu.editor", icon, Theme.accent(name), Theme.muted(description), Theme.muted("Clic: recibir el kit")), e -> {
            bench.give(player, kit);
            player.sendMessage(Theme.ok("Kit entregado: ") + name);
        });
    }

    @Override
    protected void render() {
        clear();
        kit(10, Material.IRON_INGOT, "Metales", "Combustible, minerales, lingotes y piezas por etapa", TestBench.Kit.METALES);
        kit(11, Material.OAK_PLANKS, "Maderas y telas", "Para Cortapiedras y Telar", TestBench.Kit.MADERAS_TELAS);
        kit(12, Material.SMOKER, "Aleaciones", "Entrada de cada aleación (x2)", TestBench.Kit.ALEACION);
        kit(13, Material.ENCHANTED_BOOK, "Encantamiento", "Armas y libros mágicos", TestBench.Kit.ENCANTAMIENTO);
        kit(14, Material.PAPER, "Planos", "Hasta 5 planos de investigación", TestBench.Kit.PLANOS);
        kit(15, Material.WOODEN_PICKAXE, "Moldes", "Uno de cada forma", TestBench.Kit.MOLDES);
        kit(16, Material.GRINDSTONE, "Refinado", "Dos items con calidad para refinar", TestBench.Kit.REFINADO);

        button(20, icon("menu.estaciones", Material.LECTERN, Theme.title("Colocar estaciones"),
                Theme.muted("Pone todas las estaciones en fila"), Theme.muted("delante de ti (solo sobre aire).")), e -> {
            player.closeInventory();
            player.sendMessage(Theme.ok("Estaciones colocadas: ") + bench.placeStations(player));
        });
        button(21, icon("menu.estaciones", Material.BARRIER, Theme.bad("Quitar estaciones"),
                Theme.muted("Retira las que colocaste.")), e -> {
            player.closeInventory();
            player.sendMessage(Theme.ok("Estaciones quitadas: ") + bench.undoStations(player));
        });
        button(23, icon("menu.recetas", Material.WRITABLE_BOOK, Theme.title("Instalar recetas de prueba"),
                Theme.muted("Una por cada metal y mineral (prueba_*)."), Theme.muted("Solo los admins las ven.")), e -> {
            int count = bench.installTestRecipes(player);
            player.sendMessage(Theme.ok("Recetas de prueba instaladas y desbloqueadas: ") + count);
        });
        button(24, icon("menu.recetas", Material.TNT, Theme.bad("Quitar recetas de prueba"),
                Theme.muted("Borra todas las recetas prueba_*.")), e -> {
            player.sendMessage(Theme.ok("Recetas de prueba borradas: ") + bench.removeTestRecipes());
        });
        button(31, icon("menu.editor", Material.CHEST, Theme.title("Kit completo"),
                Theme.muted("Todos los kits a la vez")), e -> {
            bench.giveAll(player);
            player.sendMessage(Theme.ok("Todos los kits entregados."));
        });
        fillEmpty();
    }
}
