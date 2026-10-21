package com.customsmithing.bosses;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.util.IdValidator;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BossManager {

    private final CustomSmithing plugin;
    private final File file;
    private YamlConfiguration yml;
    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();

    public BossManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bosses.yml");
        load();
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("bosses.yml", false);
        yml = YamlConfiguration.loadConfiguration(file);
        bosses.clear();

        ConfigurationSection root = yml.getConfigurationSection("jefes");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String idProblem = IdValidator.problem(id);
            if (idProblem != null) {
                plugin.getLogger().warning("bosses.yml › jefes." + id + ": id no válido (" + idProblem + ") — jefe omitido.");
                continue;
            }
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                plugin.getLogger().warning("bosses.yml › jefes." + id + ": no es una sección — jefe omitido.");
                continue;
            }

            BossDefinition boss = new BossDefinition(id);
            boss.setDisplayName(sec.getString("nombre", id));
            String mobName = sec.getString("mob_base", "WITHER_SKELETON");
            try {
                boss.setEntityType(EntityType.valueOf(mobName));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("bosses.yml › jefes." + id + ".mob_base: '" + mobName
                        + "' no existe, se usa WITHER_SKELETON.");
            }
            boss.setHealthMultiplier(sec.getDouble("multiplicador_vida", 3.0));
            boss.setDamageMultiplier(sec.getDouble("multiplicador_dano", 2.0));
            boss.setSpeedMultiplier(sec.getDouble("multiplicador_velocidad", 1.2));

            EnumSet<BossAbility> abilities = EnumSet.noneOf(BossAbility.class);
            for (String a : sec.getStringList("habilidades")) {
                try {
                    abilities.add(BossAbility.valueOf(a));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("bosses.yml › jefes." + id + ".habilidades: '" + a
                            + "' no existe, se ignora esa habilidad.");
                }
            }
            boss.setAbilities(abilities);

            boss.setDropMineralId(sec.getString("drop_mineral_id", null));
            boss.setDropMineralAmount(sec.getInt("drop_mineral_cantidad", 1));
            boss.setDropMineralChance(sec.getDouble("drop_mineral_probabilidad", 100.0));
            boss.setDropBlueprintId(sec.getString("drop_plano_receta", null));
            boss.setDropBlueprintChance(sec.getDouble("drop_plano_probabilidad", 25.0));
            boss.setStructureId(sec.getString("estructura_id", null));

            bosses.put(id, boss);
        }
    }

    public void save(BossDefinition boss) {
        if (!IdValidator.isValid(boss.getId())) {
            throw new IllegalArgumentException("id de jefe no válido: " + boss.getId());
        }
        String base = "jefes." + boss.getId() + ".";
        yml.set(base + "nombre", boss.getDisplayName());
        yml.set(base + "mob_base", boss.getEntityType().name());
        yml.set(base + "multiplicador_vida", boss.getHealthMultiplier());
        yml.set(base + "multiplicador_dano", boss.getDamageMultiplier());
        yml.set(base + "multiplicador_velocidad", boss.getSpeedMultiplier());

        List<String> abilityNames = new ArrayList<>();
        for (BossAbility a : boss.getAbilities()) abilityNames.add(a.name());
        yml.set(base + "habilidades", abilityNames);

        yml.set(base + "drop_mineral_id", boss.getDropMineralId());
        yml.set(base + "drop_mineral_cantidad", boss.getDropMineralAmount());
        yml.set(base + "drop_mineral_probabilidad", boss.getDropMineralChance());
        yml.set(base + "drop_plano_receta", boss.getDropBlueprintId());
        yml.set(base + "drop_plano_probabilidad", boss.getDropBlueprintChance());
        yml.set(base + "estructura_id", boss.getStructureId());

        bosses.put(boss.getId(), boss);
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar bosses.yml: " + e.getMessage());
        }
    }

    public BossDefinition get(String id) { return bosses.get(id); }
    public boolean exists(String id) { return bosses.containsKey(id); }
    public Collection<BossDefinition> getAll() { return bosses.values(); }

    /** Spawnea el jefe en una ubicacion y lo marca con su id para que el listener lo detecte. */
    public LivingEntity spawn(String bossId, Location loc) {
        BossDefinition boss = bosses.get(bossId);
        if (boss == null || loc.getWorld() == null) return null;

        org.bukkit.entity.Entity spawned = loc.getWorld().spawnEntity(loc, boss.getEntityType());
        if (!(spawned instanceof LivingEntity entity)) {
            // mob_base que no es un ser vivo (antes: ClassCastException)
            spawned.remove();
            plugin.getLogger().warning("Jefe '" + bossId + "': " + boss.getEntityType() + " no es un ser vivo, no se invoca.");
            return null;
        }
        entity.setRemoveWhenFarAway(false);
        entity.setCustomName(boss.getDisplayName() != null ? boss.getDisplayName() : bossId);
        entity.setCustomNameVisible(true);

        var maxHealthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            double newMax = maxHealthAttr.getBaseValue() * boss.getHealthMultiplier();
            maxHealthAttr.setBaseValue(newMax);
            entity.setHealth(newMax);
        }
        var speedAttr = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * boss.getSpeedMultiplier());
        }
        var damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(damageAttr.getBaseValue() * boss.getDamageMultiplier());
        }

        entity.getPersistentDataContainer().set(SmithingKeys.BOSS_ID, PersistentDataType.STRING, bossId);
        track(entity);
        return entity;
    }

    /** Jefes vivos conocidos: las habilidades recorren solo este conjunto, no todas las entidades de los mundos. */
    private final java.util.Set<java.util.UUID> active = new java.util.HashSet<>();

    public void track(org.bukkit.entity.Entity entity) { active.add(entity.getUniqueId()); }
    public void untrack(java.util.UUID id) { active.remove(id); }
    public java.util.Set<java.util.UUID> getActive() { return active; }
}
