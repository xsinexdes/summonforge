package com.summonforge.gui;

import com.summonforge.core.SummonForge;
import com.summonforge.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Entrada de texto por chat: el evento es async, así que siempre se vuelve al hilo principal. */
public final class ChatPrompt implements Listener {
    private record Pending(Consumer<String> onInput, Runnable onCancel) {}

    private final SummonForge plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatPrompt(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String what, Consumer<String> onInput, Runnable onCancel) {
        p.closeInventory();
        Pending pe = new Pending(onInput, onCancel);
        pending.put(p.getUniqueId(), pe);
        plugin.messages().send(p, "prompt-hint", "what", what);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(p.getUniqueId(), pe)) {
                plugin.messages().send(p, "prompt-expired");
                onCancel.run();
            }
        }, 600L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Pending pe = pending.remove(e.getPlayer().getUniqueId());
        if (pe == null) return;
        e.setCancelled(true);
        String text = Text.plain(e.message()).trim();
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancelar")) {
                plugin.messages().send(p, "prompt-cancelled");
                pe.onCancel().run();
            } else {
                pe.onInput().accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
