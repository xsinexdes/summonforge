package com.summonforge.item;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.gui.Pickers;
import com.summonforge.summon.SummonDef;
import com.summonforge.util.Attrs;
import com.summonforge.util.Fx;
import com.summonforge.util.Hearts;
import com.summonforge.util.Potions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ItemListener implements Listener {
    private final SummonForge plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private BukkitTask decayTask;

    public ItemListener(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::decay, 1200L, 1200L);
    }

    public void stop() {
        if (decayTask != null) decayTask.cancel();
        decayTask = null;
        cooldowns.clear();
    }

    /** Cada minuto: la fatiga baja 1 y se restauran los corazones cuando termina el castigo. */
    private void decay() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData pd = plugin.players().get(p.getUniqueId());
            pd.fatigue.replaceAll((k, v) -> v - 1);
            pd.fatigue.values().removeIf(v -> v <= 0);
            if (pd.heartsLost > 0 && now >= pd.heartsLostUntil) restoreHearts(p, pd);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.heartsLost <= 0) return;
        if (System.currentTimeMillis() >= pd.heartsLostUntil) restoreHearts(p, pd);
        else applyHearts(p, pd);
    }

    private void applyHearts(Player p, PlayerData pd) {
        Attrs.set(p, "max_health", Math.max(2, 20 - 2.0 * pd.heartsLost));
        double max = Attrs.maxHealth(p);
        if (p.getHealth() > max) p.setHealth(max);
    }

    private void restoreHearts(Player p, PlayerData pd) {
        pd.heartsLost = 0;
        pd.heartsLostUntil = 0;
        Attrs.set(p, "max_health", 20);
        plugin.players().save(pd);
        plugin.messages().send(p, "hearts-restored");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        String itemId = ItemFactory.idOf(hand);
        if (itemId == null) return;
        ItemDef idef = plugin.itemStore().get(itemId);
        if (idef == null) return;
        if (idef.kind == ItemKind.SUMMON && (idef.summonId.isEmpty() || plugin.summonStore().get(idef.summonId) == null)) return;
        e.setCancelled(true);
        if (!p.hasPermission("summonforge.use")) {
            plugin.messages().send(p, "no-permission");
            return;
        }
        String uid = ItemFactory.uidOf(hand);
        switch (idef.kind) {
            case SUMMON -> useSummon(p, plugin.summonStore().get(idef.summonId), uid);
            case AWAKEN -> useAwaken(p, idef, uid);
            case RUNE -> useRune(p, idef, uid);
            case POWER -> usePower(p, idef, uid);
        }
    }

    private void useSummon(Player p, SummonDef sdef, String uid) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.summonId.equals(sdef.id)) {
            plugin.messages().send(p, "summon-already");
            return;
        }
        if (pd.summonId.isEmpty()) {
            sign(p, sdef, uid);
            return;
        }
        new Pickers.Choice(plugin, p, "Nueva invocación", List.of(
                new Pickers.Option(Material.ENDER_PEARL, "<yellow>Reemplazar", "<gray>Sustituyes tu invocación actual.", () -> {
                    p.closeInventory();
                    sign(p, sdef, uid);
                }),
                new Pickers.Option(Material.NETHER_STAR, "<light_purple>Fusionar (sacrificar)",
                        "<gray>+" + sdef.fusionBonus + "% permanente a tu invocación actual.", () -> {
                    p.closeInventory();
                    fuse(p, sdef, uid);
                }),
                new Pickers.Option(Material.BARRIER, "<red>Cancelar", "", p::closeInventory))).open();
    }

    /** Pacto: si la invocación pide coste (XP / corazones) se firma con confirmación. */
    private void sign(Player p, SummonDef sdef, String uid) {
        if (sdef.contractXp <= 0 && sdef.contractHearts <= 0) {
            if (consume(p, uid)) plugin.summons().grant(p, sdef);
            return;
        }
        String info = "<gray>Coste: " + (sdef.contractXp > 0 ? "<white>" + sdef.contractXp + " niveles de XP " : "")
                + (sdef.contractHearts > 0 ? "<red>" + sdef.contractHearts + " corazón(es) durante " + sdef.contractMinutes + " min" : "");
        new Pickers.Confirm(plugin, p, "Firmar pacto", info, () -> {
            p.closeInventory();
            if (p.getLevel() < sdef.contractXp) {
                plugin.messages().send(p, "summon-no-xp", "cost", String.valueOf(sdef.contractXp));
                return;
            }
            if (!consume(p, uid)) return;
            p.setLevel(p.getLevel() - sdef.contractXp);
            if (sdef.contractHearts > 0) Hearts.lose(plugin, p, sdef.contractHearts, sdef.contractMinutes);
            plugin.summons().grant(p, sdef);
        }, p::closeInventory).open();
    }

    /** Fusión: el objeto de otra invocación se sacrifica para dar un bonus permanente a la actual. */
    private void fuse(Player p, SummonDef sdef, String uid) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.fusion >= 100) {
            plugin.messages().send(p, "fusion-max");
            return;
        }
        if (!consume(p, uid)) return;
        pd.fusion = Math.min(100, pd.fusion + sdef.fusionBonus);
        plugin.players().save(pd);
        plugin.messages().send(p, "fused", "bonus", String.valueOf(pd.fusion));
        Fx.spawn(p.getWorld(), "ENCHANT", p.getLocation().add(0, 1, 0), 50, 0.6, 0.9, 0.6, 0.5);
        Fx.sound(p.getLocation(), "block.enchantment_table.use", 1f, 1f);
    }

    private void useRune(Player p, ItemDef d, String uid) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.runes.size() >= plugin.getConfig().getInt("summon.rune-slots", 3)) {
            plugin.messages().send(p, "rune-full");
            return;
        }
        if (!consume(p, uid)) return;
        pd.runes.add(d.id);
        plugin.players().save(pd);
        plugin.messages().send(p, "rune-equipped", "rune", d.name);
        Fx.sound(p.getLocation(), "block.enchantment_table.use", 1f, 1.4f);
    }

    private void useAwaken(Player p, ItemDef d, String uid) {
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.summonId.isEmpty()) {
            plugin.messages().send(p, "summon-none");
            return;
        }
        if (pd.awakening >= PlayerData.MAX_AWAKENING) {
            plugin.messages().send(p, "awaken-max");
            return;
        }
        if (pd.kills < d.requireKills) {
            plugin.messages().send(p, "awaken-req-kills", "n", String.valueOf(d.requireKills), "have", String.valueOf(pd.kills));
            return;
        }
        if (pd.sync < d.requireSync) {
            plugin.messages().send(p, "awaken-req-sync", "n", String.valueOf(d.requireSync), "have", String.valueOf(pd.sync));
            return;
        }
        if (!consume(p, uid)) return;
        pd.awakening++;
        plugin.players().save(pd);
        plugin.messages().send(p, "awaken-up", "level", String.valueOf(pd.awakening));
        plugin.messages().send(p, "awaken-stage", "stage", plugin.summons().stageName(pd.awakening));
        Fx.spawn(p.getWorld(), "END_ROD", p.getLocation().add(0, 1, 0), 40, 0.6, 0.9, 0.6, 0.05);
        Fx.sound(p.getLocation(), "entity.player.levelup", 1f, 0.6f);
        if (d.heartLoss > 0) Hearts.lose(plugin, p, d.heartLoss * pd.awakening, d.heartLossMinutes);
    }

    private void usePower(Player p, ItemDef d, String uid) {
        String key = p.getUniqueId() + ":" + d.id;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(key);
        if (until != null && now < until) {
            plugin.messages().send(p, "item-cooldown", "s", String.valueOf((until - now) / 1000 + 1));
            return;
        }
        if (!consume(p, uid)) return;
        cooldowns.put(key, now + d.cooldownSeconds * 1000L);
        Potions.add(p, "strength", d.buffSeconds * 20, 1);
        Potions.add(p, "speed", d.buffSeconds * 20, 1);
        plugin.messages().send(p, "power-used", "s", String.valueOf(d.buffSeconds));
        Fx.spawn(p.getWorld(), "FLAME", p.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);

        PlayerData pd = plugin.players().get(p.getUniqueId());
        int fatigue = pd.fatigue.merge(d.id, 1, Integer::sum);
        if (d.overuseAt > 0 && fatigue >= d.overuseAt) {
            pd.fatigue.put(d.id, 0);
            overuse(p, pd, d);
        }
        plugin.players().save(pd);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Potions.add(p, "slowness", d.fatigueSeconds * 20, 1);
            Potions.add(p, "weakness", d.fatigueSeconds * 20, 0);
            plugin.messages().send(p, "power-fatigue", "s", String.valueOf(d.fatigueSeconds));
        }, d.buffSeconds * 20L);
    }

    private void overuse(Player p, PlayerData pd, ItemDef d) {
        Potions.add(p, "blindness", 200, 0);
        Potions.add(p, "nausea", 200, 0);
        plugin.messages().send(p, "overuse");
        if (d.heartLoss > 0) {
            pd.heartsLost = Math.max(pd.heartsLost, d.heartLoss);
            pd.heartsLostUntil = System.currentTimeMillis() + d.heartLossMinutes * 60_000L;
            applyHearts(p, pd);
            plugin.messages().send(p, "hearts-lost", "n", String.valueOf(d.heartLoss), "m", String.valueOf(d.heartLossMinutes));
        }
    }

    /** Verifica que el ítem siga en la mano (anti-duplicado) antes de consumirlo. */
    private boolean consume(Player p, String uid) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (uid == null || !Objects.equals(uid, ItemFactory.uidOf(hand))) return false;
        hand.setAmount(hand.getAmount() - 1);
        return true;
    }
}
