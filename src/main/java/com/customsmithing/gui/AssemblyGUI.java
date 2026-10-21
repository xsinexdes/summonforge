package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.admin.TestBench;
import com.customsmithing.data.ItemStats;
import com.customsmithing.data.Passives;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Quality;
import com.customsmithing.forge.StatRoller;
import com.customsmithing.item.ItemRef;
import com.customsmithing.item.PieceData;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.BaseKey;
import com.customsmithing.util.ItemBuilder;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AssemblyGUI extends PagedMenu<SmithingRecipe> {

    public AssemblyGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, "\u2699 Mesa de Ensamblaje");
    }

    /** Solo las recetas ya desbloqueadas (Árbol de Recetas o plano investigado). */
    @Override
    protected List<SmithingRecipe> entries() {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        List<SmithingRecipe> list = new ArrayList<>();
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            if (recipe.getId().startsWith(TestBench.TEST_PREFIX) && !player.hasPermission("customsmithing.admin")) continue;
            if (data.hasLearned(recipe.getId())) list.add(recipe);
        }
        return list;
    }

    @Override
    protected String emptyText() {
        return "Aún no has desbloqueado ninguna receta. Usa el Árbol de Recetas.";
    }

    @Override
    protected ItemStack entryIcon(SmithingRecipe recipe) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        boolean hasLevel = data.getLevel() >= recipe.getRequiredLevel();

        ItemStack icon = new ItemStack(recipe.getResultMaterial());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName((hasLevel ? "\u00a7a" : "\u00a7c") + recipe.getResultName());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        List<String> lore = new ArrayList<>();
        lore.add("\u00a77Nivel requerido: " + (hasLevel ? "\u00a7a" : "\u00a7c") + recipe.getRequiredLevel());
        lore.add("\u00a77Procesos requeridos:");
        for (ProcessStage step : recipe.getSteps()) {
            lore.add("\u00a78 - " + step.getIcon() + " " + step.getLabel());
        }
        if (!recipe.getIngredients().isEmpty()) {
            lore.add(" ");
            lore.add("\u00a77Materiales:");
            for (var ing : recipe.getIngredients().entrySet()) {
                int have = ItemRef.of(ing.getKey()).count(player.getInventory());
                boolean enough = have >= ing.getValue();
                lore.add((enough ? "\u00a7a\u2714 " : "\u00a7c\u2716 ") + Text.prettify(ing.getKey().name()) + " \u00a77(" + have + "/" + ing.getValue() + ")");
            }
        }
        boolean needsMetal = recipe.requiresStep(ProcessStage.GOLPEADO) || recipe.requiresStep(ProcessStage.ENFRIADO);
        if (needsMetal && recipe.getMetalBaseKey() != null) {
            ProcessStage state = recipe.requiresStep(ProcessStage.ENFRIADO) ? ProcessStage.ENFRIADO : ProcessStage.GOLPEADO;
            boolean hasPiece = findProcessedPiece(recipe.getMetalBaseKey(), state) != null;
            lore.add((hasPiece ? "\u00a7a\u2714 " : "\u00a7c\u2716 ") + "Pieza de " + BaseKey.displayName(plugin, recipe.getMetalBaseKey()) + " procesada");
        }
        if (recipe.requiresStep(ProcessStage.TALLADO) && recipe.getWoodMaterial() != null) {
            boolean hasPiece = findProcessedPiece(BaseKey.ofMaterial(recipe.getWoodMaterial()), ProcessStage.TALLADO) != null;
            lore.add((hasPiece ? "\u00a7a\u2714 " : "\u00a7c\u2716 ") + "Pieza tallada de " + Text.prettify(recipe.getWoodMaterial().name()) + " (Cortapiedras)");
        }
        if (recipe.requiresStep(ProcessStage.TEJIDO) && recipe.getWoodMaterial() != null) {
            boolean hasPiece = findProcessedPiece(BaseKey.ofMaterial(recipe.getWoodMaterial()), ProcessStage.TEJIDO) != null;
            lore.add((hasPiece ? "\u00a7a\u2714 " : "\u00a7c\u2716 ") + "Pieza tejida de " + Text.prettify(recipe.getWoodMaterial().name()) + " (Telar)");
        }
        lore.add(" ");
        lore.add(hasLevel ? "\u00a7eClic para ensamblar" : "\u00a7cNo disponible: falta nivel");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    /** Clic = 1 · Shift+clic = lote pequeño · clic derecho = todo lo que alcance (hasta el máximo configurado). */
    @Override
    protected void onEntryClick(SmithingRecipe recipe, InventoryClickEvent event) {
        int times = 1;
        if (event.getClick().isRightClick()) times = Math.max(1, plugin.getConfig().getInt("fabricacion.lote_maximo", 16));
        else if (event.getClick().isShiftClick()) times = Math.max(1, plugin.getConfig().getInt("fabricacion.lote_shift", 5));

        int made = 0;
        for (int i = 0; i < times; i++) {
            if (!attemptAssembly(recipe, times == 1)) break;
            made++;
        }
        if (times > 1 && made > 0) {
            Messages.send(player, "procesos.ensamblaje_lote", "%cantidad%", String.valueOf(made), "%receta%", recipe.getResultName());
        }
        render();
    }

    /** Añade la firma del herrero (y la fecha) al final del lore, fuera del bloque que regenera el refinado. */
    private ItemStack signed(ItemStack item) {
        if (!plugin.getConfig().getBoolean("firma.activar", true)) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add(" ");
        String date = plugin.getConfig().getBoolean("firma.mostrar_fecha", true)
                ? " \u00a78\u2022 " + new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()) : "";
        lore.add("\u00a78Forjado por \u00a77" + player.getName() + date);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** @return true si se fabricó un objeto. {@code announce}: mostrar el mensaje de éxito (en lote solo al final). */
    private boolean attemptAssembly(SmithingRecipe recipe, boolean announce) {
        PlayerData data = plugin.getPlayerDataManager().get(player);

        if (data.getLevel() < recipe.getRequiredLevel()) {
            Messages.send(player, "recetas.nivel_insuficiente", "%nivel%", String.valueOf(recipe.getRequiredLevel()));
            return false;
        }
        if (!data.hasLearned(recipe.getId())) {
            Messages.send(player, "desbloqueo.sin_desbloquear");
            return false;
        }

        // Determina que "estados finales" de pieza procesada requiere esta receta,
        // y de que material especifico (definido en el editor de recetas), ya que
        // las piezas procesadas son universales por material, no por receta.
        List<ItemStack> foundPieces = new ArrayList<>();

        boolean needsMetal = recipe.requiresStep(ProcessStage.GOLPEADO) || recipe.requiresStep(ProcessStage.ENFRIADO);
        if (needsMetal) {
            if (recipe.getMetalBaseKey() == null) {
                Messages.send(player, "recetas.sin_metal");
                return false;
            }
            ProcessStage state = recipe.requiresStep(ProcessStage.ENFRIADO) ? ProcessStage.ENFRIADO : ProcessStage.GOLPEADO;
            ItemStack piece = findProcessedPiece(recipe.getMetalBaseKey(), state);
            if (piece == null) {
                Messages.send(player, "recetas.ingredientes_faltantes");
                return false;
            }
            foundPieces.add(piece);
        }

        if (recipe.requiresStep(ProcessStage.TALLADO) || recipe.requiresStep(ProcessStage.TEJIDO)) {
            if (recipe.getWoodMaterial() == null) {
                Messages.send(player, "recetas.sin_madera");
                return false;
            }
            ProcessStage state = recipe.requiresStep(ProcessStage.TEJIDO) ? ProcessStage.TEJIDO : ProcessStage.TALLADO;
            ItemStack piece = findProcessedPiece(BaseKey.ofMaterial(recipe.getWoodMaterial()), state);
            if (piece == null) {
                Messages.send(player, "recetas.ingredientes_faltantes");
                return false;
            }
            foundPieces.add(piece);
        }

        // Verifica TODOS los ingredientes antes de consumir nada. Se cuentan con la misma regla
        // (ItemRef) con la que luego se consumen, así que nunca se cobra de más ni de menos.
        for (var entry : recipe.getIngredients().entrySet()) {
            if (ItemRef.of(entry.getKey()).count(player.getInventory()) < entry.getValue()) {
                Messages.send(player, "recetas.ingredientes_faltantes");
                return false;
            }
        }

        // Pasiva de maestría: probabilidad de NO gastar los ingredientes (por nodo alcanzado del árbol).
        double saveChance = plugin.getConfig().getDouble("habilidades.ahorro_material_por_nivel", 3)
                * Passives.tiersReached(data.getLevel(), plugin.getConfig().getIntegerList("habilidades.niveles"));
        boolean saved = !recipe.getIngredients().isEmpty() && Math.random() * 100 < saveChance;
        if (saved) Messages.send(player, "procesos.material_ahorrado");

        for (var entry : recipe.getIngredients().entrySet()) {
            if (saved) break;
            if (!ItemRef.of(entry.getKey()).remove(player.getInventory(), entry.getValue())) {
                // No debería pasar (ya se verificó arriba, y todo ocurre en el mismo tick).
                plugin.getLogger().warning("Ensamblaje: no se pudo consumir " + entry.getKey() + " x" + entry.getValue()
                        + " a " + player.getName() + " tras verificarlo.");
                Messages.send(player, "recetas.ingredientes_faltantes");
                return false;
            }
        }

        double baseScore = 50.0;
        if (!foundPieces.isEmpty()) {
            double sum = 0;
            for (ItemStack piece : foundPieces) {
                PieceData.Data pieceData = PieceData.read(piece);
                sum += pieceData != null ? pieceData.score() : PieceData.DEFAULT_SCORE;
                piece.setAmount(piece.getAmount() - 1);
            }
            baseScore = sum / foundPieces.size();
        }

        Quality quality = plugin.getQualityConfig().tierFor(baseScore);
        ItemStats stats = StatRoller.roll(plugin, player, recipe, quality);

        ItemStack result = new ItemBuilder(recipe.getResultMaterial())
                .name("&f" + recipe.getResultName())
                .markSmithingItem()
                .recipeId(recipe.getId())
                .applyQualityAndStats(quality, stats)
                .build();

        plugin.getGiveService().give(player, signed(result));
        if (announce) Messages.send(player, "procesos.ensamblaje_completo", "%calidad%", quality.formatted());

        data.incrementCrafts();
        boolean leveled = data.addXp(plugin.getConfig().getInt("experiencia.xp_por_objeto_terminado", 25));
        if (leveled) Messages.send(player, "nivel.subida", "%nivel%", String.valueOf(data.getLevel()));

        plugin.getQuestManager().onItemCrafted(player, recipe, quality);
        return true;
    }

    private ItemStack findProcessedPiece(String baseKey, ProcessStage requiredState) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            PieceData.Data pieceData = PieceData.read(item);
            if (pieceData != null && baseKey.equals(pieceData.baseKey()) && pieceData.stage() == requiredState) {
                return item;
            }
        }
        return null;
    }

}
