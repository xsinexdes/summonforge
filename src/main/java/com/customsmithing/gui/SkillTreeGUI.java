package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.Passives;
import com.customsmithing.data.Specialization;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Árbol de habilidades: una columna por especialización. La cabecera se pulsa para elegirla (con
 * enfriamiento entre cambios) y debajo hay un nodo pasivo por cada umbral de nivel
 * (habilidades.niveles). Los nodos alcanzados de TU especialización aumentan los stats de lo que fabricas
 * de esa especialización (habilidades.bonus_por_nivel).
 */
public class SkillTreeGUI implements SmithingGUIHolder {

    private final CustomSmithing plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int[] COLUMNS = {10, 12, 14, 16};   // slot de la cabecera de cada especialización

    public SkillTreeGUI(CustomSmithing plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, "\u2694 Árbol de Habilidades");
        render();
    }

    private List<Integer> thresholds() {
        List<Integer> list = plugin.getConfig().getIntegerList("habilidades.niveles");
        return list.isEmpty() ? List.of(5, 10, 15) : list;
    }

    private double bonusPerTier() {
        return plugin.getConfig().getDouble("habilidades.bonus_por_nivel", 10);
    }

    private void render() {
        inventory.clear();
        PlayerData data = plugin.getPlayerDataManager().get(player);

        ItemStack info = new ItemStack(Material.ANVIL);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("\u00a7bNivel de Herrería: \u00a7f" + data.getLevel());
        infoMeta.setLore(List.of(
                "\u00a77XP: \u00a7f" + data.getXp() + " / " + data.requiredXpForNextLevel(),
                "\u00a77Puntos de receta: \u00a7f" + plugin.getUnlockService().availablePoints(data),
                "\u00a77Objetos fabricados: \u00a7f" + data.getTotalCrafts(),
                "\u00a77Precisión promedio: \u00a7f" + Text.fmt1(data.getAvgTimingAccuracy()) + "%"));
        info.setItemMeta(infoMeta);
        inventory.setItem(4, info);

        List<Integer> levels = thresholds();
        int reached = Passives.tiersReached(data.getLevel(), levels);
        Specialization[] specs = Specialization.values();
        for (int i = 0; i < specs.length && i < COLUMNS.length; i++) {
            Specialization spec = specs[i];
            boolean selected = spec == data.getSpecialization();
            inventory.setItem(COLUMNS[i], header(spec, selected));

            for (int node = 0; node < levels.size() && node < 3; node++) {
                int level = levels.get(node);
                boolean unlocked = data.getLevel() >= level;
                boolean active = unlocked && selected;
                ItemStack pane = new ItemStack(active ? Material.LIME_STAINED_GLASS_PANE
                        : unlocked ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = pane.getItemMeta();
                meta.setDisplayName((active ? "\u00a7a" : unlocked ? "\u00a7e" : "\u00a77") + "Nodo " + (node + 1) + " \u00a78(nivel " + level + ")");
                List<String> lore = new ArrayList<>();
                lore.add("\u00a77+" + Text.fmt1(bonusPerTier()) + "% a los stats de lo que fabricas");
                lore.add("\u00a77de " + spec.getLabel() + ".");
                lore.add(" ");
                lore.add(active ? "\u00a7aActivo"
                        : unlocked ? "\u00a7eAlcanzado, pero no es tu especialización" : "\u00a7cSe alcanza en el nivel " + level);
                meta.setLore(lore);
                pane.setItemMeta(meta);
                inventory.setItem(COLUMNS[i] + 9 * (node + 1), pane);
            }
        }

        if (data.getSpecialization() != null) {
            ItemStack total = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = total.getItemMeta();
            meta.setDisplayName("\u00a76Bonus actual: \u00a7f+" + Text.fmt1(Passives.multiplier(reached, bonusPerTier()) * 100 - 100)
                    + "% \u00a77en " + data.getSpecialization().getLabel());
            total.setItemMeta(meta);
            inventory.setItem(40, total);
        }
    }

    private ItemStack header(Specialization spec, boolean selected) {
        ItemStack item = new ItemStack(specMaterial(spec));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((selected ? "\u00a7a\u2714 " : "\u00a7f") + spec.getLabel());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setLore(List.of(
                "\u00a77" + spec.getDescription(),
                " ",
                selected ? "\u00a7aEspecialización activa" : "\u00a7eClic para elegir esta especialización"));
        item.setItemMeta(meta);
        return item;
    }

    private Material specMaterial(Specialization spec) {
        return switch (spec) {
            case ESPADAS -> Material.IRON_SWORD;
            case ARMADURAS -> Material.IRON_CHESTPLATE;
            case HERRAMIENTAS -> Material.IRON_PICKAXE;
            case FORJA_MAGICA -> Material.BLAZE_ROD;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        Specialization[] specs = Specialization.values();
        for (int i = 0; i < COLUMNS.length && i < specs.length; i++) {
            if (slot != COLUMNS[i]) continue;
            long wait = plugin.getUnlockService().changeSpecialization(player, specs[i]);
            if (wait > 0) {
                Messages.send(player, "especializacion.cooldown", "%tiempo%", Text.formatDuration(wait));
            } else {
                Messages.send(player, "especializacion.cambiada", "%especializacion%", specs[i].getLabel());
            }
            render();
            return;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
