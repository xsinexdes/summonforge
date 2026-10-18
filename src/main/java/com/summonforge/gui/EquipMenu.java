package com.summonforge.gui;

import com.summonforge.ability.AbilityDef;
import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.summon.SummonDef;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** El jugador elige cuáles de las habilidades de su invocación equipa (el orden = slot de lanzamiento). */
public final class EquipMenu extends AbstractMenu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public EquipMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Equipar habilidades", 5);
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    @Override
    protected void build() {
        for (int i = 0; i < 45; i++) set(i, filler(), null);
        PlayerData pd = plugin.players().get(viewer.getUniqueId());
        SummonDef def = plugin.summonStore().get(pd.summonId);
        if (def == null) {
            set(22, Items.icon(Material.BARRIER, "<red>No tienes invocación"), null);
            set(40, Items.icon(Material.ARROW, "<yellow>Volver"), (p, t) -> new MySummonMenu(plugin, p).open());
            return;
        }
        int max = plugin.getConfig().getInt("summon.max-equipped", 3);
        List<String> load = plugin.summons().loadout(viewer.getUniqueId(), def);
        set(4, Items.icon(Material.BOOK, "<yellow>Equipadas: <white>" + load.size() + "/" + max,
                "<gray>Click en una habilidad para equiparla o quitarla."), null);
        for (int i = 0; i < def.abilities.size() && i < SLOTS.length; i++) {
            AbilityDef a = plugin.abilityStore().get(def.abilities.get(i));
            if (a == null) continue;
            boolean on = load.contains(a.id);
            ItemStack s = Items.icon(a.icon, a.name, "<gray>" + Text.escape(a.description),
                    on ? "<green>✔ Equipada (slot " + (load.indexOf(a.id) + 1) + ")" : "<gray>✘ No equipada",
                    "<yellow>Click: alternar");
            set(SLOTS[i], on ? Items.glow(s) : s, (p, t) -> toggle(pd, def, a.id, max));
        }
        set(40, Items.icon(Material.ARROW, "<yellow>Volver"), (p, t) -> new MySummonMenu(plugin, p).open());
    }

    private void toggle(PlayerData pd, SummonDef def, String id, int max) {
        List<String> cur = plugin.summons().loadout(pd.uuid, def);
        pd.equipped.clear();
        pd.equipped.addAll(cur);
        if (!pd.equipped.remove(id)) {
            if (pd.equipped.size() >= max) {
                plugin.messages().send(viewer, "equip-full", "n", String.valueOf(max));
                refresh();
                return;
            }
            pd.equipped.add(id);
        }
        plugin.players().save(pd);
        refresh();
    }
}
