package com.summonforge.util;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import org.bukkit.entity.Player;

/** Pérdida temporal de corazones (pactos, sobreuso, despertar). La restauración la hace ItemListener. */
public final class Hearts {
    private Hearts() {}

    public static void lose(SummonForge plugin, Player p, int hearts, int minutes) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        pd.heartsLost = Math.max(pd.heartsLost, hearts);
        pd.heartsLostUntil = System.currentTimeMillis() + minutes * 60_000L;
        Attrs.set(p, "max_health", Math.max(2, 20 - 2.0 * pd.heartsLost));
        double max = Attrs.maxHealth(p);
        if (p.getHealth() > max) p.setHealth(max);
        plugin.players().save(pd);
        plugin.messages().send(p, "hearts-lost", "n", String.valueOf(hearts), "m", String.valueOf(minutes));
    }
}
