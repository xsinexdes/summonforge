package com.customsmithing.forge;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ItemStats;
import com.customsmithing.data.Quality;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.item.ItemRef;
import com.customsmithing.material.MaterialRegistry;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.ItemBuilder;
import com.customsmithing.util.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Desguace (devuelve parte de los materiales según la calidad) y reforja (vuelve a tirar los stats con un costo y un
 * límite por objeto) de lo que se fabricó con este plugin. Se usan con el objeto en la mano principal.
 */
public final class ForgeService {

    private final CustomSmithing plugin;

    public ForgeService(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    /** Receta y calidad del objeto de la mano, o null (ya avisó al jugador) si no es un objeto forjado válido. */
    private record Forged(ItemStack item, SmithingRecipe recipe, Quality quality) {
    }

    private Forged forgedInHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir() || !hand.hasItemMeta() || hand.getItemMeta() == null) {
            Messages.send(player, "forja.sin_objeto");
            return null;
        }
        PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
        String recipeId = pdc.get(SmithingKeys.RECIPE_ID, PersistentDataType.STRING);
        Quality quality = Quality.parse(pdc.get(SmithingKeys.QUALITY, PersistentDataType.STRING));
        SmithingRecipe recipe = recipeId == null ? null : plugin.getRecipeManager().get(recipeId);
        if (recipe == null || quality == null) {
            Messages.send(player, "forja.sin_receta");
            return null;
        }
        return new Forged(hand, recipe, quality);
    }

    // ---------------------------------------------------------------- desguace

    public void salvage(Player player) {
        if (!plugin.getConfig().getBoolean("desguace.activar", true)) {
            Messages.send(player, "forja.desactivado");
            return;
        }
        Forged forged = forgedInHand(player);
        if (forged == null) return;

        double percent = plugin.getConfig().getDouble("desguace.porcentaje_base", 50);
        if (plugin.getConfig().getBoolean("desguace.segun_calidad", true)) {
            percent *= plugin.getQualityConfig().multiplier(forged.quality());
        }
        percent = Math.max(0, Math.min(100, percent));

        List<ItemStack> returned = new ArrayList<>();
        for (Map.Entry<Material, Integer> ingredient : forged.recipe().getIngredients().entrySet()) {
            int amount = (int) Math.floor(ingredient.getValue() * percent / 100.0);
            if (amount > 0) returned.add(new ItemStack(ingredient.getKey(), amount));
        }
        if (plugin.getConfig().getBoolean("desguace.devuelve_metal", true) && forged.recipe().getMetalBaseKey() != null) {
            MaterialRegistry.Entry metal = plugin.getMaterialRegistry().forBaseKey(forged.recipe().getMetalBaseKey());
            if (metal != null) {
                String key = forged.recipe().getMetalBaseKey();
                if (key.startsWith("MIN:")) returned.add(plugin.getCustomMineralManager().createRawItem(metal.id(), 1));
                else returned.add(new ItemStack(metal.item(), 1));
            }
        }

        // Se quita UNA unidad del objeto y recién entonces se entrega (nunca se entrega sin haber quitado).
        ItemStack hand = forged.item();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);

        plugin.getGiveService().giveAll(player, returned);
        Messages.send(player, "forja.desguace_ok", "%porcentaje%", String.valueOf(Math.round(percent)));
    }

    // ---------------------------------------------------------------- reforja

    public void reforge(Player player) {
        if (!plugin.getConfig().getBoolean("reforja.activar", true)) {
            Messages.send(player, "forja.desactivado");
            return;
        }
        Forged forged = forgedInHand(player);
        if (forged == null) return;
        ItemStack hand = forged.item();
        if (hand.getAmount() != 1) {
            Messages.send(player, "forja.una_unidad");
            return;
        }

        ItemMeta meta = hand.getItemMeta();
        int done = meta.getPersistentDataContainer().getOrDefault(SmithingKeys.REFORGES, PersistentDataType.INTEGER, 0);
        int limit = Math.max(0, plugin.getConfig().getInt("reforja.limite", 3));
        if (done >= limit) {
            Messages.send(player, "forja.reforja_limite", "%limite%", String.valueOf(limit));
            return;
        }

        Material costMaterial = Material.matchMaterial(plugin.getConfig().getString("reforja.costo_material", "DIAMOND"));
        int costAmount = Math.max(0, plugin.getConfig().getInt("reforja.costo_cantidad", 1));
        if (costMaterial != null && costMaterial.isItem() && !costMaterial.isAir() && costAmount > 0
                && !ItemRef.of(costMaterial).remove(player.getInventory(), costAmount)) {
            Messages.send(player, "forja.reforja_costo", "%cantidad%", String.valueOf(costAmount),
                    "%material%", com.customsmithing.util.Text.prettify(costMaterial.name()));
            return;
        }

        ItemStats stats = StatRoller.roll(plugin, player, forged.recipe(), forged.quality());
        ItemStack rebuilt = new ItemBuilder(hand).applyQualityAndStats(forged.quality(), stats).build();
        ItemMeta rebuiltMeta = rebuilt.getItemMeta();
        rebuiltMeta.getPersistentDataContainer().set(SmithingKeys.REFORGES, PersistentDataType.INTEGER, done + 1);
        hand.setItemMeta(rebuiltMeta);
        player.getInventory().setItemInMainHand(hand);

        Messages.send(player, "forja.reforja_ok", "%hechas%", String.valueOf(done + 1), "%limite%", String.valueOf(limit));
    }
}
