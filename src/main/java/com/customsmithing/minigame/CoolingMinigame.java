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
 * Minijuego de "Enfriado": temple del metal en el agua. UNIVERSAL por
 * material: el resultado sirve para cualquier receta que use ese metal.
 */
public class CoolingMinigame implements SmithingGUIHolder {

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
    public CoolingMinigame refund(ItemStack... items) {
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) refund.add(item.clone());
        }
        return this;
    }

    public CoolingMinigame(CustomSmithing plugin, Player player, String baseKey, String displayName, double previousScore) {
        this.plugin = plugin;
        this.player = player;
        this.baseKey = baseKey;
        this.displayName = displayName;
        this.previousScore = previousScore;

        int hits = 3;
        int perfectZone = plugin.getConfig().getInt("minijuego.zona_perfecta_slots", 1) + plugin.getUnlockService().extraPerfectSlots(player);
        int goodZone = plugin.getConfig().getInt("minijuego.zona_buena_slots", 2);
        this.session = new HammeringSession(hits, perfectZone, goodZone);
        this.inventory = Bukkit.createInventory(this, 9, "\u00a7b\u2744 Temple: sumerge en el momento justo");
    }

    public void start() {
        MinigameLock.lock(player.getUniqueId());
        render();
        player.openInventory(inventory);
        int intervalTicks = Math.max(1, plugin.getConfig().getInt("minijuego.duracion_ciclo_ticks", 34) / 12);
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
        if (dist <= session.getPerfectZoneSize()) { mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE; name = "\u00a7b\u00a7lTEMPLE PERFECTO"; }
        else if (dist <= session.getGoodZoneSize()) { mat = Material.CYAN_STAINED_GLASS_PANE; name = "\u00a73Aceptable"; }
        else { mat = Material.GRAY_STAINED_GLASS_PANE; name = "\u00a77Momento incorrecto"; }

        if (index == session.getCursorPosition()) { mat = Material.WATER_BUCKET; name = "\u00a79\u00a7l\u25BC SUMERGIR AHORA \u25BC"; }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("\u00a77Clic para sumergir la pieza"));
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
            case PERFECTO -> { player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.5f); Messages.send(player, "minijuego.temple_perfecto"); }
            case BUENO -> { player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f); Messages.send(player, "minijuego.temple_bueno"); }
            case FALLO -> { player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); Messages.send(player, "minijuego.temple_fallo"); }
        }

        if (session.isComplete()) finish();
        else render();
    }

    private void finish() {
        finished = true;
        Bukkit.getScheduler().cancelTask(session.getTaskId());
        MinigameLock.unlock(player.getUniqueId());
        player.closeInventory();

        double thisScore = Math.max(0, Math.min(100, session.getAccumulatedScore()));
        double envMod = plugin.getEnvironmentModifier().calculateModifier(player.getLocation());
        double blended = ((previousScore + thisScore) / 2.0) + envMod;
        double finalScore = plugin.getProgressService().applyTo(player, "enfriado", baseKey, Math.max(0, Math.min(100, blended)));

        ItemStack cooled = plugin.getPieceCatalog().build(ProcessStage.ENFRIADO, baseKey, displayName,
                com.customsmithing.util.BaseKey.iconFor(plugin, baseKey, Material.IRON_NUGGET), finalScore);

        plugin.getProgressService().recordPiece(player, baseKey);
        plugin.getGiveService().give(player, cooled);
        Messages.send(player, "procesos.enfriado_completo");
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
