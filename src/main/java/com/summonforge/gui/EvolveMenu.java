package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.summon.SummonDef;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Ramas de evolución de la invocación actual. */
public final class EvolveMenu extends AbstractMenu {
    public EvolveMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Evolución", 3);
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    @Override
    protected void build() {
        for (int i = 0; i < 27; i++) set(i, filler(), null);
        PlayerData pd = plugin.players().get(viewer.getUniqueId());
        SummonDef cur = plugin.summonStore().get(pd.summonId);
        set(22, Items.icon(Material.ARROW, "<yellow>Volver"), (p, t) -> new MySummonMenu(plugin, p).open());
        if (cur == null || cur.evolutions.isEmpty()) {
            set(13, Items.icon(Material.BARRIER, "<red>Sin evoluciones", "<gray>Esta invocación no tiene ramas."), null);
            return;
        }
        int[] pos = {11, 13, 15, 10, 16};
        for (int i = 0; i < cur.evolutions.size() && i < pos.length; i++) {
            SummonDef next = plugin.summonStore().get(cur.evolutions.get(i));
            if (next == null) continue;
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Requiere nivel: <white>" + cur.evolveLevel + (pd.level >= cur.evolveLevel ? " <green>✔" : " <red>✘"));
            if (!cur.evolveItem.isEmpty()) {
                var it = plugin.itemStore().get(cur.evolveItem);
                lore.add("<gray>Consume: <white>" + (it != null ? it.name : cur.evolveItem));
            }
            lore.add("<gray>Pasas a nivel 1 (talentos reiniciados).");
            lore.add("");
            lore.add("<yellow>Click: evolucionar");
            String id = next.id;
            set(pos[i], Items.icon(next.icon, next.name, lore), (p, t) -> {
                if (plugin.summons().evolve(p, id)) new MySummonMenu(plugin, p).open();
                else refresh();
            });
        }
    }
}
