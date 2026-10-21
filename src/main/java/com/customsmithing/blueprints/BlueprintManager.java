package com.customsmithing.blueprints;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.ItemBuilder;
import com.customsmithing.util.Messages;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlueprintManager {

    private final CustomSmithing plugin;
    private final File file;
    private YamlConfiguration yml;

    public static class MobDrop {
        public final String recipeId;
        public final double chance;
        public MobDrop(String recipeId, double chance) { this.recipeId = recipeId; this.chance = chance; }
    }

    private final Map<EntityType, MobDrop> mobDrops = new HashMap<>();

    public BlueprintManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "blueprints.yml");
        load();
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("blueprints.yml", false);
        yml = YamlConfiguration.loadConfiguration(file);
        mobDrops.clear();

        ConfigurationSection sec = yml.getConfigurationSection("drops_mobs");
        if (sec != null) {
            for (String mobName : sec.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(mobName);
                    String recipeId = sec.getString(mobName + ".receta");
                    double chance = sec.getDouble(mobName + ".probabilidad", 1.0);
                    mobDrops.put(type, new MobDrop(recipeId, chance));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public Map<EntityType, MobDrop> getMobDrops() {
        return mobDrops;
    }

    public ItemStack createBlueprintItem(String recipeId) {
        SmithingRecipe recipe = plugin.getRecipeManager().get(recipeId);
        String displayName = recipe != null ? recipe.getResultName() : recipeId;
        return new ItemBuilder(Material.PAPER)
                .name("&d&lPlano: &f" + displayName)
                .lore(List.of("&7Lleva este plano a la mesa de planos", "&7para desbloquear la receta.", "&8ID: " + recipeId))
                .blueprintFor(recipeId)
                .build();
    }

    public void giveBlueprint(Player player, String recipeId) {
        ItemStack item = createBlueprintItem(recipeId);
        plugin.getGiveService().give(player, item);
        Messages.send(player, "planos.recibido", "%receta%", recipeId);
    }

    public boolean hasBlueprint(Player player, String recipeId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            String id = meta.getPersistentDataContainer().get(SmithingKeys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
            if (recipeId.equals(id)) return true;
        }
        return false;
    }
}
