package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.summon.Talent;
import com.summonforge.util.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Talentos de la invocación: 1 punto por nivel a partir del 2, máx. 5 por talento. Se aplican al volver a invocar. */
public final class TalentMenu extends AbstractMenu {
    private static final int[] SLOTS = {11, 13, 15};

    public TalentMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Talentos", 3);
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    private int free(PlayerData pd) {
        int spent = pd.talents.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, pd.level - 1) - spent;
    }

    @Override
    protected void build() {
        for (int i = 0; i < 27; i++) set(i, filler(), null);
        PlayerData pd = plugin.players().get(viewer.getUniqueId());
        set(4, Items.icon(Material.EXPERIENCE_BOTTLE, "<yellow>Puntos libres: <white>" + free(pd),
                "<gray>Ganas 1 punto por cada nivel de la invocación."), null);
        Talent[] ts = Talent.values();
        for (int i = 0; i < ts.length && i < SLOTS.length; i++) {
            Talent t = ts[i];
            int cur = pd.talents.getOrDefault(t.name(), 0);
            set(SLOTS[i], Items.icon(t.icon, "<yellow>" + t.label, "<gray>" + t.desc,
                    "<white>Puntos: " + cur + "/" + Talent.MAX_POINTS, "<yellow>Click: gastar 1 punto"), (p, c) -> {
                if (free(pd) <= 0) {
                    plugin.messages().send(p, "talent-none");
                } else if (cur >= Talent.MAX_POINTS) {
                    plugin.messages().send(p, "talent-max");
                } else {
                    pd.talents.put(t.name(), cur + 1);
                    plugin.players().save(pd);
                    plugin.messages().send(p, "talent-applied");
                }
                refresh();
            });
        }
        set(22, Items.icon(Material.TNT, "<red>Reiniciar talentos", "<gray>Recuperas todos los puntos."), (p, c) -> {
            pd.talents.clear();
            plugin.players().save(pd);
            plugin.messages().send(p, "talent-reset");
            refresh();
        });
        set(18, Items.icon(Material.ARROW, "<yellow>Volver"), (p, c) -> new MySummonMenu(plugin, p).open());
    }
}
