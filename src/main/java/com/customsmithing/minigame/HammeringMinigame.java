package com.customsmithing.minigame;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.util.Text;
import com.customsmithing.gui.SmithingGUIHolder;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Minijuego de golpeado en la forja. UNIVERSAL por material: el resultado
 * es una "Pieza Forjada de <Material>" que sirve para cualquier receta que
 * use ese metal. La receta final recien se elige en la Mesa de Ensamblaje.
 */
public class HammeringMinigame implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final HammeringSession session;
    private final Inventory inventory;
    private final String baseKey;
    private final String displayName;
    private final double previousScore;
    private boolean finished = false;
    /** Items que se le quitaron al jugador para empezar: se le devuelven si no termina el minijuego. */
    private final java.util.List<ItemStack> refund = new java.util.ArrayList<>();

    /** Registra lo consumido al iniciar (copia de 1 unidad). Encadenable: new X(...).refund(a, b).start(). */
    public HammeringMinigame refund(ItemStack... items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) refund.add(item.clone());
        }
        return this;
    }

    public HammeringMinigame(CustomSmithing plugin, Player player, String baseKey, String displayName, double previousScore, double difficulty) {
        this.plugin = plugin;
        this.player = player;
        this.baseKey = baseKey;
        this.displayName = displayName;
        this.previousScore = previousScore;

        int hitsRequired = plugin.getConfig().getInt("minijuego.golpes_requeridos", 5);
        int perfectZone = plugin.getConfig().getInt("minijuego.zona_perfecta_slots", 1) + plugin.getUnlockService().extraPerfectSlots(player);
        int goodZone = plugin.getConfig().getInt("minijuego.zona_buena_slots", 2);

        if (plugin.getConfig().getBoolean("ia_adaptativa.activar", true)) {
            PlayerData data = plugin.getPlayerDataManager().get(player);
            int maxAdjust = plugin.getConfig().getInt("ia_adaptativa.ajuste_maximo_slots", 1);
            if (data.getAvgTimingAccuracy() > 75) {
                perfectZone = Math.max(0, perfectZone - maxAdjust);
            } else if (data.getAvgTimingAccuracy() < 35) {
                perfectZone = perfectZone + maxAdjust;
            }
        }

        int difficultyAdjust = (int) Math.round(difficulty - 1.0);
        perfectZone = Math.max(0, perfectZone - difficultyAdjust);

        this.session = new HammeringSession(hitsRequired, perfectZone, goodZone);
        this.inventory = Bukkit.createInventory(this, 9, "\u2692 Golpe 1/" + hitsRequired);
    }

    public static boolean hasActiveSession(Player player) {
        return MinigameLock.isLocked(player.getUniqueId());
    }

    public void start() {
        MinigameLock.lock(player.getUniqueId());
        render();
        player.openInventory(inventory);

        int intervalTicks = Math.max(1, plugin.getConfig().getInt("minijuego.duracion_ciclo_ticks", 34) / 9);
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, intervalTicks, intervalTicks);
        session.setTaskId(taskId);
    }

    private void tick() {
        if (finished) return;
        int pos = session.getCursorPosition();
        boolean right = session.isMovingRight();

        if (right) {
            pos++;
            if (pos >= 8) { pos = 8; right = false; }
        } else {
            pos--;
            if (pos <= 0) { pos = 0; right = true; }
        }
        session.setCursorPosition(pos);
        session.setMovingRight(right);
        render();
    }

    private void render() {
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, buildSlotItem(i));
        }
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        "\u00a76\u2692 Golpea en el momento justo (clic izquierdo en cualquier item) \u2692"));
    }

    private ItemStack buildSlotItem(int index) {
        Material mat;
        String name;
        int perfect = session.getPerfectSlot();
        int distFromPerfect = Math.abs(index - perfect);

        if (distFromPerfect <= session.getPerfectZoneSize()) {
            mat = Material.LIME_STAINED_GLASS_PANE;
            name = "\u00a7a\u00a7lZONA PERFECTA";
        } else if (distFromPerfect <= session.getGoodZoneSize()) {
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            name = "\u00a7eZona buena";
        } else {
            mat = Material.RED_STAINED_GLASS_PANE;
            name = "\u00a7cZona mala";
        }

        if (index == session.getCursorPosition()) {
            mat = Material.ANVIL;
            name = "\u00a76\u00a7l\u25BC MARTILLO \u25BC";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new java.util.ArrayList<>();
        lore.add("\u00a77Clic para golpear cuando el");
        lore.add("\u00a77martillo esté aquí");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (finished || event.getRawSlot() < 0 || event.getRawSlot() > 8) return;

        HammeringSession.HitResult result = session.registerHit();

        double accuracyThisHit = switch (result) {
            case PERFECTO -> 100.0;
            case BUENO -> 60.0;
            case FALLO -> 10.0;
        };
        PlayerData data = plugin.getPlayerDataManager().get(player);
        data.registerHitAccuracy(accuracyThisHit);

        switch (result) {
            case PERFECTO -> {
                Messages.send(player, "minijuego.golpe_perfecto");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.6f);
                data.addXp(plugin.getConfig().getInt("experiencia.xp_por_golpe_perfecto", 3));
            }
            case BUENO -> {
                Messages.send(player, "minijuego.golpe_bueno");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.0f);
            }
            case FALLO -> {
                Messages.send(player, "minijuego.golpe_fallado");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                if (plugin.getConfig().getBoolean("fallos.activar", true) && Math.random() * 100 < failChance()) {
                    session.setFailed(true);
                }
            }
        }

        if (session.isComplete() || session.isFailed()) {
            finish();
        } else {
            render();
        }
    }

    /** Probabilidad de fallo, reducida por los nodos alcanzados del árbol de habilidades. */
    private double failChance() {
        double base = plugin.getConfig().getDouble("fallos.probabilidad_fallo_golpeado", 8);
        int tiers = com.customsmithing.data.Passives.tiersReached(plugin.getPlayerDataManager().get(player).getLevel(),
                plugin.getConfig().getIntegerList("habilidades.niveles"));
        double reduction = Math.min(90, tiers * plugin.getConfig().getDouble("habilidades.reduccion_fallo_por_nivel", 10));
        return base * (1.0 - reduction / 100.0);
    }

    private void finish() {
        finished = true;
        Bukkit.getScheduler().cancelTask(session.getTaskId());
        MinigameLock.unlock(player.getUniqueId());
        player.closeInventory();

        boolean damaged = session.isFailed();
        if (damaged && plugin.getConfig().getBoolean("fallos.golpeado_rompe_pieza", true)) {
            Messages.send(player, "minijuego.golpeado_danado");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
            return;
        }
        if (damaged) Messages.send(player, "minijuego.golpeado_baja_calidad");

        double rawScore = Math.max(0, Math.min(100, session.getAccumulatedScore()));
        if (damaged) rawScore *= 0.5; // pieza dañada pero aprovechable: la calidad baja a la mitad
        double finalScore = plugin.getProgressService().applyTo(player, "forja", baseKey,
                Math.max(0, Math.min(100, (previousScore + rawScore) / 2.0)));

        ItemStack shaped = plugin.getPieceCatalog().build(ProcessStage.GOLPEADO, baseKey, displayName,
                com.customsmithing.util.BaseKey.iconFor(plugin, baseKey, Material.IRON_NUGGET), finalScore);

        plugin.getProgressService().recordPiece(player, baseKey);
        plugin.getGiveService().give(player, shaped);
        Messages.send(player, "procesos.golpeado_completo", "%score%", Text.fmt1(finalScore));
        plugin.getPlayerDataManager().get(player).addXp(plugin.getConfig().getInt("experiencia.xp_por_paso_completado", 5));
    }

    @Override
    public void handleClose(Player closing) {
        if (!finished) {
            finished = true;
            Bukkit.getScheduler().cancelTask(session.getTaskId());
            MinigameLock.unlock(player.getUniqueId());
            // Cierre, desconexión, muerte, recarga o apagado: lo consumido vuelve al jugador.
            // Se vacía la lista antes de entregar para que una segunda llamada no duplique nada.
            java.util.List<ItemStack> toReturn = new java.util.ArrayList<>(refund);
            refund.clear();
            plugin.getGiveService().giveAll(closing, toReturn);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
