package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ItemStats;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.Quality;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.gui.AssemblyGUI;
import com.customsmithing.gui.BlueprintTableGUI;
import com.customsmithing.item.ItemRef;
import com.customsmithing.item.PieceData;
import com.customsmithing.material.MaterialRegistry;
import com.customsmithing.minigame.HammeringMinigame;
import com.customsmithing.minigame.MinigameLock;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.BaseKey;
import com.customsmithing.util.ItemBuilder;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;


public class BlockInteractListener implements Listener {

    private final CustomSmithing plugin;

    public BlockInteractListener(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        // Bukkit dispara este evento dos veces (mano principal y secundaria). Solo se atiende la principal:
        // si no, un item en la mano secundaria se procesaba como si fuera el de la principal.
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getPlayer().hasPermission("customsmithing.use")) return;

        Material blockType = event.getClickedBlock().getType();
        Player player = event.getPlayer();
        ItemStack hand = event.getItem();

        Material calentador = matOf("estaciones.calentador", Material.BLAST_FURNACE);
        Material forja = matOf("estaciones.forja", Material.ANVIL);
        Material fundicion = matOf("estaciones.fundicion", Material.FURNACE);
        // El caldero con agua es otro bloque (WATER_CAULDRON); el vacío no sirve para enfriar.
        Material enfriado = matOf("estaciones.enfriado", Material.WATER_CAULDRON);
        if (enfriado == Material.CAULDRON) enfriado = Material.WATER_CAULDRON;
        Material ensamblaje = matOf("estaciones.ensamblaje", Material.SMITHING_TABLE);
        Material refinado = matOf("estaciones.refinado", Material.GRINDSTONE);
        Material mesaPlanos = matOf("estaciones.mesa_planos", Material.LECTERN);
        Material aleacion = matOf("estaciones.aleacion", Material.SMOKER);
        Material telar = matOf("estaciones.telar", Material.LOOM);
        Material cortapiedras = matOf("estaciones.cortapiedras", Material.STONECUTTER);
        Material encantamiento = matOf("estaciones.encantamiento", Material.ENCHANTING_TABLE);

