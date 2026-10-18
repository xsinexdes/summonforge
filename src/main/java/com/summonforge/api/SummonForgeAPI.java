package com.summonforge.api;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.boss.BossDef;
import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemFactory;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** API pública estable para otros plugins. Los eventos están en este mismo paquete. */
public final class SummonForgeAPI {
    private final SummonForge plugin;

    private SummonForgeAPI(SummonForge plugin) {
        this.plugin = plugin;
    }

    /** @return la API, o null si SummonForge no está cargado. */
    public static SummonForgeAPI get() {
        SummonForge p = SummonForge.get();
        return p == null ? null : new SummonForgeAPI(p);
    }

    /** Id de la invocación del jugador ("" si no tiene). */
    public String summonOf(Player p) {
        return plugin.players().get(p.getUniqueId()).summonId;
    }

    public int levelOf(Player p) {
        return plugin.players().get(p.getUniqueId()).level;
    }

    public int loyaltyOf(Player p) {
        PlayerData d = plugin.players().get(p.getUniqueId());
        return d.loyalty;
    }

    public boolean hasActiveSummon(Player p) {
        return plugin.summons().activeOf(p.getUniqueId()) != null;
    }

    public void toggleSummon(Player p) {
        plugin.summons().toggle(p);
    }

    public boolean spawnBoss(String bossId, Location loc) {
        BossDef d = plugin.bossStore().get(bossId);
        return d != null && plugin.bosses().spawn(d, loc);
    }

    public boolean isBossAlive(String bossId) {
        return plugin.bosses().isAlive(bossId);
    }

    /** Lanza una habilidad desde cualquier entidad, hacia donde mira. */
    public boolean castAbility(LivingEntity caster, String abilityId, boolean ignoreCooldown) {
        AbilityDef d = plugin.abilityStore().get(abilityId);
        if (d == null) return false;
        Player owner = caster instanceof Player p ? p : null;
        return plugin.abilities().cast(new CastContext(caster, owner, caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(), 1.0), d, ignoreCooldown);
    }

    public boolean giveItem(Player p, String itemId) {
        ItemDef d = plugin.itemStore().get(itemId);
        if (d == null) return false;
        p.getInventory().addItem(ItemFactory.create(d));
        return true;
    }
}
