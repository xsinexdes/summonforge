package com.customsmithing.item;

import com.customsmithing.data.SmithingKeys;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/**
 * Identifica un item (vanilla o custom) y es la ÚNICA puerta para compararlo:
 * {@link #matches(ItemStack)}, contar y quitar de un inventario usan exactamente la misma regla,
 * así que "lo que cuentas" siempre es "lo que se consume".
 *
 * La regla vive en {@link ItemSignature} (probada con JUnit). Meta considerada: nombre propio,
 * CustomModelData y etiqueta del plugin (minerales custom u otros items marcados). El lore y los
 * datos de otros plugins no se comparan a propósito: el lore cambia al refinar o firmar.
 */
public final class ItemRef {

    private static final String SMITHING_ITEM_TAG = "smithing_item";
    private static final String MINERAL_PREFIX = "mineral:";

    private final Material material;
    private final ItemSignature signature;

    private ItemRef(Material material, ItemSignature signature) {
        this.material = material;
        this.signature = signature;
    }

    /** Referencia a un item "normal" de ese material (sin nombre, sin modelo, sin etiqueta). */
    public static ItemRef of(Material material) {
        if (material == null || !material.isItem() || material.isAir()) {
            throw new IllegalArgumentException("material no válido como item: " + material);
        }
        return new ItemRef(material, new ItemSignature(material.name(), null, null, null));
    }

    /** Referencia al item exacto que tienes en la mano / en un slot. Null si está vacío. */
    public static ItemRef of(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.getType().isItem()) return null;
        return new ItemRef(stack.getType(), signatureOf(stack));
    }

    /**
     * Lee una referencia desde YAML. Acepta un texto ("IRON_INGOT") o una sección con
     * material/modelo/nombre/etiqueta.
     *
     * @throws IllegalArgumentException con el motivo legible si los datos no sirven
     */
    public static ItemRef load(ConfigurationSection parent, String key) {
        if (parent.isString(key)) {
            return ofName(parent.getString(key));
        }
        ConfigurationSection sec = parent.getConfigurationSection(key);
        if (sec == null) throw new IllegalArgumentException("falta el item '" + key + "'");
        ItemSignature sig = ItemSignature.fromMap(sec.getValues(false));
        Material mat = Material.matchMaterial(sig.getMaterial());
        if (mat == null || !mat.isItem() || mat.isAir()) {
            throw new IllegalArgumentException("material desconocido o que no es un item: " + sig.getMaterial());
        }
        return new ItemRef(mat, sig);
    }

    private static ItemRef ofName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("material vacío");
        Material mat = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (mat == null || !mat.isItem() || mat.isAir()) {
            throw new IllegalArgumentException("material desconocido o que no es un item: " + name);
        }
        return of(mat);
    }

    /** Escribe la referencia en una sección YAML (sustituye lo que hubiera). */
    public void save(ConfigurationSection section) {
        for (String existing : section.getKeys(false)) section.set(existing, null);
        for (var entry : signature.toMap().entrySet()) section.set(entry.getKey(), entry.getValue());
    }

    public Material getMaterial() { return material; }
    public ItemSignature getSignature() { return signature; }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() != material) return false;
        return signature.matches(signatureOf(stack));
    }

    /** Crea el item real descrito por esta referencia. */
    public ItemStack toItemStack(int amount) {
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        if (signature.getName() != null) meta.setDisplayName(signature.getName());
        if (signature.getModelData() != null) meta.setCustomModelData(signature.getModelData());
        String tag = signature.getTag();
        if (tag != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (tag.startsWith(MINERAL_PREFIX)) {
                pdc.set(SmithingKeys.CUSTOM_MINERAL_ID, PersistentDataType.STRING, tag.substring(MINERAL_PREFIX.length()));
            } else if (tag.equals(SMITHING_ITEM_TAG)) {
                pdc.set(SmithingKeys.IS_SMITHING_ITEM, PersistentDataType.BYTE, (byte) 1);
            } else {
                pdc.set(SmithingKeys.REF_TAG, PersistentDataType.STRING, tag);
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** Nombre para mostrar en mensajes: el nombre propio si tiene, si no el del material. */
    public String displayName() {
        return signature.getName() != null ? signature.getName() : com.customsmithing.util.Text.prettify(material.name());
    }

    // ---------------------------------------------------------------- inventarios

    /** Cuántos items que cumplen esta referencia hay en el inventario (solo slots de almacenamiento). */
    public int count(Inventory inventory) {
        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (matches(stack)) total += stack.getAmount();
        }
        return total;
    }

    /**
     * Quita exactamente {@code amount} items que cumplen la referencia. Todo o nada:
     * si no alcanza, no toca el inventario y devuelve false.
     */
    public boolean remove(Inventory inventory, int amount) {
        if (amount <= 0) return true;
        if (count(inventory) < amount) return false;
        int left = amount;
        int size = inventory.getStorageContents().length;
        for (int i = 0; i < size && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!matches(stack)) continue;
            int take = Math.min(left, stack.getAmount());
            if (take >= stack.getAmount()) {
                inventory.setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
                inventory.setItem(i, stack);
            }
            left -= take;
        }
        return left == 0;
    }

    // ---------------------------------------------------------------- interno

    private static ItemSignature signatureOf(ItemStack stack) {
        String name = null;
        Integer model = null;
        String tag = null;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) name = meta.getDisplayName();
            if (meta.hasCustomModelData()) model = meta.getCustomModelData();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String refTag = pdc.get(SmithingKeys.REF_TAG, PersistentDataType.STRING);
            String mineral = pdc.get(SmithingKeys.CUSTOM_MINERAL_ID, PersistentDataType.STRING);
            if (refTag != null) {
                tag = refTag;
            } else if (mineral != null) {
                tag = MINERAL_PREFIX + mineral;
            } else if (pdc.has(SmithingKeys.IS_SMITHING_ITEM, PersistentDataType.BYTE)) {
                tag = SMITHING_ITEM_TAG;
            }
        }
        return new ItemSignature(stack.getType().name(), model, name, tag);
    }
}
