package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemFactory;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Runas equipadas en la invocación (slots configurables). Clic para quitar una y recuperarla. */
public final class RunesMenu extends AbstractMenu {
    public RunesMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Runas de la invocación", 3);
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    @Override
    protected void build() {
        for (int i = 0; i < 27; i++) set(i, filler(), null);
        PlayerData pd = plugin.players().get(viewer.getUniqueId());
        int slots = plugin.getConfig().getInt("summon.rune-slots", 3);
        int[] pos = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < slots && i < pos.length; i++) {
            if (i < pd.runes.size()) {
                int idx = i;
                ItemDef d = plugin.itemStore().get(pd.runes.get(i));
                String name = d != null ? d.name : pd.runes.get(i);
                String line = d != null ? "<gray>" + d.runeStat.label + ": <white>+" + Math.round(d.runeValue) + "%" : "<red>Runa eliminada";
                set(pos[i], Items.glow(Items.icon(d != null ? d.material : Material.BARRIER, name, line, "", "<yellow>Click: quitar y recuperar")), (p, t) -> {
                    String id = pd.runes.remove(idx);
                    plugin.players().save(pd);
                    ItemDef back = plugin.itemStore().get(id);
                    if (back != null) {
                        p.getInventory().addItem(ItemFactory.create(back)).values()
                                .forEach(rest -> p.getWorld().dropItem(p.getLocation(), rest));
                    }
                    refresh();
                });
            } else {
                set(pos[i], Items.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "<gray>Slot libre", "<dark_gray>Usa una runa con clic derecho."), null);
            }
        }
        set(22, Items.icon(Material.ARROW, "<yellow>Volver"), (p, t) -> new MySummonMenu(plugin, p).open());
    }
}
