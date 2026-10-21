package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Menú de 6 filas con paginación: 45 entradas por página y una fila inferior de navegación.
 * Las subclases solo dicen qué entradas hay y cómo se dibuja cada una.
 */
public abstract class PagedMenu<T> extends MenuBase {

    protected static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INDICATOR_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private int page = 0;

    protected PagedMenu(CustomSmithing plugin, Player player, String title) {
        super(plugin, player, 6, title);
    }

    protected abstract List<T> entries();

    protected abstract ItemStack entryIcon(T entry);

    /** Texto que se muestra cuando no hay ninguna entrada. */
    protected abstract String emptyText();

    protected void onEntryClick(T entry, InventoryClickEvent event) {
    }

    /** Para añadir botones propios en la fila inferior (slots 46-48 y 50-52). */
    protected void decorate() {
    }

    @Override
    protected final void render() {
        clear();
        List<T> all = entries();
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));

        int from = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && from + i < all.size(); i++) {
            T entry = all.get(from + i);
            button(i, entryIcon(entry), event -> onEntryClick(entry, event));
        }
        if (all.isEmpty()) {
            set(22, icon("nav.pagina", Material.PAPER, Theme.muted(emptyText())));
        }

        if (page > 0) {
            button(PREV_SLOT, icon("nav.anterior", Material.ARROW, Theme.accent("Página anterior")), event -> {
                page--;
                render();
            });
        }
        if (page < pages - 1) {
            button(NEXT_SLOT, icon("nav.siguiente", Material.ARROW, Theme.accent("Página siguiente")), event -> {
                page++;
                render();
            });
        }
        set(INDICATOR_SLOT, icon("nav.pagina", Material.PAPER,
                Theme.muted("Página " + (page + 1) + " de " + pages),
                Theme.dim(all.size() + " en total")));
        decorate();
    }
}
