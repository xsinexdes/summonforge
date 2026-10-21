package com.customsmithing.skin;

import com.customsmithing.CustomSmithing;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Única puerta para el aspecto de los items del plugin (iconos de menú, botones...).
 * Cada elemento visual tiene un id (p. ej. "menu.habilidades") y en skins.yml se le asigna
 * material base + CustomModelData. Si el resource pack está desactivado
 * ({@code resourcepack.activar_skins: false}) o el id no existe, se usa SIEMPRE el icono
 * vanilla de respaldo, sin errores.
 *
 * Cuando se migre a 1.21.4+ (componente item_model) solo hay que cambiar {@link #apply}.
 */
public final class SkinService {

    public record Skin(Material material, int modelData) {
    }

    private final CustomSmithing plugin;
    private final File file;
    private final Map<String, Skin> skins = new HashMap<>();
    private boolean enabled;

    public SkinService(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "skins.yml");
        load();
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("skins.yml", false);
        skins.clear();
        enabled = plugin.getConfig().getBoolean("resourcepack.activar_skins", false);

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("skins");
        if (root == null) return;

        for (String id : root.getKeys(true)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null || !sec.contains("material")) continue; // es un grupo, no una skin
            Material material = Material.matchMaterial(sec.getString("material", ""));
            if (material == null || !material.isItem() || material.isAir()) {
                plugin.getLogger().warning("skins.yml › skins." + id + ".material: '"
                        + sec.getString("material") + "' no es un item válido. Se usa el icono vanilla.");
                continue;
            }
            if (!sec.isInt("modelo") || sec.getInt("modelo") < 0) {
                plugin.getLogger().warning("skins.yml › skins." + id + ".modelo: debe ser un entero mayor o igual que 0. "
                        + "Se usa el icono vanilla.");
                continue;
            }
            skins.put(id, new Skin(material, sec.getInt("modelo")));
        }
        plugin.debug("Skins cargadas: " + skins.size() + " (activas: " + enabled + ")");
    }

    public boolean isEnabled() { return enabled; }

    /** ¿Existe una skin con ese id? (aunque estén desactivadas) */
    public boolean has(String skinId) {
        return skins.containsKey(skinId);
    }

    /** La skin de ese id si las skins están activas y existe; si no, null (usar el icono vanilla). */
    public Skin skinFor(String skinId) {
        return enabled && skinId != null ? skins.get(skinId) : null;
    }

    /** Item base para ese elemento, con el modelo aplicado si las skins están activas. */
    public ItemStack icon(String skinId, Material fallback) {
        Skin skin = enabled ? skins.get(skinId) : null;
        ItemStack stack = new ItemStack(skin != null ? skin.material() : fallback);
        if (skin != null) apply(stack, skin);
        return stack;
    }

    private void apply(ItemStack stack, Skin skin) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.setCustomModelData(skin.modelData());
        stack.setItemMeta(meta);
    }
}
