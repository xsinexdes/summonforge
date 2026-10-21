package com.customsmithing.minigame;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.ProcessStage;
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
 * Minijuego de "Calentado": UNIVERSAL por material, no por receta.
 * El resultado es un "Lingote Calentado de <Material>" que sirve para
 * CUALQUIER receta que use ese mismo metal (espada, hacha, armadura, etc.).
 * La receta final se elige recien en la Mesa de Ensamblaje.
 */
public class HeatingMinigame implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final HammeringSession session;
    private final Inventory inventory;
    private final String baseKey;
    private final String displayName;
    private boolean finished = false;
    /** Items que se le quitaron al jugador para empezar: se le devuelven si no termina el minijuego. */
    private final java.util.List<ItemStack> refund = new java.util.ArrayList<>();

    private Runnable onSuccess;
    private double scoreCap = 100.0;

    /** Tope de puntaje que permite el combustible usado (100 = sin límite). */
    public HeatingMinigame scoreCap(double cap) {
        this.scoreCap = Math.max(0, Math.min(100, cap));
        return this;
    }

    /** Se ejecuta solo si el minijuego termina bien (p. ej. para devolver el molde desgastado). */
    public HeatingMinigame onSuccess(Runnable action) {
        this.onSuccess = action;
        return this;
    }

    /** Registra lo consumido al iniciar (copia de 1 unidad). Encadenable: new X(...).refund(a, b).start(). */
    public HeatingMinigame refund(ItemStack... items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) refund.add(item.clone());
        }
        return this;
    }

    public HeatingMinigame(CustomSmithing plugin, Player player, String baseKey, String displayName, double difficulty) {
        this.plugin = plugin;
        this.player = player;
        this.baseKey = baseKey;
        this.displayName = displayName;

        int hits = Math.max(3, plugin.getConfig().getInt("minijuego.golpes_requeridos", 5) - 2);
        int perfectZone = plugin.getConfig().getInt("minijuego.zona_perfecta_slots", 1);
        int goodZone = plugin.getConfig().getInt("minijuego.zona_buena_slots", 2);
        int diffAdjust = (int) Math.round(difficulty - 1.0);
        perfectZone = Math.max(0, perfectZone - diffAdjust);
        this.session = new HammeringSession(hits, perfectZone, goodZone);
        this.inventory = Bukkit.createInventory(this, 9, "\u00a76\ud83d\udd25 Calentador: mant\u00e9n la temperatura");
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
        if (right) { pos++; if (pos >= 8) { pos = 8; right = false; } }
        else { pos--; if (pos <= 0) { pos = 0; right = true; } }
        session.setCursorPosition(pos);
        session.setMovingRight(right);
        render();
    }

    private void render() {
        for (int i = 0; i < 9; i++) inventory.setItem(i, slotItem(i));
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        "\u00a76\ud83d\udd25 Golpea cuando la temperatura est\u00e9 en el punto justo \ud83d\udd25"));
    }

    private ItemStack slotItem(int index) {
        Material mat; String name;
        int perfect = session.getPerfectSlot();
        int dist = Math.abs(index - perfect);
        if (dist <= session.getPerfectZoneSize()) { mat = Material.ORANGE_STAINED_GLASS_PANE; name = "\u00a76\u00a7lTEMPERATURA IDEAL"; }
        else if (dist <= session.getGoodZoneSize()) { mat = Material.YELLOW_STAINED_GLASS_PANE; name = "\u00a7eAceptable"; }
        else { mat = index < perfect ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE; name = index < perfect ? "\u00a7bMuy fr\u00edo" : "\u00a7cSobrecalentado"; }

        if (index == session.getCursorPosition()) { mat = Material.BLAZE_POWDER; name = "\u00a76\u00a7l\u25BC LLAMA \u25BC"; }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("\u00a77Clic para echar combustible", "\u00a77cuando la llama est\u00e9 aqu\u00ed"));
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
            case PERFECTO -> { player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.4f); Messages.send(player, "minijuego.calor_perfecto"); }
            case BUENO -> { player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f); Messages.send(player, "minijuego.calor_bueno"); }
            case FALLO -> { player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); Messages.send(player, "minijuego.calor_fallo"); }
        }

        if (session.isComplete()) finish();
        else render();
    }

    private void finish() {
        finished = true;
        Bukkit.getScheduler().cancelTask(session.getTaskId());
        MinigameLock.unlock(player.getUniqueId());
        player.closeInventory();

        double raw = Math.max(0, Math.min(100, session.getAccumulatedScore()));
        double score = Math.min(scoreCap, plugin.getProgressService().applyTo(player, "calentador", baseKey, raw));
        Material icon = com.customsmithing.util.BaseKey.iconFor(plugin, baseKey, Material.NETHERITE_SCRAP);
        ItemStack heated = plugin.getPieceCatalog().build(ProcessStage.CALENTADO, baseKey, displayName, icon, score);

        plugin.getProgressService().recordPiece(player, baseKey);
        plugin.getGiveService().give(player, heated);
        Messages.send(player, "procesos.calentado_completo");
        if (onSuccess != null) onSuccess.run();
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
