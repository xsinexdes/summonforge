package com.summonforge.gui;

import com.summonforge.data.Definition;
import com.summonforge.data.DefinitionStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Describe un tipo de contenido editable: su almacén, cómo crearlo, sus campos y su botón "Probar". */
public final class DefinitionKind<T extends Definition> {
    public final String title;
    public final String plural;
    public final Material icon;
    public final DefinitionStore<T> store;
    public final Function<String, T> factory;
    public final List<Field<T>> fields;
    public final BiConsumer<Player, T> tester;

    public DefinitionKind(String title, String plural, Material icon, DefinitionStore<T> store,
                          Function<String, T> factory, List<Field<T>> fields, BiConsumer<Player, T> tester) {
        this.title = title;
        this.plural = plural;
        this.icon = icon;
        this.store = store;
        this.factory = factory;
        this.fields = fields;
        this.tester = tester;
    }
}
