package com.customsmithing.listeners;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.SmithingKeys;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Efectos de combate de los items forjados. Todo lo que muestra el lore hace algo real:
 * crítico (+50 % de daño), robo de vida (con tope), daño elemental con efecto propio y resistencia
 * sumando las 4 piezas de armadura (con tope). Se ignoran los golpes cancelados por otros plugins.
 */
public class CombatListener implements Listener {

    private final CustomSmithing plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> lastCritNotice = new HashMap<>();

    public CombatListener(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta()) return;
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(SmithingKeys.QUALITY, PersistentDataType.STRING)) return;

        FileConfiguration cfg = plugin.getConfig();
        double crit = getD(pdc, SmithingKeys.STAT_CRIT);
        double lifesteal = getD(pdc, SmithingKeys.STAT_LIFESTEAL);
        double elementalDamage = getD(pdc, SmithingKeys.STAT_ELEMENTAL_DAMAGE);
        double materialDamage = getD(pdc, SmithingKeys.STAT_BONUS_DAMAGE);
        String element = pdc.get(SmithingKeys.STAT_ELEMENTAL_TYPE, PersistentDataType.STRING);

        double bonusDamage = 0;
        if (crit > 0 && random.nextDouble() * 100 < crit) {
            bonusDamage += event.getDamage() * 0.5; // +50% en crítico
            notifyCrit(attacker);
        }
        if (elementalDamage > 0) {
            bonusDamage += elementalDamage;
            applyElement(victim, element);
        }
        bonusDamage += materialDamage;
        if (bonusDamage > 0) event.setDamage(event.getDamage() + bonusDamage);

        if (lifesteal > 0) {
            double capped = Math.min(lifesteal, cfg.getDouble("combate.tope_robo_vida", 15));
            double healAmount = event.getFinalDamage() * (capped / 100.0);
            AttributeInstance maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double max = maxHealth != null ? maxHealth.getValue() : 20.0;
            attacker.setHealth(Math.min(max, attacker.getHealth() + healAmount));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;

        // La resistencia especial de las 4 piezas se suma, con un tope configurable.
        double resistance = 0;
        for (ItemStack piece : defender.getInventory().getArmorContents()) {
            if (piece == null || !piece.hasItemMeta()) continue;
            ItemMeta meta = piece.getItemMeta();
            if (meta == null) continue;
            resistance += getD(meta.getPersistentDataContainer(), SmithingKeys.STAT_RESISTANCE);
        }
        resistance = Math.min(resistance, plugin.getConfig().getDouble("combate.tope_resistencia", 40));
        if (resistance <= 0) return;

        double reduction = event.getDamage() * (resistance / 100.0) * 0.5; // reduce hasta el 50 % de su valor
        event.setDamage(Math.max(0, event.getDamage() - reduction));
    }

    private void applyElement(LivingEntity victim, String element) {
        if (element == null) return;
        FileConfiguration cfg = plugin.getConfig();
        switch (element) {
            case "FUEGO" -> victim.setFireTicks(Math.max(victim.getFireTicks(),
                    (int) (cfg.getDouble("combate.elementos.fuego_segundos", 4) * 20)));
            case "HIELO" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    (int) (cfg.getDouble("combate.elementos.hielo_segundos", 3) * 20), 1));
            case "VENENO" -> victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON,
                    (int) (cfg.getDouble("combate.elementos.veneno_segundos", 3) * 20), 0));
            case "RAYO" -> victim.getWorld().strikeLightningEffect(victim.getLocation());
            default -> { }
        }
    }

    /** Aviso por la barra de acción, como máximo una vez por segundo (antes llenaba el chat). */
    private void notifyCrit(Player attacker) {
        long now = System.currentTimeMillis();
        Long last = lastCritNotice.get(attacker.getUniqueId());
        if (last != null && now - last < 1000) return;
        lastCritNotice.put(attacker.getUniqueId(), now);
        attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("\u00a76\u00a7l\u00a1CRÍTICO!"));
    }

    private double getD(PersistentDataContainer pdc, NamespacedKey key) {
        Double v = pdc.get(key, PersistentDataType.DOUBLE);
        return v != null ? v : 0.0;
    }
}
