package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.bosses.BossAbility;
import com.customsmithing.bosses.BossDefinition;
import com.customsmithing.minerals.CustomMineral;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.Text;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de jefes 100% in-game: elegis el mob base, sus multiplicadores,
 * que habilidades custom tiene, y que dropea (mineral custom + plano).
 */
public class BossEditorGUI implements SmithingGUIHolder {

    private static final EntityType[] BASE_MOBS = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER_SKELETON, EntityType.HUSK,
            EntityType.PIGLIN_BRUTE, EntityType.VINDICATOR, EntityType.RAVAGER, EntityType.BLAZE,
            EntityType.ENDERMAN, EntityType.STRAY
    };

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;
    private final BossDefinition working;

    private static final int BANNER_SLOT = 4;
    private static final int MOB_SLOT = 10;
    private static final int HEALTH_SLOT = 12;
    private static final int DAMAGE_SLOT = 13;
    private static final int SPEED_SLOT = 14;
    private static final int[] ABILITY_SLOTS = {19, 20, 21, 22};
    private static final int MINERAL_SLOT = 29;
    private static final int MINERAL_AMOUNT_SLOT = 30;
    private static final int MINERAL_CHANCE_SLOT = 31;
    private static final int BLUEPRINT_SLOT = 33;
    private static final int BLUEPRINT_CHANCE_SLOT = 34;
    private static final int STRUCTURE_SLOT = 40;
    private static final int SAVE_SLOT = 49;
    private static final int CANCEL_SLOT = 53;

    public BossEditorGUI(CustomSmithing plugin, Player player, String id) {
        this.plugin = plugin;
        this.player = player;
        if (plugin.getBossManager().exists(id)) {
            BossDefinition src = plugin.getBossManager().get(id);
            this.working = new BossDefinition(id);
            working.setDisplayName(src.getDisplayName());
            working.setEntityType(src.getEntityType());
            working.setHealthMultiplier(src.getHealthMultiplier());
            working.setDamageMultiplier(src.getDamageMultiplier());
            working.setSpeedMultiplier(src.getSpeedMultiplier());
            working.setAbilities(java.util.EnumSet.copyOf(src.getAbilities().isEmpty() ? java.util.EnumSet.noneOf(BossAbility.class) : src.getAbilities()));
            working.setDropMineralId(src.getDropMineralId());
            working.setDropMineralAmount(src.getDropMineralAmount());
            working.setDropMineralChance(src.getDropMineralChance());
            working.setDropBlueprintId(src.getDropBlueprintId());
            working.setDropBlueprintChance(src.getDropBlueprintChance());
            working.setStructureId(src.getStructureId());
        } else {
            this.working = new BossDefinition(id);
            working.setDisplayName(id);
        }
        this.inventory = Bukkit.createInventory(this, 54, "\u2620 Editor de Jefe: " + id);
        render();
    }

    private void render() {
        inventory.clear();
        ItemStack filler = labeled(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        inventory.setItem(BANNER_SLOT, labeled(Material.WITHER_SKELETON_SKULL,
                "\u00a7c\u00a7l" + working.getId(),
                "\u00a77Configurá el mob base, sus stats,",
                "\u00a77habilidades y qué dropea al morir."));

        inventory.setItem(MOB_SLOT, labeled(Material.ZOMBIE_HEAD,
                "\u00a7cMob base: \u00a7f" + working.getEntityType().name(),
                "\u00a77Clic para ciclar entre mobs disponibles"));

        inventory.setItem(HEALTH_SLOT, labeled(Material.APPLE,
                "\u00a7c\u2764 Multiplicador de vida: \u00a7fx" + Text.fmt1(working.getHealthMultiplier()),
                "\u00a77Clic izq: +0.5  |  Clic der: -0.5"));
        inventory.setItem(DAMAGE_SLOT, labeled(Material.IRON_SWORD,
                "\u00a76\u2694 Multiplicador de daño: \u00a7fx" + Text.fmt1(working.getDamageMultiplier()),
                "\u00a77Clic izq: +0.2  |  Clic der: -0.2"));
        inventory.setItem(SPEED_SLOT, labeled(Material.FEATHER,
                "\u00a7b\u27a4 Multiplicador de velocidad: \u00a7fx" + Text.fmt1(working.getSpeedMultiplier()),
                "\u00a77Clic izq: +0.1  |  Clic der: -0.1"));

        BossAbility[] abilities = BossAbility.values();
        for (int i = 0; i < ABILITY_SLOTS.length && i < abilities.length; i++) {
            BossAbility ability = abilities[i];
            boolean active = working.getAbilities().contains(ability);
            inventory.setItem(ABILITY_SLOTS[i], labeled(active ? Material.LIME_DYE : Material.GRAY_DYE,
                    (active ? "\u00a7a\u2714 " : "\u00a7c\u2716 ") + ability.getLabel(),
                    "\u00a77" + ability.getDescription(),
                    "\u00a77Clic para " + (active ? "desactivar" : "activar")));
        }

        if (working.getDropMineralId() != null && plugin.getCustomMineralManager().exists(working.getDropMineralId())) {
            CustomMineral mineral = plugin.getCustomMineralManager().get(working.getDropMineralId());
            inventory.setItem(MINERAL_SLOT, labeled(mineral.getIconMaterial(),
                    "\u00a75Dropea mineral: \u00a7f" + mineral.getDisplayName(),
                    "\u00a77Clic para ciclar / quitar"));
        } else {
            inventory.setItem(MINERAL_SLOT, labeled(Material.PAPER,
                    "\u00a77+ Elegir mineral a dropear",
                    "\u00a77Clic para ciclar entre los minerales creados"));
        }
        inventory.setItem(MINERAL_AMOUNT_SLOT, labeled(Material.HOPPER,
                "\u00a7bCantidad: \u00a7f" + working.getDropMineralAmount(),
                "\u00a77Clic izq: +1  |  Clic der: -1"));
        inventory.setItem(MINERAL_CHANCE_SLOT, labeled(Material.CLOCK,
                "\u00a7bProbabilidad: \u00a7f" + String.format("%.0f", working.getDropMineralChance()) + "%",
                "\u00a77Clic izq: +5%  |  Clic der: -5%"));

        SmithingRecipe blueprintRecipe = working.getDropBlueprintId() != null ? plugin.getRecipeManager().get(working.getDropBlueprintId()) : null;
        inventory.setItem(BLUEPRINT_SLOT, labeled(Material.WRITTEN_BOOK,
                "\u00a7dDropea plano de: \u00a7f" + (blueprintRecipe != null ? blueprintRecipe.getResultName() : "Ninguno"),
                "\u00a77Clic para ciclar entre recetas con plano"));
        inventory.setItem(BLUEPRINT_CHANCE_SLOT, labeled(Material.CLOCK,
                "\u00a7bProbabilidad: \u00a7f" + String.format("%.0f", working.getDropBlueprintChance()) + "%",
                "\u00a77Clic izq: +5%  |  Clic der: -5%"));

        List<String> structureIds = plugin.getStructureManager().listIds();
        inventory.setItem(STRUCTURE_SLOT, labeled(Material.BRICKS,
                "\u00a76Estructura: \u00a7f" + (working.getStructureId() != null ? working.getStructureId() : "Ninguna (solo /smithing spawnboss)"),
                "\u00a77Clic para ciclar (" + structureIds.size() + " guardadas)",
                "\u00a77Clic derecho: quitar",
                "\u00a77Guardá una con /smithing structure save <id>"));

        inventory.setItem(SAVE_SLOT, labeled(Material.EMERALD_BLOCK, "\u00a7a\u00a7lGUARDAR JEFE", "\u00a77Usa /smithing spawnboss " + working.getId() + " para probarlo"));
        inventory.setItem(CANCEL_SLOT, labeled(Material.REDSTONE_BLOCK, "\u00a7c\u00a7lCANCELAR", "\u00a77Cierra sin guardar"));
    }

    private ItemStack labeled(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        boolean right = event.getClick() == ClickType.RIGHT;

        if (slot == MOB_SLOT) {
            int idx = indexOf(BASE_MOBS, working.getEntityType());
            int next = (idx + 1) % BASE_MOBS.length;
            working.setEntityType(BASE_MOBS[next]);
            render();
            return;
        }
        if (slot == HEALTH_SLOT) {
            working.setHealthMultiplier(working.getHealthMultiplier() + (right ? -0.5 : 0.5));
            render();
            return;
        }
        if (slot == DAMAGE_SLOT) {
            working.setDamageMultiplier(working.getDamageMultiplier() + (right ? -0.2 : 0.2));
            render();
            return;
        }
        if (slot == SPEED_SLOT) {
            working.setSpeedMultiplier(working.getSpeedMultiplier() + (right ? -0.1 : 0.1));
            render();
            return;
        }

        BossAbility[] abilities = BossAbility.values();
        for (int i = 0; i < ABILITY_SLOTS.length && i < abilities.length; i++) {
            if (slot == ABILITY_SLOTS[i]) {
                BossAbility ability = abilities[i];
                if (working.getAbilities().contains(ability)) working.getAbilities().remove(ability);
                else working.getAbilities().add(ability);
                render();
                return;
            }
        }

        if (slot == MINERAL_SLOT) {
            List<CustomMineral> allMinerals = new ArrayList<>(plugin.getCustomMineralManager().getAll());
            if (allMinerals.isEmpty()) return;
            if (right) { working.setDropMineralId(null); render(); return; }
            int idx = -1;
            for (int i = 0; i < allMinerals.size(); i++) if (allMinerals.get(i).getId().equals(working.getDropMineralId())) { idx = i; break; }
            int next = (idx + 1) % allMinerals.size();
            working.setDropMineralId(allMinerals.get(next).getId());
            render();
            return;
        }
        if (slot == MINERAL_AMOUNT_SLOT) {
            working.setDropMineralAmount(working.getDropMineralAmount() + (right ? -1 : 1));
            render();
            return;
        }
        if (slot == MINERAL_CHANCE_SLOT) {
            working.setDropMineralChance(working.getDropMineralChance() + (right ? -5 : 5));
            render();
            return;
        }

        if (slot == BLUEPRINT_SLOT) {
            List<SmithingRecipe> candidates = new ArrayList<>();
            for (SmithingRecipe r : plugin.getRecipeManager().getAll()) if (r.isRequiresBlueprint()) candidates.add(r);
            if (candidates.isEmpty()) return;
            if (right) { working.setDropBlueprintId(null); render(); return; }
            int idx = -1;
            for (int i = 0; i < candidates.size(); i++) if (candidates.get(i).getId().equals(working.getDropBlueprintId())) { idx = i; break; }
            int next = (idx + 1) % candidates.size();
            working.setDropBlueprintId(candidates.get(next).getId());
            render();
            return;
        }
        if (slot == BLUEPRINT_CHANCE_SLOT) {
            working.setDropBlueprintChance(working.getDropBlueprintChance() + (right ? -5 : 5));
            render();
            return;
        }

        if (slot == STRUCTURE_SLOT) {
            List<String> structureIds = plugin.getStructureManager().listIds();
            if (structureIds.isEmpty()) return;
            if (right) { working.setStructureId(null); render(); return; }
            int idx = structureIds.indexOf(working.getStructureId());
            int next = (idx + 1) % structureIds.size();
            working.setStructureId(structureIds.get(next));
            render();
            return;
        }

        if (slot == SAVE_SLOT) {
            if (working.getDisplayName() == null || working.getDisplayName().isBlank()) working.setDisplayName(working.getId());
            plugin.getBossManager().save(working);
            Messages.send(player, "editor.jefe_guardado", "%id%", working.getId());
            player.closeInventory();
            return;
        }
        if (slot == CANCEL_SLOT) {
            player.closeInventory();
        }
    }

    private int indexOf(EntityType[] arr, EntityType v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return 0;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
