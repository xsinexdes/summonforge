package com.summonforge.summon;

import com.summonforge.core.SummonForge;
import com.summonforge.data.PlayerData;
import com.summonforge.gui.MySummonMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entrada configurable por jugador (menú Teclas). Solo se cancela el evento original si la entrada
 * está asignada a una acción y esa acción aplica; si no, la tecla funciona con normalidad.
 */
public final class SummonInput implements Listener {
    private final SummonForge plugin;
    private final SummonManager manager;
    private final Map<UUID, Long> lastSneak = new HashMap<>();

    public SummonInput(SummonForge plugin, SummonManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (handle(e.getPlayer(), e.getPlayer().isSneaking() ? KeyInput.SHIFT_F : KeyInput.F)) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (handle(e.getPlayer(), KeyInput.Q)) e.setCancelled(true);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        UUID id = e.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSneak.put(id, now);
        if (last != null && now - last < 400) {
            lastSneak.remove(id);
            handle(e.getPlayer(), KeyInput.DOUBLE_SHIFT);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        KeyInput in = e.getNewSlot() == 7 ? KeyInput.HOTBAR_8 : (e.getNewSlot() == 8 ? KeyInput.HOTBAR_9 : null);
        if (in != null && handle(e.getPlayer(), in)) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (!e.getPlayer().isSneaking()) return;
        if (handle(e.getPlayer(), KeyInput.SHIFT_LEFT_CLICK)) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastSneak.remove(e.getPlayer().getUniqueId());
    }

    /** @return true si la entrada estaba asignada a una acción aplicable (y se ejecutó). */
    private boolean handle(Player p, KeyInput in) {
        if (in == KeyInput.NONE || !p.hasPermission("summonforge.use")) return false;
        PlayerData pd = plugin.players().get(p.getUniqueId());
        if (pd.summonId.isEmpty()) return false;
        KeyAction action = null;
        for (KeyAction a : KeyAction.values()) {
            if (pd.keys.get(a) == in) {
                action = a;
                break;
            }
        }
        if (action == null) return false;
        if (action == KeyAction.TOGGLE) {
            if (manager.throttle(p.getUniqueId(), 500)) manager.toggle(p);
            return true;
        }
        if (action == KeyAction.MENU) {
            new MySummonMenu(plugin, p).open();
            return true;
        }
        if (action == KeyAction.MODE) {
            if (manager.throttle(p.getUniqueId(), 400)) {
                pd.mode = pd.mode.next();
                plugin.players().save(pd);
                plugin.messages().send(p, "mode-changed", "mode", pd.mode.label);
            }
            return true;
        }
        if (manager.activeOf(p.getUniqueId()) == null) return false;
        if (action == KeyAction.ULTIMATE) {
            if (manager.throttle(p.getUniqueId(), 400)) manager.castUltimate(p);
            return true;
        }
        if (manager.throttle(p.getUniqueId(), 250)) manager.castSlot(p, action.slot);
        return true;
    }
}
