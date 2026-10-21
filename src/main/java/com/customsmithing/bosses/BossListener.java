package com.customsmithing.bosses;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class BossListener implements Listener {

    private final CustomSmithing plugin;
    private final Random random = new Random();
    private int taskId = -1;

    public BossListener(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    private static final String MINION_TAG = "customsmithing_minion";

    public void start() {
        stop();
        scanLoadedBosses();
        taskId = org.bukkit.Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickAbilities, 100L, 100L);
    }

    public void stop() {
        if (taskId != -1) org.bukkit.Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    /** Registra los jefes que ya estaban en el mundo (p. ej. tras un reinicio). */
    private void scanLoadedBosses() {
        for (World world : org.bukkit.Bukkit.getWorlds()) {
            for (LivingEntity living : world.getLivingEntities()) {
                if (living.getPersistentDataContainer().has(SmithingKeys.BOSS_ID, PersistentDataType.STRING)) {
                    plugin.getBossManager().track(living);
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(org.bukkit.event.world.EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living
                    && living.getPersistentDataContainer().has(SmithingKeys.BOSS_ID, PersistentDataType.STRING)) {
                plugin.getBossManager().track(living);
            }
        }
    }

    private void tickAbilities() {
        double range = plugin.getConfig().getDouble("jefes.radio_habilidades", 20);
        var iterator = plugin.getBossManager().getActive().iterator();
        while (iterator.hasNext()) {
            Entity entity = org.bukkit.Bukkit.getEntity(iterator.next());
            // Si el chunk del jefe está descargado no se puede resolver: se conserva y se reintenta luego.
            if (entity == null) continue;
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                iterator.remove();
                continue;
            }
            String bossId = living.getPersistentDataContainer().get(SmithingKeys.BOSS_ID, PersistentDataType.STRING);
            if (bossId == null) { iterator.remove(); continue; }
            BossDefinition boss = plugin.getBossManager().get(bossId);
            if (boss == null || boss.getAbilities().isEmpty()) continue;

            // Sin ningún jugador cerca no tiene sentido gastar habilidades ni crear súbditos.
            if (living.getWorld().getNearbyEntities(living.getLocation(), range, range, range, e -> e instanceof Player).isEmpty()) continue;

            BossAbility[] abilities = boss.getAbilities().toArray(new BossAbility[0]);
            triggerAbility(living, abilities[random.nextInt(abilities.length)]);
        }
    }

    private void triggerAbility(LivingEntity boss, BossAbility ability) {
        Location loc = boss.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        switch (ability) {
            case FIRE_PULSE -> {
                world.spawnParticle(Particle.FLAME, loc, 40, 2, 1, 2);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                for (Entity nearby : world.getNearbyEntities(loc, 4, 3, 4)) {
                    if (nearby instanceof Player player) {
                        player.setFireTicks(60);
                    }
                }
            }
            case SPEED_DASH -> {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 3));
                world.spawnParticle(Particle.CLOUD, loc, 20, 0.5, 0.5, 0.5);
                world.playSound(loc, Sound.ENTITY_HORSE_GALLOP, 1f, 1.2f);
            }
            case EXPLOSION_SLAM -> {
                world.createExplosion(loc, 0f, false, false, boss);
                world.spawnParticle(Particle.EXPLOSION, loc, 3);
                for (Entity nearby : world.getNearbyEntities(loc, 3, 2, 3)) {
                    if (nearby instanceof Player player) {
                        player.damage(4.0, boss);
                    }
                }
            }
            case SUMMON_MINIONS -> {
                EntityType minionType = boss.getType() == EntityType.WITHER_SKELETON ? EntityType.SKELETON : EntityType.ZOMBIE;
                int cap = plugin.getConfig().getInt("jefes.max_subditos", 6);
                long alive = world.getNearbyEntities(loc, 24, 24, 24, e -> e.getScoreboardTags().contains(MINION_TAG)).size();
                for (int i = 0; i < 2 && alive < cap; i++, alive++) {
                    Location spawnAt = loc.clone().add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    Entity minion = world.spawnEntity(spawnAt, minionType);
                    minion.addScoreboardTag(MINION_TAG);
                }
                world.playSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String bossId = entity.getPersistentDataContainer().get(SmithingKeys.BOSS_ID, PersistentDataType.STRING);
        if (bossId == null) return;
        plugin.getBossManager().untrack(entity.getUniqueId());
        BossDefinition boss = plugin.getBossManager().get(bossId);
        if (boss == null) return;

        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        if (boss.getDropMineralId() != null && random.nextDouble() * 100 < boss.getDropMineralChance()) {
            var item = plugin.getCustomMineralManager().createRawItem(boss.getDropMineralId(), boss.getDropMineralAmount());
            if (item != null) world.dropItemNaturally(loc, item);
        }

        if (boss.getDropBlueprintId() != null && plugin.getRecipeManager().exists(boss.getDropBlueprintId())
                && random.nextDouble() * 100 < boss.getDropBlueprintChance()) {
            var item = plugin.getBlueprintManager().createBlueprintItem(boss.getDropBlueprintId());
            world.dropItemNaturally(loc, item);
        }

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 60, 1, 1, 1);
        world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 1f, 1f);
    }
}
