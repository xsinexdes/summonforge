package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.stats.Element;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;
import java.util.List;

/** Matriz de elementos: primero eliges el atacante, luego ajustas su multiplicador contra cada defensor. */
public final class MatrixMenu extends AbstractMenu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
    private static final List<Element> ELEMENTS = Arrays.stream(Element.values()).filter(e -> e != Element.NONE).toList();

    private final Element attacker;

    public MatrixMenu(SummonForge plugin, Player viewer, Element attacker) {
        super(plugin, viewer, attacker == null ? "Elementos: elige atacante" : "Ataque " + attacker.label + " contra...", 4);
        this.attacker = attacker;
    }

    @Override
    protected void build() {
        for (int i = 0; i < 36; i++) set(i, filler(), null);
        for (int i = 0; i < ELEMENTS.size() && i < SLOTS.length; i++) {
            Element e = ELEMENTS.get(i);
            if (attacker == null) {
                set(SLOTS[i], Items.icon(Material.AMETHYST_SHARD, "<" + e.tag + ">" + e.label, "<yellow>Click: editar sus multiplicadores"),
                        (p, t) -> new MatrixMenu(plugin, p, e).open());
            } else {
                double v = plugin.elements().get(attacker, e);
                String col = v > 1 ? "<green>" : (v < 1 ? "<red>" : "<white>");
                set(SLOTS[i], Items.icon(Material.AMETHYST_SHARD, "<" + e.tag + ">" + e.label, "<gray>Multiplicador: " + col + "x" + v,
                        "<yellow>Izq: +0.25 · Der: -0.25 · Shift+Izq: reiniciar a 1"), (p, t) -> {
                    double cur = plugin.elements().get(attacker, e);
                    if (t == ClickType.RIGHT || t == ClickType.SHIFT_RIGHT) plugin.elements().set(attacker, e, cur - 0.25);
                    else if (t == ClickType.SHIFT_LEFT) plugin.elements().set(attacker, e, 1.0);
                    else plugin.elements().set(attacker, e, cur + 0.25);
                    refresh();
                });
            }
        }
        set(31, Items.icon(Material.BARRIER, "<red>Volver"), (p, t) -> {
            if (attacker == null) new MainMenu(plugin, p).open();
            else new MatrixMenu(plugin, p, null).open();
        });
    }
}
