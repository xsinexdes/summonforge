package com.customsmithing.admin;

import com.customsmithing.CustomSmithing;
import com.customsmithing.alloys.AlloyRecipe;
import com.customsmithing.data.ItemStats;
import com.customsmithing.data.MoldType;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Quality;
import com.customsmithing.data.Specialization;
import com.customsmithing.item.Molds;
import com.customsmithing.item.MagicBooks;
import com.customsmithing.material.MaterialRegistry;
import com.customsmithing.minerals.CustomMineral;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.BaseKey;
import com.customsmithing.util.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Banco de pruebas para administradores: entrega kits para recorrer cada estación, coloca/quita todas las
 * estaciones en fila y instala/quita recetas de prueba (una por cada metal y mineral) para comprobar que
 * "todo material funciona" en un par de minutos.
 */
public final class TestBench {

    public enum Kit { METALES, MADERAS_TELAS, ALEACION, ENCANTAMIENTO, PLANOS, MOLDES, REFINADO }

    public static final String TEST_PREFIX = "prueba_";

    private static final String[] STATION_KEYS = {"fundicion", "calentador", "forja", "enfriado", "ensamblaje",
            "refinado", "mesa_planos", "aleacion", "telar", "cortapiedras", "encantamiento"};
    private static final Material[] STATION_DEFAULTS = {Material.FURNACE, Material.BLAST_FURNACE, Material.ANVIL,
            Material.WATER_CAULDRON, Material.SMITHING_TABLE, Material.GRINDSTONE, Material.LECTERN, Material.SMOKER,
            Material.LOOM, Material.STONECUTTER, Material.ENCHANTING_TABLE};

    private final CustomSmithing plugin;
    private final Map<UUID, List<Location>> placed = new HashMap<>();

    public TestBench(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- kits

    public void give(Player player, Kit kit) {
        List<ItemStack> items = new ArrayList<>();
        switch (kit) {
            case METALES -> metalKit(items);
            case MADERAS_TELAS -> carvingKit(items);
            case ALEACION -> {
                for (AlloyRecipe alloy : plugin.getAlloyManager().getAll()) {
                    items.add(alloy.getInput().toItemStack(Math.min(64, alloy.getInputAmount() * 2)));
                }
            }
            case ENCANTAMIENTO -> {
                items.add(new ItemStack(Material.IRON_SWORD));
                items.add(new ItemStack(Material.IRON_CHESTPLATE));
                items.add(MagicBooks.create(Enchantment.SHARPNESS, 3));
                items.add(MagicBooks.create(Enchantment.PROTECTION, 3));
            }
            case PLANOS -> {
                int count = 0;
                for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
                    if (!recipe.isRequiresBlueprint() || count >= 5) continue;
                    items.add(plugin.getBlueprintManager().createBlueprintItem(recipe.getId()));
                    count++;
                }
            }
            case MOLDES -> {
                for (MoldType type : MoldType.values()) items.add(Molds.create(plugin, type, Molds.defaultUses(plugin)));
                for (com.customsmithing.data.ToolType tool : com.customsmithing.data.ToolType.values()) {
                    items.add(com.customsmithing.item.Tools.create(plugin, tool, com.customsmithing.item.Tools.defaultUses(plugin)));
                }
            }
            case REFINADO -> {
                for (Material base : new Material[]{Material.IRON_SWORD, Material.IRON_CHESTPLATE}) {
                    items.add(new ItemBuilder(base).name("&fPieza de prueba").markSmithingItem()
                            .applyQualityAndStats(Quality.RARO, new ItemStats()).build());
                }
            }
        }
        plugin.getGiveService().giveAll(player, items);
    }

    public void giveAll(Player player) {
        for (Kit kit : Kit.values()) give(player, kit);
    }

    private void metalKit(List<ItemStack> items) {
        MaterialRegistry registry = plugin.getMaterialRegistry();
        items.add(new ItemStack(Material.COAL, 16));
        for (MaterialRegistry.Smelt smelt : registry.smelts()) items.add(new ItemStack(smelt.input(), 4));
        int shown = 0;
        for (MaterialRegistry.Entry entry : registry.entries()) {
            if (entry.kind() != MaterialRegistry.Kind.METAL || shown >= 5) continue;
            shown++;
            items.add(new ItemStack(entry.item(), 4));
            String baseKey = BaseKey.ofMaterial(entry.item());
            String name = BaseKey.displayName(plugin, baseKey);
            // una pieza ya procesada en cada etapa, para probar Forja, Enfriado y Ensamblaje directamente
            items.add(piece(entry.item(), baseKey, ProcessStage.CALENTADO, "&cLingote Calentado de " + name));
            items.add(piece(entry.item(), baseKey, ProcessStage.GOLPEADO, "&6Pieza Golpeada de " + name));
            items.add(piece(entry.item(), baseKey, ProcessStage.ENFRIADO, "&bPieza Enfriada de " + name));
        }
        for (CustomMineral mineral : plugin.getCustomMineralManager().getAll()) {
            items.add(plugin.getCustomMineralManager().createRawItem(mineral.getId(), 4));
        }
    }

