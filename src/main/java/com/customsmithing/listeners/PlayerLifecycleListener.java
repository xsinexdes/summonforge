package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.blueprints.BlueprintManager;
import com.customsmithing.gui.GuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Random;

public class PlayerLifecycleListener implements Listener {

    private final CustomSmithing plugin;
    private final Random random = new Random();

    public PlayerLifecycleListener(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().get(event.getPlayer()); // precarga los datos
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Si tenía un menú abierto (Encantamiento, minijuegos...) se le devuelven sus items ANTES de
        // que se guarde su inventario. Es seguro llamarlo aunque el cierre normal también corra.
        GuiService.cleanup(event.getPlayer(), false);
        plugin.getAttackSpeedTask().clear(event.getPlayer());
        plugin.getSmithingCommand().forget(event.getPlayer().getUniqueId());
        plugin.getTestBench().undoStations(event.getPlayer());
        plugin.getPlayerDataManager().unload(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Un menú o minijuego abierto no debe sobrevivir a un cambio de mundo: se cierra devolviendo lo consumido.
        GuiService.cleanup(event.getPlayer(), true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        GuiService.cleanup(event.getEntity(), true);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        BlueprintManager.MobDrop drop = plugin.getBlueprintManager().getMobDrops().get(event.getEntityType());
        if (drop == null || drop.recipeId == null) return;
        if (!plugin.getRecipeManager().exists(drop.recipeId)) return;

        if (random.nextDouble() * 100 < drop.chance) {
            plugin.getBlueprintManager().giveBlueprint(killer, drop.recipeId);
        }
    }
}
