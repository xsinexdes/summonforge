package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** Lista paginada con buscador (45 entradas por página, fila inferior de navegación). */
public abstract class PagedMenu<E> extends AbstractMenu {
    protected int page = 0;
    protected String filter = "";

    protected PagedMenu(SummonForge plugin, Player viewer, String title) {
        super(plugin, viewer, title, 6);
    }

    protected abstract List<E> entries();

    protected abstract ItemStack render(E e);

    protected abstract String label(E e);

    protected abstract void picked(E e, ClickType click);

    protected abstract void back();

    protected void extra() {}

    @Override
    protected void build() {
        List<E> all = entries().stream()
                .filter(e -> filter.isEmpty() || label(e).toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        int pages = Math.max(1, (all.size() + 44) / 45);
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= all.size()) break;
            E e = all.get(idx);
            set(i, render(e), (p, t) -> picked(e, t));
        }
        for (int i = 45; i < 54; i++) set(i, filler(), null);
        if (page > 0) set(45, Items.icon(Material.ARROW, "<yellow>◀ Anterior"), (p, t) -> {
            page--;
            refresh();
        });
        if (page < pages - 1) set(53, Items.icon(Material.ARROW, "<yellow>Siguiente ▶"), (p, t) -> {
            page++;
            refresh();
        });
        set(46, Items.icon(Material.PAPER, "<gray>Página <white>" + (page + 1) + "/" + pages,
                "<gray>Resultados: <white>" + all.size()), null);
        set(47, Items.icon(Material.COMPASS, "<aqua>Buscar",
                "<gray>Filtro: <white>" + (filter.isEmpty() ? "(ninguno)" : filter),
                "<yellow>Izq: escribir  <yellow>Der: limpiar"), (p, t) -> {
            if (t == ClickType.RIGHT || t == ClickType.SHIFT_RIGHT) {
                filter = "";
                page = 0;
                refresh();
            } else {
                plugin.prompts().ask(p, "¿Qué quieres buscar?", in -> {
                    filter = in.toLowerCase(Locale.ROOT);
                    page = 0;
                    open();
                }, this::open);
            }
        });
        set(49, Items.icon(Material.BARRIER, "<red>Volver"), (p, t) -> back());
        extra();
    }
}