    private ItemStack piece(Material material, String baseKey, ProcessStage stage, String name) {
        return new ItemBuilder(material).name(name).markSmithingItem().piece(stage, baseKey, 75.0).build();
    }

    private void carvingKit(List<ItemStack> items) {
        int woods = 0;
        int cloths = 0;
        for (MaterialRegistry.Entry entry : plugin.getMaterialRegistry().entries()) {
            if (entry.kind() == MaterialRegistry.Kind.MADERA && woods < 3) { items.add(new ItemStack(entry.item(), 8)); woods++; }
            if (entry.kind() == MaterialRegistry.Kind.TELA && cloths < 3) { items.add(new ItemStack(entry.item(), 8)); cloths++; }
        }
    }

    // ---------------------------------------------------------------- estaciones en fila

    /** Coloca todas las estaciones en una fila delante del jugador (solo sobre aire). @return cuántas colocó */
    public int placeStations(Player player) {
        undoStations(player);
        Vector dir = player.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1e-6) dir = new Vector(0, 0, 1);
        dir.normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        Location base = player.getLocation().getBlock().getLocation().add(dir.clone().multiply(3));

        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < STATION_KEYS.length; i++) {
            Material material = Material.matchMaterial(plugin.getConfig().getString("estaciones." + STATION_KEYS[i], STATION_DEFAULTS[i].name()));
            if (material == null || !material.isBlock()) material = STATION_DEFAULTS[i];
            Location at = base.clone().add(right.clone().multiply((i - STATION_KEYS.length / 2.0) * 2)).getBlock().getLocation();
            Block block = at.getBlock();
            if (!block.getType().isAir()) continue; // nunca pisa construcciones
            block.setType(material);
            locations.add(at);
        }
        placed.put(player.getUniqueId(), locations);
        return locations.size();
    }

    /** Quita las estaciones colocadas (solo si el bloque sigue siendo el que se colocó). @return cuántas quitó */
    public int undoStations(Player player) {
        List<Location> locations = placed.remove(player.getUniqueId());
        if (locations == null) return 0;
        int removed = 0;
        for (Location at : locations) {
            Block block = at.getBlock();
            if (isStationMaterial(block.getType())) {
                block.setType(Material.AIR);
                removed++;
            }
        }
        return removed;
    }

    private boolean isStationMaterial(Material type) {
        for (int i = 0; i < STATION_KEYS.length; i++) {
            Material configured = Material.matchMaterial(plugin.getConfig().getString("estaciones." + STATION_KEYS[i], STATION_DEFAULTS[i].name()));
            if (type == STATION_DEFAULTS[i] || type == configured) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- recetas de prueba

    private static Material swordFor(Material metal) {
        return switch (metal) {
            case GOLD_INGOT -> Material.GOLDEN_SWORD;
            case DIAMOND -> Material.DIAMOND_SWORD;
            case NETHERITE_INGOT -> Material.NETHERITE_SWORD;
            case COPPER_INGOT -> Material.STONE_SWORD;
            default -> Material.IRON_SWORD;
        };
    }

    private static String testId(String id) {
        String full = TEST_PREFIX + id;
        return full.length() > 32 ? full.substring(0, 32) : full;
    }

    /** Crea una receta de prueba por cada metal y mineral y la desbloquea para el jugador. @return cuántas */
    public int installTestRecipes(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        int count = 0;
        for (MaterialRegistry.Entry entry : plugin.getMaterialRegistry().entries()) {
            if (entry.kind() != MaterialRegistry.Kind.METAL) continue;
            SmithingRecipe recipe = baseTestRecipe(testId(entry.id()), swordFor(entry.item()), "Espada de prueba (" + entry.id() + ")");
            recipe.setMetalMaterial(entry.item());
            plugin.getRecipeManager().save(recipe);
            data.learnRecipe(recipe.getId());
            count++;
        }
        for (CustomMineral mineral : plugin.getCustomMineralManager().getAll()) {
            SmithingRecipe recipe = baseTestRecipe(testId(mineral.getId()), Material.IRON_SWORD, "Espada de prueba (" + mineral.getId() + ")");
            recipe.setMetalCustomMineralId(mineral.getId());
            plugin.getRecipeManager().save(recipe);
            data.learnRecipe(recipe.getId());
            count++;
        }
        plugin.getPlayerDataManager().save(player);
        return count;
    }

    private SmithingRecipe baseTestRecipe(String id, Material result, String name) {
        SmithingRecipe recipe = new SmithingRecipe(id);
        recipe.setResultMaterial(result);
        recipe.setResultName(name);
        recipe.setRequiredLevel(1);
        recipe.setRequiresBlueprint(false);
        recipe.setDifficulty(1.0);
        recipe.setSpecialization(Specialization.ESPADAS);
        return recipe;
    }

    /** Borra todas las recetas cuyo id empieza por "prueba_". @return cuántas borró */
    public int removeTestRecipes() {
        List<String> ids = new ArrayList<>();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            if (recipe.getId().startsWith(TEST_PREFIX)) ids.add(recipe.getId());
        }
        for (String id : ids) plugin.getRecipeManager().delete(id);
        return ids.size();
    }
}
