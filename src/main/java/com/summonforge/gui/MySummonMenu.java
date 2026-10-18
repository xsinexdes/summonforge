package com.summonforge.gui;

import com.summonforge.ability.AbilityDef;
import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.summon.SummonDef;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** "Mi Invocación": nivel, stats, habilidades y botones de mejorar / invocar / despedir. */
public final class MySummonMenu extends AbstractMenu {
    public MySummonMenu(SummonForge plugin, Player viewer) {
        super(plugin, viewer, "Mi Invocación", 4);
    }

    @Override
    public String permission() {
        return "summonforge.use";
    }

    @Override
    protected void build() {
        for (int i = 0; i < 36; i++) set(i, filler(), null);
        PlayerData pd = plugin.players().get(viewer.getUniqueId());
        SummonDef def = plugin.summonStore().get(pd.summonId);
        if (def == null) {
            set(13, Items.icon(Material.BARRIER, "<red>No tienes ninguna invocación",
                    "<gray>Consigue un objeto de invocación derrotando bosses."), null);
            set(31, Items.icon(Material.ARROW, "<yellow>Cerrar"), (p, t) -> p.closeInventory());
            return;
        }
        double scale = plugin.summons().levelScale(def, pd.level);
        boolean active = plugin.summons().activeOf(viewer.getUniqueId()) != null;
        int cost = def.upgradeXpLevels * pd.level;
        set(11, Items.icon(def.icon, def.name, "<gray>Nivel: <white>" + pd.level + "/" + PlayerData.MAX_LEVEL,
                "<gray>Estado: " + (active ? "<green>invocada" : "<gray>descansando")), null);
        set(13, Items.icon(Material.IRON_SWORD, "<yellow>Estadísticas",
                "<gray>Vida: <white>" + Math.round(def.health * scale),
                "<gray>Daño: <white>" + Math.round(def.damage * scale),
                "<gray>Habilidades: <white>+" + Math.round((scale - 1) * 100) + "% de daño",
                "<gray>Tipo: <white>" + def.type,
                "<gray>Lealtad: <white>nivel " + Math.min(10, pd.loyalty / 100) + " <gray>(" + pd.loyalty + " pts, +2% daño por nivel)",
                "<gray>Sincronización: <white>" + pd.sync + "% <gray>(+" + Math.round(pd.sync * 0.25) + "% daño)"), null);
        set(15, Items.icon(Material.EXPERIENCE_BOTTLE, "<green>Mejorar",
                pd.level >= PlayerData.MAX_LEVEL ? "<yellow>Nivel máximo" : "<gray>Cuesta <white>" + cost + " <gray>niveles de XP",
                "<yellow>Click: mejorar"), (p, t) -> {
            plugin.summons().upgrade(p);
            open();
        });
        int slot = 19;
        for (String id : plugin.summons().loadout(viewer.getUniqueId(), def)) {
            AbilityDef a = plugin.abilityStore().get(id);
            if (a == null || slot > 25) continue;
            int idx = slot - 19;
            set(slot++, Items.icon(a.icon, a.name, "<gray>" + Text.escape(a.description), "",
                    "<yellow>Click: lanzar <gray>(también /inv habilidad " + (idx + 1) + ")"), (p, t) -> {
                p.closeInventory();
                plugin.summons().castSlot(p, idx);
            });
        }
        set(23, Items.icon(Material.ENCHANTED_BOOK, "<aqua>Equipar habilidades", "<gray>Elige cuáles usa tu invocación.",
                "<yellow>Click: abrir"), (p, t) -> new EquipMenu(plugin, p).open());
        set(24, Items.icon(Material.COMPASS, "<gold>Modo: <white>" + pd.mode.label, "<gray>" + pd.mode.desc,
                "<yellow>Click: cambiar"), (p, t) -> {
            pd.mode = pd.mode.next();
            plugin.players().save(pd);
            plugin.messages().send(p, "mode-changed", "mode", pd.mode.label);
            open();
        });
        set(25, Items.icon(Material.EXPERIENCE_BOTTLE, "<light_purple>Talentos", "<gray>Gasta puntos de nivel en mejoras.",
                "<yellow>Click: abrir"), (p, t) -> new TalentMenu(plugin, p).open());
        set(26, Items.icon(Material.WRITTEN_BOOK, "<gold>Bestiario", "<gray>Bosses que has derrotado.",
                "<yellow>Click: abrir"), (p, t) -> new BestiaryMenu(plugin, p).open());
        set(33, Items.icon(Material.COMPARATOR, "<yellow>Teclas", "<gray>Elige con qué teclas invocas y lanzas.",
                "<yellow>Click: abrir"), (p, t) -> new KeysMenu(plugin, p).open());
        set(29, Items.icon(Material.LIME_DYE, active ? "<red>Despedir" : "<green>Invocar", "<gray>Atajo: Shift + F"), (p, t) -> {
            p.closeInventory();
            plugin.summons().toggle(p);
        });
        set(31, Items.icon(Material.ARROW, "<yellow>Cerrar"), (p, t) -> p.closeInventory());
    }
}
