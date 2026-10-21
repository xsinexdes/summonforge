package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Aplica de verdad la "Vel. ataque" del lore: mientras el jugador sostiene el arma, se le añade un
 * modificador de velocidad de ataque. Se hace sobre el jugador (no sobre el item) porque en 1.21 añadir
 * un modificador al item reemplaza sus atributos base (daño y velocidad) y lo dejaría sin daño.
 * Se retira al soltar el arma, al salir y al apagar el plugin.
 */
public final class AttackSpeedTask {

    private final CustomSmithing plugin;
    private final NamespacedKey modifierKey;
    private int taskId = -1;

    public AttackSpeedTask(CustomSmithing plugin) {
        this.plugin = plugin;
        this.modifierKey = new NamespacedKey(plugin, "attack_speed_bonus");
    }

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 20L, 10L);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        for (Player player : Bukkit.getOnlinePlayers()) clear(player);
    }

    public void clear(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attribute == null) return;
        AttributeModifier existing = find(attribute);
        if (existing != null) attribute.removeModifier(existing);
    }

    private void tick() {
        double cap = plugin.getConfig().getDouble("combate.tope_velocidad_ataque", 50);
        for (Player player : Bukkit.getOnlinePlayers()) {
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attribute == null) continue;
            double bonus = Math.min(bonusOf(player.getInventory().getItemInMainHand()), cap) / 100.0;
            AttributeModifier existing = find(attribute);
            if (existing != null && Math.abs(existing.getAmount() - bonus) < 1e-6) continue;
            if (existing != null) attribute.removeModifier(existing);
            if (bonus > 0) {
                attribute.addModifier(new AttributeModifier(modifierKey, bonus,
                        AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
            }
        }
    }

    private AttributeModifier find(AttributeInstance attribute) {
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifierKey.equals(modifier.getKey())) return modifier;
        }
        return null;
    }

    private double bonusOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return 0;
        Double value = meta.getPersistentDataContainer().get(SmithingKeys.STAT_ATTACK_SPEED, PersistentDataType.DOUBLE);
        return value != null ? Math.max(0, value) : 0;
    }
}
