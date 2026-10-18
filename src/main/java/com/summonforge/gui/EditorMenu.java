package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.Definition;
import com.summonforge.data.DefinitionStore;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor genérico: un botón por campo, paginado, con probar / duplicar / borrar / volver. */
public final class EditorMenu<T extends Definition> extends AbstractMenu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final DefinitionKind<T> kind;
    private final T def;
    private final EditCtx<T> ctx;
    private int page = 0;

    public EditorMenu(SummonForge plugin, Player viewer, DefinitionKind<T> kind, T def) {
        super(plugin, viewer, kind.title + ": " + def.id(), 6);
        this.kind = kind;
        this.def = def;
        this.ctx = new EditCtx<>(plugin, viewer, kind, def, this);
    }

    @Override
    protected void build() {
        for (int i = 0; i < 54; i++) set(i, filler(), null);
        List<Field<T>> fs = kind.fields;
        int pages = Math.max(1, (fs.size() + SLOTS.length - 1) / SLOTS.length);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= fs.size()) break;
            Field<T> f = fs.get(idx);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + f.help());
            lore.add("");
            lore.add("<white>Valor: <aqua>" + Text.escape(f.value().apply(def)));
            lore.add("");
            lore.add("<dark_gray>" + f.hint());
            set(SLOTS[i], Items.icon(f.icon(), "<yellow>" + f.label(), lore), (p, t) -> f.action().run(ctx, t));
        }
        set(4, Items.icon(def.icon(), def.displayName(), "<gray>ID: <white>" + def.id(), "<gray>Los cambios se guardan al instante."), null);
        if (page > 0) set(45, Items.icon(Material.ARROW, "<yellow>◀ Anterior"), (p, t) -> {
            page--;
            refresh();
        });
        if (page < pages - 1) set(53, Items.icon(Material.ARROW, "<yellow>Siguiente ▶"), (p, t) -> {
            page++;
            refresh();
        });
        set(47, Items.icon(Material.LIME_DYE, "<green>Probar", "<gray>Lo lanza/spawnea frente a ti."), (p, t) -> {
            p.closeInventory();
            kind.tester.accept(p, def);
        });
        set(48, Items.icon(Material.PAPER, "<aqua>Duplicar"), (p, t) -> duplicate());
        set(49, Items.icon(Material.BARRIER, "<red>Volver"), (p, t) -> new ListMenu<>(plugin, viewer, kind).open());
        set(51, Items.icon(Material.CLOCK, "<aqua>Versiones", "<gray>Restaura una versión anterior."),
                (p, t) -> new VersionsMenu<>(plugin, viewer, kind, def).open());
        set(50, Items.icon(Material.TNT, "<red>Borrar", "<gray>Pide confirmación."), (p, t) ->
                new Pickers.Confirm(plugin, viewer, "Borrar " + def.id(), "<red>Esta acción no se puede deshacer fácilmente.", () -> {
                    kind.store.delete(def.id());
                    plugin.messages().send(viewer, "deleted", "id", def.id());
                    new ListMenu<>(plugin, viewer, kind).open();
                }, this::open).open());
    }

    private void duplicate() {
        plugin.prompts().ask(viewer, "Escribe el ID de la copia", in -> {
            String id = in.trim().toLowerCase(Locale.ROOT);
            if (!DefinitionStore.ID.matcher(id).matches()) {
                plugin.messages().send(viewer, "invalid-id");
            } else if (kind.store.has(id)) {
                plugin.messages().send(viewer, "id-exists");
            } else {
                T copy = kind.store.copy(def, id);
                plugin.messages().send(viewer, "duplicated", "id", id);
                new EditorMenu<>(plugin, viewer, kind, copy).open();
                return;
            }
            open();
        }, this::open);
    }
}
