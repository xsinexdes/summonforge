package com.customsmithing.minerals;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.util.IdValidator;
import com.customsmithing.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomMineralManager {

    private final CustomSmithing plugin;
    private final File file;
    private YamlConfiguration yml;
    private final Map<String, CustomMineral> minerals = new LinkedHashMap<>();

    public CustomMineralManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "custom_minerals.yml");
        load();
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("custom_minerals.yml", false);
        yml = YamlConfiguration.loadConfiguration(file);
        minerals.clear();

        ConfigurationSection root = yml.getConfigurationSection("minerales");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String idProblem = IdValidator.problem(id);
            if (idProblem != null) {
                plugin.getLogger().warning("custom_minerals.yml › minerales." + id + ": id no válido ("
                        + idProblem + ") — mineral omitido.");
                continue;
            }
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                plugin.getLogger().warning("custom_minerals.yml › minerales." + id + ": no es una sección — mineral omitido.");
                continue;
            }
            CustomMineral mineral = new CustomMineral(id);
            mineral.setDisplayName(sec.getString("nombre", id));
            Material icon = Material.matchMaterial(sec.getString("icono", "NETHERITE_INGOT"));
            if (icon == null || !icon.isItem() || icon.isAir()) {
                plugin.getLogger().warning("custom_minerals.yml › minerales." + id + ".icono: '"
                        + sec.getString("icono") + "' no es un item válido, se usa NETHERITE_INGOT.");
                icon = Material.NETHERITE_INGOT;
            }
            mineral.setIconMaterial(icon);
            mineral.setBonusResistance(sec.getDouble("bonus_resistencia", 5.0));
            mineral.setBonusDamageFlat(sec.getDouble("bonus_dano", 1.0));
            minerals.put(id, mineral);
        }
    }

    public void save(CustomMineral mineral) {
        if (!IdValidator.isValid(mineral.getId())) {
            throw new IllegalArgumentException("id de mineral no válido: " + mineral.getId());
        }
        String base = "minerales." + mineral.getId() + ".";
        yml.set(base + "nombre", mineral.getDisplayName());
        yml.set(base + "icono", mineral.getIconMaterial().name());
        yml.set(base + "bonus_resistencia", mineral.getBonusResistance());
        yml.set(base + "bonus_dano", mineral.getBonusDamageFlat());
        minerals.put(mineral.getId(), mineral);
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar custom_minerals.yml: " + e.getMessage());
        }
    }

    public CustomMineral get(String id) { return minerals.get(id); }
    public boolean exists(String id) { return minerals.containsKey(id); }
    public Collection<CustomMineral> getAll() { return minerals.values(); }

    /** Crea el item "en bruto" de este mineral (lo que dropea el jefe, por ejemplo). */
    public ItemStack createRawItem(String mineralId, int amount) {
        CustomMineral mineral = minerals.get(mineralId);
        if (mineral == null) return null;
        ItemStack item = new ItemBuilder(mineral.getIconMaterial())
                .name("&5&l" + mineral.getDisplayName())
                .lore(List.of("&7Mineral raro. Se puede procesar", "&7igual que cualquier otro metal."))
                .amount(Math.max(1, amount))
                .build();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SmithingKeys.CUSTOM_MINERAL_ID, PersistentDataType.STRING, mineralId);
        item.setItemMeta(meta);
        return item;
    }

    /** Extrae el id de mineral custom de un item, si lo tiene. */
    public String getMineralId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SmithingKeys.CUSTOM_MINERAL_ID, PersistentDataType.STRING);
    }
}
