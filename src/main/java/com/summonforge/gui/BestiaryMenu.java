package com.summonforge.gui;

import com.summonforge.boss.BossDef;
import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bestiario: bosses derrotados por el jugador (los no derrotados aparecen como ???). */
public final class BestiaryMenu extends PagedMenu<BossDef> {
    private final PlayerData pd;

    public BestiaryMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Bestiario");
        this.pd = plugin.players().get(viewer.getUniqueId());
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    @Override
    protected List<BossDef> entries() {
        List<BossDef> l = new ArrayList<>(plugin.bossStore().all());
        l.sort(Comparator.comparing(b -> b.id));
        return l;
    }

    @Override
    protected String label(BossDef b) {
        return pd.bossKills.getOrDefault(b.id, 0) > 0 ? Text.plain(Text.mm(b.name)) : "???";
    }

    @Override
    protected ItemStack render(BossDef b) {
        int kills = pd.bossKills.getOrDefault(b.id, 0);
        if (kills <= 0) return Items.icon(Material.GRAY_DYE, "<gray>???", "<dark_gray>Aún no has derrotado a este boss.");
        return Items.icon(b.icon, b.name, "<gray>Derrotado: <white>" + kills + (kills == 1 ? " vez" : " veces"),
                "<gray>Elemento: <white>" + b.element.label,
                b.family.isEmpty() ? "" : "<gray>Familia: <white>" + b.family, "<gray>Vida: <white>" + Math.round(b.health));
    }

    @Override
    protected void picked(BossDef b, ClickType click) {}

    @Override
    protected void back() {
        new MySummonMenu(plugin, viewer).open();
    }
}
