package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.Definition;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Versiones anteriores de una definición (se guardan solas en cada cambio, máx. 10). */
public final class VersionsMenu<T extends Definition> extends PagedMenu<File> {
    private final DefinitionKind<T> kind;
    private final T def;

    public VersionsMenu(SummonForge plugin, Player viewer, DefinitionKind<T> kind, T def) {
        super(plugin, viewer, "Versiones de " + def.id());
        this.kind = kind;
        this.def = def;
    }

    @Override
    protected List<File> entries() {
        return kind.store.versions(def.id());
    }

    private static String when(File f) {
        String n = f.getName();
        try {
            long ms = Long.parseLong(n.substring(n.lastIndexOf('-') + 1, n.length() - 4));
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(ms));
        } catch (RuntimeException e) {
            return n;
        }
    }

    @Override
    protected String label(File f) {
        return when(f);
    }

    @Override
    protected ItemStack render(File f) {
        return Items.icon(Material.CLOCK, "<yellow>" + when(f), "<gray>" + f.getName(), "", "<yellow>Click: restaurar esta versión");
    }

    @Override
    protected void picked(File f, ClickType click) {
        new Pickers.Confirm(plugin, viewer, "Restaurar versión", "<gray>" + when(f) + "\n<gray>La actual se guarda como versión.", () -> {
            T restored = kind.store.restore(def.id(), f);
            new EditorMenu<>(plugin, viewer, kind, restored).open();
        }, this::open).open();
    }

    @Override
    protected void back() {
        T cur = kind.store.get(def.id());
        new EditorMenu<>(plugin, viewer, kind, cur != null ? cur : def).open();
    }
}
