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
 * Minijuego real para Tallado (Cortapiedras) y Tejido (Telar). Antes esto
 * era una conversion instantanea con un puntaje fijo de 60 SIEMPRE — por
 * eso nunca se notaba una mejora o empeoramiento real. Ahora, igual que
 * Calentado/Golpeado/Enfriado, el puntaje depende de tu precision y
 * termina afectando de verdad las estadisticas del objeto final.
 */
public class CarvingMinigame implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final HammeringSession session;
    private final Inventory inventory;
    private final String baseKey;
    private final String displayName;
    private final ProcessStage step;
    private final Material outputIcon;
    private boolean finished = false;
    /** Items que se le quitaron al jugador para empezar: se le devuelven si no termina el minijuego. */
    private final java.util.List<ItemStack> refund = new java.util.ArrayList<>();

    /** Registra lo consumido al iniciar (copia de 1 unidad). Encadenable: new X(...).refund(a, b).start(). */
    public CarvingMinigame refund(ItemStack... items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) refund.add(item.clone());
        }
        return this;
    }

    public CarvingMinigame(CustomSmithing plugin, Player player, String baseKey, String displayName,
                            ProcessStage step, Material outputIcon) {
        this.plugin = plugin;
        this.player = player;
        this.baseKey = baseKey;
        this.displayName = displayName;
        this.step = step;
        this.outputIcon = outputIcon;

        int hits = 3;
        int perfectZone = plugin.getConfig().getInt("minijuego.zona_perfecta_slots", 1) + plugin.getUnlockService().extraPerfectSlots(player);
        int goodZone = plugin.getConfig().getInt("minijuego.zona_buena_slots", 2);
        this.session = new HammeringSession(hits, perfectZone, goodZone);
        String title = step == ProcessStage.TALLADO ? "\u00a76\u26cf Tallado: corta en el momento justo" : "\u00a7d\ud83e\uddf5 Tejido: teje en el momento justo";
        this.inventory = Bukkit.createInventory(this, 9, title);
    }

    public void start() {
        MinigameLock.lock(player.getUniqueId());
        render();
        player.openInventory(inventory);
        int intervalTicks = Math.max(1, plugin.getConfig().getInt("minijuego.duracion_ciclo_ticks", 34) / 10);
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, intervalTicks, intervalTicks);
        session.setTaskId(taskId);
    }

    private void tick() {
        if (finished) return;
        int pos = session.getCursorPosition();
        boolean right = session.isMovingRight();
        if (right) { pos++; if (pos >= 8) { pos = 8; right = false; } }
        else { pos--; if (pos <= 0) { pos = 0; right = true; } }
        session.setCursorPosition(pos);
        session.setMovingRight(right);
        render();
    }

    private void render() {
        for (int i = 0; i < 9; i++) inventory.setItem(i, slotItem(i));
    }

    private ItemStack slotItem(int index) {
        Material mat; String name;
        int perfect = session.getPerfectSlot();
        int dist = Math.abs(index - perfect);
        Material perfectMat = step == ProcessStage.TALLADO ? Material.LIME_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE;
        if (dist <= session.getPerfectZoneSize()) { mat = perfectMat; name = "\u00a7a\u00a7lCORTE/PUNTADA PERFECTA"; }
        else if (dist <= session.getGoodZoneSize()) { mat = Material.YELLOW_STAINED_GLASS_PANE; name = "\u00a7eAceptable"; }
        else { mat = Material.RED_STAINED_GLASS_PANE; name = "\u00a7cMomento incorrecto"; }

        if (index == session.getCursorPosition()) {
            mat = step == ProcessStage.TALLADO ? Material.STONECUTTER : Material.LOOM;
            name = "\u00a76\u00a7l\u25BC " + (step == ProcessStage.TALLADO ? "CORTAR" : "TEJER") + " AHORA \u25BC";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("\u00a77Clic para " + (step == ProcessStage.TALLADO ? "cortar" : "tejer") + " en este instante"));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (finished || event.getRawSlot() < 0 || event.getRawSlot() > 8) return;
        HammeringSession.HitResult result = session.registerHit();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        data.registerHitAccuracy(result == HammeringSession.HitResult.PERFECTO ? 100 : result == HammeringSession.HitResult.BUENO ? 60 : 10);

        switch (result) {
            case PERFECTO -> { player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 1f, 1.6f); Messages.send(player, "minijuego.talla_perfecto"); }
            case BUENO -> { player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 1f, 1f); Messages.send(player, "minijuego.talla_bueno"); }
            case FALLO -> { player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); Messages.send(player, "minijuego.talla_fallo"); }
        }

        if (session.isComplete()) finish();
        else render();
    }

    private void finish() {
        finished = true;
        Bukkit.getScheduler().cancelTask(session.getTaskId());
        MinigameLock.unlock(player.getUniqueId());
        player.closeInventory();

        double score = plugin.getProgressService().applyTo(player, step == ProcessStage.TEJIDO ? "telar" : "cortapiedras", baseKey,
                Math.max(0, Math.min(100, session.getAccumulatedScore())));

        ItemStack piece = plugin.getPieceCatalog().build(step, baseKey, displayName, outputIcon, score);

        plugin.getProgressService().recordPiece(player, baseKey);
        plugin.getGiveService().give(player, piece);
        Messages.send(player, "procesos.pieza_lista", "%icono%", step.getIcon(), "%etapa%", step.getLabel().toLowerCase(), "%puntaje%", Text.fmt1(score));
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
