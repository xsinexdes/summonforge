package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.Definition;
import com.summonforge.data.DefinitionStore;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ListMenu<T extends Definition> extends PagedMenu<T> {
    private final DefinitionKind<T> kind;

    public ListMenu(SummonForge plugin, Player viewer, DefinitionKind<T> kind) {
        super(plugin, viewer, kind.plural);
        this.kind = kind;
    }

    @Override
    protected List<T> entries() {
        List<T> l = new ArrayList<>(kind.store.all());
        l.sort(Comparator.comparing(Definition::id));
        return l;
    }

    @Override
    protected String label(T d) {
        return d.id() + " " + Text.plain(Text.mm(d.displayName()));
    }

    @Override
    protected ItemStack render(T d) {
        return Items.icon(d.icon(), d.displayName(), "<gray>ID: <white>" + d.id(), "",
                "<yellow>Izq: <white>editar", "<yellow>Der: <white>duplicar", "<red>Shift + Der: borrar");
    }

    @Override
    protected void picked(T d, ClickType click) {
        switch (click) {
            case RIGHT -> duplicate(d);
            case SHIFT_RIGHT -> new Pickers.Confirm(plugin, viewer, "Borrar " + d.id(), "<red>Se borrará la definición.", () -> {
                kind.store.delete(d.id());
                plugin.messages().send(viewer, "deleted", "id", d.id());
                open();
            }, this::open).open();
            default -> new EditorMenu<>(plugin, viewer, kind, d).open();
        }
    }

    @Override
    protected void back() {
        new MainMenu(plugin, viewer).open();
    }

    @Override
    protected void extra() {
        set(50, Items.icon(Material.EMERALD, "<green>+ Crear nuevo"), (p, t) ->
                plugin.prompts().ask(p, "Escribe el ID del nuevo elemento (a-z, 0-9, _)", in -> {
                    String id = in.trim().toLowerCase(Locale.ROOT);
                    if (!DefinitionStore.ID.matcher(id).matches()) {
                        plugin.messages().send(p, "invalid-id");
                        open();
                    } else if (kind.store.has(id)) {
                        plugin.messages().send(p, "id-exists");
                        open();
                    } else {
                        T d = kind.factory.apply(id);
                        kind.store.save(d);
                        plugin.messages().send(p, "created", "id", id);
                        new EditorMenu<>(plugin, p, kind, d).open();
                    }
                }, this::open));
    }

    private void duplicate(T d) {
        plugin.prompts().ask(viewer, "Escribe el ID de la copia", in -> {
            String id = in.trim().toLowerCase(Locale.ROOT);
            if (!DefinitionStore.ID.matcher(id).matches()) plugin.messages().send(viewer, "invalid-id");
            else if (kind.store.has(id)) plugin.messages().send(viewer, "id-exists");
            else {
                kind.store.copy(d, id);
                plugin.messages().send(viewer, "duplicated", "id", id);
            }
            open();
        }, this::open);
    }
}
