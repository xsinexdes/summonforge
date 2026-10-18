package com.summonforge.item;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.gui.Pickers;
import com.summonforge.summon.SummonDef;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class ItemListener implements Listener {
    private final SummonForge plugin;

    public ItemListener(SummonForge plugin) {
        this.plugin = plugin;
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
        if (idef == null || idef.summonId.isEmpty()) return;
        SummonDef sdef = plugin.summonStore().get(idef.summonId);
        if (sdef == null) return;
        e.setCancelled(true);
        if (!p.hasPermission("summonforge.use")) {
            plugin.messages().send(p, "no-permission");
            return;
        }
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.summonId.equals(sdef.id)) {
            plugin.messages().send(p, "summon-already");
            return;
        }
        String uid = ItemFactory.uidOf(hand);
        if (pd.summonId.isEmpty()) {
            claim(p, sdef, uid);
        } else {
            new Pickers.Confirm(plugin, p, "Reemplazar invocación", plugin.messages().raw("replace-info"),
                    () -> {
                        p.closeInventory();
                        claim(p, sdef, uid);
                    }, p::closeInventory).open();
        }
    }

    /** Verifica que el ítem siga en la mano (anti-duplicado) antes de consumirlo. */
    private void claim(Player p, SummonDef sdef, String uid) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (uid == null || !Objects.equals(uid, ItemFactory.uidOf(hand))) return;
        hand.setAmount(hand.getAmount() - 1);
        plugin.summons().grant(p, sdef);
    }
}
