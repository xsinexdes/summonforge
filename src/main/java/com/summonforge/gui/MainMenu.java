package com.summonforge.gui;

import com.summonforge.content.Examples;
import com.summonforge.core.SummonForge;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

/** Menú Maestro (admin). */
public final class MainMenu extends AbstractMenu {
    public MainMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "<gradient:#8e44ad:#3498db>SummonForge</gradient>", 4);
    }

    @Override
    protected void build() {
        for (int i = 0; i < 36; i++) set(i, filler(), null);
        Kinds k = plugin.kinds();
        set(10, Items.icon(k.summon.icon, "<light_purple>Invocaciones", "<gray>Definidas: <white>" + plugin.summonStore().size()),
                (p, t) -> new ListMenu<>(plugin, p, k.summon).open());
        set(11, Items.icon(k.boss.icon, "<red>Bosses", "<gray>Definidos: <white>" + plugin.bossStore().size()),
                (p, t) -> new ListMenu<>(plugin, p, k.boss).open());
        set(12, Items.icon(k.ability.icon, "<gold>Habilidades", "<gray>Definidas: <white>" + plugin.abilityStore().size()),
                (p, t) -> new ListMenu<>(plugin, p, k.ability).open());
        set(13, Items.icon(k.item.icon, "<aqua>Objetos", "<gray>Definidos: <white>" + plugin.itemStore().size()),
                (p, t) -> new ListMenu<>(plugin, p, k.item).open());
        set(14, Items.icon(k.status.icon, "<dark_green>Estados", "<gray>Definidos: <white>" + plugin.statusStore().size()),
                (p, t) -> new ListMenu<>(plugin, p, k.status).open());
        set(15, Items.icon(Material.NETHER_STAR, "<green>Mi Invocación", "<gray>Tu invocación personal."),
                (p, t) -> new MySummonMenu(plugin, p).open());
        set(16, Items.icon(Material.ARMOR_STAND, "<white>Modelos BetterModel",
                "<gray>Detectados: <white>" + plugin.model().modelKeys().size(),
                plugin.model().available() ? "<green>BetterModel activo" : "<red>BetterModel no disponible",
                "<yellow>Click: listar en el chat"), (p, t) -> {
            p.closeInventory();
            String list = plugin.model().modelKeys().stream().limit(40).collect(Collectors.joining(", "));
            plugin.messages().send(p, "models-list", "n", String.valueOf(plugin.model().modelKeys().size()), "list", list.isEmpty() ? "-" : list);
        });
        set(24, Items.icon(k.cinematic.icon, "<yellow>Cinemáticas", "<gray>Definidas: <white>" + plugin.cinematicStore().size()),
                (p, t) -> new ListMenu<>(plugin, p, k.cinematic).open());
        set(23, Items.icon(Material.AMETHYST_SHARD, "<light_purple>Elementos", "<gray>Matriz de fortalezas y debilidades."),
                (p, t) -> new MatrixMenu(plugin, p, null).open());
        set(21, Items.icon(Material.CHEST, "<yellow>Instalar contenido de ejemplo",
                "<gray>Crea habilidades, 1 invocación,", "<gray>1 boss y 1 objeto de prueba."), (p, t) -> {
            Examples.install(plugin);
            plugin.messages().send(p, "examples-done");
            open();
        });
        set(22, Items.icon(Material.COMMAND_BLOCK, "<yellow>Recargar", "<gray>Config, mensajes y definiciones."), (p, t) -> {
            plugin.reloadAll();
            plugin.messages().send(p, "reloaded");
            open();
        });
        set(31, Items.icon(Material.BARRIER, "<red>Cerrar"), (p, t) -> p.closeInventory());
    }
}
