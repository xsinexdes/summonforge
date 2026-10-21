package com.summonforge.gui;

import com.summonforge.data.Definition;
import org.bukkit.Material;

import java.util.function.Function;

/** Un campo editable de una definición, mostrado como botón en el EditorMenu. */
public record Field<T extends Definition>(String label, Material icon, String help, String hint,
                                          Function<T, String> value, FieldAction<T> action) {
    @FunctionalInterface
    public interface FieldAction<T extends Definition> {
        void run(EditCtx<T> ctx, org.bukkit.event.inventory.ClickType click);
    }
}
