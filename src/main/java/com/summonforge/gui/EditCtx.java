package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.Definition;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/** Contexto que reciben las acciones de los campos. */
public final class EditCtx<T extends Definition> {
    public final SummonForge plugin;
    public final Player player;
    public final DefinitionKind<T> kind;
    public final T def;
    private final EditorMenu<T> menu;

    EditCtx(SummonForge plugin, Player player, DefinitionKind<T> kind, T def, EditorMenu<T> menu) {
        this.plugin = plugin;
        this.player = player;
        this.kind = kind;
        this.def = def;
        this.menu = menu;
    }

    /** Guarda (atómico, con versión) y repinta. */
    public void commit() {
        kind.store.save(def);
        menu.refresh();
    }

    public void reopen() {
        menu.open();
    }

    public void open(AbstractMenu other) {
        other.open();
    }

    public void prompt(String what, Consumer<String> onInput) {
        plugin.prompts().ask(player, what, in -> {
            onInput.accept(in);
            menu.open();
        }, menu::open);
    }
}