        if (blockType == fundicion) {
            handleSmelting(player, hand, event);
        } else if (blockType == calentador) {
            handleHeating(player, hand, event);
        } else if (blockType == forja) {
            handleHammering(player, hand, event);
        } else if (blockType == enfriado) {
            handleCooling(player, hand, event);
        } else if (blockType == ensamblaje) {
            event.setCancelled(true);
            new AssemblyGUI(plugin, player).open(player);
        } else if (blockType == refinado) {
            handleRefining(player, hand, event);
        } else if (blockType == aleacion) {
            handleAlloy(player, hand, event);
        } else if (blockType == telar) {
            handleCarving(player, hand, event, ProcessStage.TEJIDO, Material.STRING);
        } else if (blockType == cortapiedras) {
            handleCarving(player, hand, event, ProcessStage.TALLADO, Material.STICK);
        } else if (blockType == encantamiento) {
            event.setCancelled(true);
            new com.customsmithing.gui.EnchantGUI(plugin, player).open(player);
        } else if (blockType == mesaPlanos) {
            event.setCancelled(true);
            String heldRecipeId = hand != null && hand.hasItemMeta()
                    ? hand.getItemMeta().getPersistentDataContainer().get(SmithingKeys.BLUEPRINT_RECIPE, PersistentDataType.STRING)
                    : null;
            if (heldRecipeId != null && plugin.getRecipeManager().exists(heldRecipeId)) {
                new com.customsmithing.gui.ResearchGUI(plugin, player, heldRecipeId).open(player);
            } else {
                new BlueprintTableGUI(plugin, player).open(player);
            }
        }
    }

    /** True si alguna receta guardada usa esta clave base (metal vanilla o mineral custom) en este paso. */
    /** ¿El material de esta clave base admite la etapa? Depende solo del registro, no de las recetas. */
    private boolean isProcessable(String baseKey, ProcessStage step) {
        MaterialRegistry.Entry entry = plugin.getMaterialRegistry().forBaseKey(baseKey);
        return entry != null && entry.kind().allows(step);
    }

    /** Dificultad de referencia: la de las recetas que usan esta clave (promedio) o, si no hay, la del material. */
    private double referenceDifficulty(String baseKey) {
        double sum = 0; int count = 0;
        for (SmithingRecipe recipe : plugin.getRecipeManager().getAll()) {
            if (BaseKey.matchesMetal(recipe, baseKey)) {
                sum += recipe.getDifficulty();
                count++;
            }
        }
        if (count > 0) return sum / count;
        MaterialRegistry.Entry entry = plugin.getMaterialRegistry().forBaseKey(baseKey);
        return entry != null ? entry.difficulty() : 1.0;
    }

    /**
     * Avisa cuando el item de la mano es de este sistema pero no va en esta estación (antes se ignoraba en
     * silencio). Devuelve true si mostró un aviso. Los items que no son de herrería se ignoran sin molestar.
     */
    private boolean explainRejection(Player player, ItemStack hand, ProcessStage expected) {
        PieceData.Data piece = PieceData.read(hand);
        if (piece != null) {
            Messages.send(player, "procesos.pieza_etapa_incorrecta", "%etapa%", piece.stage().getLabel().toLowerCase(),
                    "%siguiente%", nextStep(piece.stage()));
            return true;
        }
        MaterialRegistry.Entry entry = plugin.getMaterialRegistry().forItem(hand);
        if (entry != null && !entry.kind().allows(expected)) {
            Messages.send(player, "procesos.estacion_equivocada", "%item%", BaseKey.displayName(plugin, BaseKey.ofItem(hand)),
                    "%estaciones%", entry.kind().getStations());
            return true;
        }
        return false;
    }

    private static String nextStep(ProcessStage stage) {
        return switch (stage) {
            case CALENTADO -> "la Forja (golpeado)";
            case GOLPEADO -> "el Caldero con agua (enfriado)";
            default -> "la Mesa de Ensamblaje";
        };
    }

    // ---------------- Fundición (mineral en bruto -> lingote) ----------------
    private void handleSmelting(Player player, ItemStack hand, PlayerInteractEvent event) {
        MaterialRegistry.Smelt smelt = plugin.getMaterialRegistry().smeltFor(hand);
        if (smelt == null) return; // no es algo que se funda aquí: el horno funciona como siempre

        event.setCancelled(true);
        ItemRef fuel = null;
        for (Material candidate : plugin.getMaterialRegistry().fuels()) {
            ItemRef ref = ItemRef.of(candidate);
            if (ref.count(player.getInventory()) > 0) { fuel = ref; break; }
        }
        if (fuel == null) {
            Messages.send(player, "procesos.fundicion_sin_combustible");
            return;
        }
        if (!fuel.remove(player.getInventory(), 1)) return;
        hand.setAmount(hand.getAmount() - 1);

        plugin.getGiveService().give(player, new ItemStack(smelt.output()));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1f, 1f);
        Messages.send(player, "procesos.fundicion_completa");
        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (smelt.xp() > 0 && data.addXp(smelt.xp())) {
            Messages.send(player, "nivel.subida", "%nivel%", String.valueOf(data.getLevel()));
        }
    }

    // ---------------- Aleacion (elegir a que metal convertir) ----------------
    // Nota: la mesa de aleacion trabaja con materiales vanilla; para "fabricar"
    // un mineral custom desde ahi, el admin define una aleacion de salida que
    // apunte a un item marcado (usa /smithing mineral givemineral para pruebas).
    private void handleAlloy(Player player, ItemStack hand, PlayerInteractEvent event) {
        if (hand == null || hand.getType() == Material.AIR) return;
        if (plugin.getAlloyManager().getByInput(hand).isEmpty()) return;
        event.setCancelled(true);
        new com.customsmithing.gui.AlloyGUI(plugin, player, hand.clone()).open(player);
    }

    // ---------------- Telar / Cortapiedras (minijuego real, UNIVERSAL por material) ----------------
    private void handleCarving(Player player, ItemStack hand, PlayerInteractEvent event,
                                ProcessStage step,
                                Material outputMaterial) {
        if (MinigameLock.isLocked(player.getUniqueId())) return;
        if (hand == null || hand.getType().isAir()) return;
        event.setCancelled(true);
        String baseKey = BaseKey.ofItem(hand);
        if (PieceData.isProcessed(hand) || plugin.getMaterialRegistry().forItem(hand) == null || !isProcessable(baseKey, step)) {
            if (!explainRejection(player, hand, step)) event.setCancelled(false);
            return;
        }
        String displayName = BaseKey.displayName(plugin, baseKey);
        ItemStack consumed = hand.clone();
        consumed.setAmount(1);
        hand.setAmount(hand.getAmount() - 1);

        new com.customsmithing.minigame.CarvingMinigame(plugin, player, baseKey, displayName, step, outputMaterial)
                .refund(consumed).start();
    }

    private Material matOf(String path, Material fallback) {
        String name = plugin.getConfig().getString(path);
        if (name == null) return fallback;
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : fallback;
    }

    // ---------------- Calentado (minijuego, UNIVERSAL por metal o mineral custom) ----------------
    private void handleHeating(Player player, ItemStack hand, PlayerInteractEvent event) {
        if (MinigameLock.isLocked(player.getUniqueId())) return;
        if (hand == null || hand.getType().isAir()) return;
        if (PieceData.isProcessed(hand)) {
            if (explainRejection(player, hand, ProcessStage.CALENTADO)) event.setCancelled(true);
            return;
        }

        String materialKey = BaseKey.ofItem(hand);
        if (materialKey == null) return;
        if (plugin.getMaterialRegistry().forItem(hand) == null) return; // no es un material de herrería: el horno funciona como siempre

        // Si el jugador tiene un Molde en la mano secundaria, la pieza final queda con
        // esa forma EXACTA (pico, hacha, peto, etc), en vez de ser generica por material.
        ItemStack offHand = player.getInventory().getItemInOffHand();
        com.customsmithing.data.MoldType heldMold = com.customsmithing.item.Molds.shapeOf(offHand);
        String baseKey = heldMold != null ? BaseKey.ofMold(heldMold, materialKey) : materialKey;

        if (!isProcessable(baseKey, ProcessStage.CALENTADO)) {
            if (explainRejection(player, hand, ProcessStage.CALENTADO)) event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        String displayName = BaseKey.displayName(plugin, baseKey);
        double difficulty = referenceDifficulty(baseKey);
        if (!com.customsmithing.item.Tools.available(plugin, player, com.customsmithing.data.ToolType.TENAZAS)) return;

        // Combustible (opcional): cada item da una temperatura máxima, que limita la calidad del Calentado.
        double fuelCap = 100.0;
        ItemStack consumedFuel = null;
        if (plugin.getConfig().getBoolean("combustible.activar", false)) {
            ItemRef bestFuel = null;
            double bestTemp = -1;
            var fuels = plugin.getConfig().getConfigurationSection("combustible.items");
            if (fuels != null) {
                for (String name : fuels.getKeys(false)) {
                    Material mat = Material.matchMaterial(name);
                    if (mat == null || !mat.isItem() || mat.isAir()) continue;
                    double temp = fuels.getDouble(name, 0);
                    ItemRef ref = ItemRef.of(mat);
                    if (temp > bestTemp && ref.count(player.getInventory()) > 0) { bestFuel = ref; bestTemp = temp; }
                }
            }
            if (bestFuel == null) {
                Messages.send(player, "procesos.calentado_sin_combustible");
                return;
            }
            if (!bestFuel.remove(player.getInventory(), 1)) return;
            fuelCap = bestTemp;
            consumedFuel = bestFuel.toItemStack(1);
        }

        ItemStack consumedMetal = hand.clone();
        consumedMetal.setAmount(1);
        ItemStack consumedMold = null;
        if (heldMold != null) {
            consumedMold = offHand.clone();
            consumedMold.setAmount(1);
        }
        hand.setAmount(hand.getAmount() - 1);
        if (heldMold != null) {
            if (offHand.getAmount() > 1) offHand.setAmount(offHand.getAmount() - 1);
            else player.getInventory().setItemInOffHand(null);
        }
        com.customsmithing.item.Tools.wear(plugin, player, com.customsmithing.data.ToolType.TENAZAS);
        final ItemStack moldUsed = consumedMold;
        new com.customsmithing.minigame.HeatingMinigame(plugin, player, baseKey, displayName, difficulty)
                .scoreCap(fuelCap)
                .refund(consumedMetal, consumedMold, consumedFuel)
                .onSuccess(() -> {
                    // El molde vuelve con un uso menos; al llegar a 0 se rompe.
                    if (moldUsed == null) return;
                    ItemStack worn = com.customsmithing.item.Molds.worn(plugin, moldUsed);
                    if (worn == null) Messages.send(player, "moldes.roto");
                    else plugin.getGiveService().give(player, worn);
                })
                .start();
    }

    // ---------------- Golpeado (minijuego, UNIVERSAL) ----------------
    private void handleHammering(Player player, ItemStack hand, PlayerInteractEvent event) {
        if (MinigameLock.isLocked(player.getUniqueId())) return;
        PieceData.Data piece = PieceData.read(hand);
        if (piece == null || piece.stage() != ProcessStage.CALENTADO) {
            if (hand != null && explainRejection(player, hand, ProcessStage.GOLPEADO)) event.setCancelled(true);
            return;
        }

        String baseKey = piece.baseKey();
        if (baseKey == null || !isProcessable(baseKey, ProcessStage.GOLPEADO)) return;

        double previousScore = piece.score();
        double difficulty = referenceDifficulty(baseKey);

        event.setCancelled(true);
        if (!com.customsmithing.item.Tools.available(plugin, player, com.customsmithing.data.ToolType.MARTILLO)) return;
        com.customsmithing.item.Tools.wear(plugin, player, com.customsmithing.data.ToolType.MARTILLO);
        ItemStack consumed = hand.clone();
        consumed.setAmount(1);
        hand.setAmount(hand.getAmount() - 1);
        new HammeringMinigame(plugin, player, baseKey, BaseKey.displayName(plugin, baseKey), previousScore, difficulty)
                .refund(consumed).start();
    }

    // ---------------- Enfriado (minijuego, UNIVERSAL) ----------------
    private void handleCooling(Player player, ItemStack hand, PlayerInteractEvent event) {
        if (MinigameLock.isLocked(player.getUniqueId())) return;
        if (event.getClickedBlock() == null) return;

        PieceData.Data piece = PieceData.read(hand);
        if (piece == null || piece.stage() != ProcessStage.GOLPEADO) {
            if (hand != null && explainRejection(player, hand, ProcessStage.ENFRIADO)) event.setCancelled(true);
            return;
        }

        String baseKey = piece.baseKey();
        if (baseKey == null || !isProcessable(baseKey, ProcessStage.ENFRIADO)) return;

        double previousScore = piece.score();

        event.setCancelled(true);
        ItemStack consumed = hand.clone();
        consumed.setAmount(1);
        hand.setAmount(hand.getAmount() - 1);
        new com.customsmithing.minigame.CoolingMinigame(plugin, player, baseKey, BaseKey.displayName(plugin, baseKey), previousScore)
                .refund(consumed).start();
    }

    // ---------------- Refinado ----------------
    private void handleRefining(Player player, ItemStack hand, PlayerInteractEvent event) {
        if (hand == null || !hand.hasItemMeta()) return;
        ItemMeta meta = hand.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        if (!pdc.has(SmithingKeys.QUALITY, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        boolean failActive = plugin.getConfig().getBoolean("fallos.activar", true);
        double baseFailChance = plugin.getConfig().getDouble("fallos.probabilidad_fallo_refinado", 15);

        String recipeId = pdc.get(SmithingKeys.RECIPE_ID, PersistentDataType.STRING);
        double difficulty = 1.0;
        if (recipeId != null) {
            SmithingRecipe recipe = plugin.getRecipeManager().get(recipeId);
            if (recipe != null) difficulty = recipe.getDifficulty();
        }
        double failChance = Math.min(90, baseFailChance * difficulty);

        if (failActive && Math.random() * 100 < failChance) {
            double breakChance = plugin.getConfig().getDouble("fallos.probabilidad_romper_en_fallo", 25);
            if (Math.random() * 100 < breakChance) {
                hand.setAmount(hand.getAmount() - 1);
                Messages.send(player, "procesos.refinado_roto");
            } else {
                adjustQuality(hand, -1);
                Messages.send(player, "procesos.refinado_fallo");
            }
        } else {
            adjustQuality(hand, 1);
            Messages.send(player, "procesos.refinado_exito");
        }
    }

    private void adjustQuality(ItemStack item, int direction) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        Quality oldQuality = Quality.parse(pdc.get(SmithingKeys.QUALITY, PersistentDataType.STRING));
        if (oldQuality == null) return;

        Quality newQuality = oldQuality.shift(direction);
        double ratio = plugin.getQualityConfig().ratio(oldQuality, newQuality);

        ItemStats stats = new ItemStats();
        stats.setCritChance(round(getD(pdc, SmithingKeys.STAT_CRIT) * ratio));
        stats.setLifesteal(round(getD(pdc, SmithingKeys.STAT_LIFESTEAL) * ratio));
        stats.setAttackSpeedBonus(round(getD(pdc, SmithingKeys.STAT_ATTACK_SPEED) * ratio));
        stats.setElementalType(pdc.getOrDefault(SmithingKeys.STAT_ELEMENTAL_TYPE, PersistentDataType.STRING, "NINGUNO"));
        stats.setElementalDamage(round(getD(pdc, SmithingKeys.STAT_ELEMENTAL_DAMAGE) * ratio));
        stats.setResistance(round(getD(pdc, SmithingKeys.STAT_RESISTANCE) * ratio));
        stats.setBonusDamage(round(getD(pdc, SmithingKeys.STAT_BONUS_DAMAGE) * ratio));

        ItemStack rebuilt = new ItemBuilder(item).applyQualityAndStats(newQuality, stats).build();
        item.setItemMeta(rebuilt.getItemMeta());
    }

    private double getD(org.bukkit.persistence.PersistentDataContainer pdc, org.bukkit.NamespacedKey key) {
        Double v = pdc.get(key, PersistentDataType.DOUBLE);
        return v != null ? v : 0.0;
    }

    private double round(double v) {
        return Text.round1(v);
    }
}
