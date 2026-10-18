package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.summon.KeyAction;
import com.summonforge.summon.KeyInput;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/** Teclas configurables por jugador: izq = siguiente entrada, der = anterior. */
public final class KeysMenu extends AbstractMenu {
    private static final int[] SLOTS = {10, 12, 14, 16};

    public KeysMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Teclas", 3);
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    @Override
    protected void build() {
        for (int i = 0; i < 27; i++) set(i, filler(), null);
        PlayerData pd = plugin.players().get(viewer.getUniqueId());
        KeyAction[] actions = KeyAction.values();
        for (int i = 0; i < actions.length && i < SLOTS.length; i++) {
            KeyAction a = actions[i];
            KeyInput cur = pd.keys.get(a);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Entrada: <white>" + cur.label);
            for (KeyAction other : actions) {
                if (other != a && cur != KeyInput.NONE && pd.keys.get(other) == cur) {
                    lore.add("<red>⚠ Repetida con: " + other.label + " (se usa la primera)");
                }
            }
            lore.add("");
            lore.add("<yellow>Izq: siguiente · Der: anterior");
            set(SLOTS[i], Items.icon(Material.COMPARATOR, "<yellow>" + a.label, lore), (p, t) -> {
                int dir = (t == ClickType.RIGHT || t == ClickType.SHIFT_RIGHT) ? -1 : 1;
                pd.keys.put(a, cur.next(dir));
                plugin.players().save(pd);
                refresh();
            });
        }
        set(22, Items.icon(Material.ARROW, "<yellow>Volver"), (p, t) -> new MySummonMenu(plugin, p).open());
    }
}
