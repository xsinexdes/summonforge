package com.customsmithing.material;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.item.ItemRef;
import com.customsmithing.minerals.CustomMineral;
import com.customsmithing.util.IdValidator;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de materiales trabajables ("universal por material"): cualquier metal, madera o tela
 * que figure en materiales.yml (y todo mineral custom) se puede procesar en las estaciones SIN
 * necesidad de que exista una receta que lo use. También define la Fundición (mineral -> lingote).
 */
public final class MaterialRegistry {

    public enum Kind {
        METAL("el Calentador, la Forja y el Enfriado"),
        MADERA("el Cortapiedras"),
        TELA("el Telar");

        private final String stations;

        Kind(String stations) {
            this.stations = stations;
        }

        public String getStations() { return stations; }

        /** Etapas que admite este tipo de material. */
        public boolean allows(ProcessStage stage) {
            return switch (this) {
                case METAL -> stage == ProcessStage.CALENTADO || stage == ProcessStage.GOLPEADO || stage == ProcessStage.ENFRIADO;
                case MADERA -> stage == ProcessStage.TALLADO;
                case TELA -> stage == ProcessStage.TEJIDO;
            };
        }
    }

    /** bonusResistance / bonusDamage: lo que aporta este material al item fabricado con él. */
    public record Entry(String id, Kind kind, Material item, double difficulty, double bonusResistance, double bonusDamage) {
    }

    public record Smelt(String id, Material input, Material output, int xp) {
    }

    private static final String[] SECTIONS = {"metales", "maderas", "telas"};
    private static final Kind[] KINDS = {Kind.METAL, Kind.MADERA, Kind.TELA};

    private final CustomSmithing plugin;
    private final File file;
    private final Map<Material, Entry> byMaterial = new LinkedHashMap<>();
    private final Map<Material, Smelt> smelts = new LinkedHashMap<>();
    private final List<Material> fuels = new ArrayList<>();

    public MaterialRegistry(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "materiales.yml");
        load();
    }

    private void warn(String path, String reason) {
        plugin.getLogger().warning("materiales.yml › " + path + ": " + reason + " — se omite.");
    }

    private static Material parseItem(String name) {
        if (name == null) return null;
        Material mat = Material.matchMaterial(name.trim());
        return (mat == null || !mat.isItem() || mat.isAir()) ? null : mat;
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("materiales.yml", false);
        byMaterial.clear();
        smelts.clear();
        fuels.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        for (int i = 0; i < SECTIONS.length; i++) {
            ConfigurationSection root = yml.getConfigurationSection(SECTIONS[i]);
            if (root == null) continue;
            for (String id : root.getKeys(false)) {
                String path = SECTIONS[i] + "." + id;
                String problem = IdValidator.problem(id);
                if (problem != null) { warn(path, "id no válido (" + problem + ")"); continue; }
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) { warn(path, "no es una sección"); continue; }
                Material item = parseItem(sec.getString("item"));
                if (item == null) { warn(path + ".item", "'" + sec.getString("item") + "' no es un item válido"); continue; }
                double difficulty = sec.getDouble("dificultad", 1.0);
                if (difficulty < 0.5 || difficulty > 3.0) {
                    warn(path + ".dificultad", difficulty + " fuera de 0.5-3.0, se ajusta al límite");
                    difficulty = Math.max(0.5, Math.min(3.0, difficulty));
                }
                if (byMaterial.containsKey(item)) { warn(path + ".item", item + " ya está registrado por otra entrada"); continue; }
                double bonusRes = Math.max(0, sec.getDouble("bonus_resistencia", 0));
                double bonusDmg = Math.max(0, sec.getDouble("bonus_dano", 0));
                byMaterial.put(item, new Entry(id, KINDS[i], item, difficulty, bonusRes, bonusDmg));
            }
        }

        ConfigurationSection smeltRoot = yml.getConfigurationSection("fundicion.recetas");
        if (smeltRoot != null) {
            for (String id : smeltRoot.getKeys(false)) {
                String path = "fundicion.recetas." + id;
                String problem = IdValidator.problem(id);
                if (problem != null) { warn(path, "id no válido (" + problem + ")"); continue; }
                ConfigurationSection sec = smeltRoot.getConfigurationSection(id);
                if (sec == null) { warn(path, "no es una sección"); continue; }
                Material in = parseItem(sec.getString("entrada"));
                Material out = parseItem(sec.getString("salida"));
                if (in == null) { warn(path + ".entrada", "'" + sec.getString("entrada") + "' no es un item válido"); continue; }
                if (out == null) { warn(path + ".salida", "'" + sec.getString("salida") + "' no es un item válido"); continue; }
                smelts.put(in, new Smelt(id, in, out, Math.max(0, sec.getInt("xp", 0))));
            }
        }
        for (String name : yml.getStringList("fundicion.combustibles")) {
            Material fuel = parseItem(name);
            if (fuel == null) warn("fundicion.combustibles", "'" + name + "' no es un item válido");
            else fuels.add(fuel);
        }
        plugin.debug("Materiales: " + byMaterial.size() + ", fundiciones: " + smelts.size() + ", combustibles: " + fuels.size());
    }

    public Collection<Entry> entries() { return Collections.unmodifiableCollection(byMaterial.values()); }
    public Collection<Smelt> smelts() { return Collections.unmodifiableCollection(smelts.values()); }
    public List<Material> fuels() { return Collections.unmodifiableList(fuels); }

    /** Material registrado para el item de la mano. Un mineral custom siempre cuenta como metal. */
    public Entry forItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String mineralId = meta.getPersistentDataContainer().get(SmithingKeys.CUSTOM_MINERAL_ID, PersistentDataType.STRING);
            if (mineralId != null) return mineralEntry(mineralId);
        }
        Entry entry = byMaterial.get(stack.getType());
        // Un item renombrado o custom de otro tipo NO cuenta como el material normal (misma regla que ItemRef).
        return entry != null && ItemRef.of(entry.item()).matches(stack) ? entry : null;
    }

    /** Material registrado para una clave base (MAT:, MIN: o MLD:FORMA:<clave>). */
    public Entry forBaseKey(String baseKey) {
        if (baseKey == null) return null;
        if (baseKey.startsWith("MLD:")) {
            int idx = baseKey.indexOf(':', 4);
            return idx < 0 ? null : forBaseKey(baseKey.substring(idx + 1));
        }
        if (baseKey.startsWith("MIN:")) return mineralEntry(baseKey.substring(4));
        if (baseKey.startsWith("MAT:")) {
            Material mat = Material.matchMaterial(baseKey.substring(4));
            return mat == null ? null : byMaterial.get(mat);
        }
        return null;
    }

    private Entry mineralEntry(String mineralId) {
        CustomMineral mineral = plugin.getCustomMineralManager().get(mineralId);
        return mineral == null ? null : new Entry(mineralId, Kind.METAL, mineral.getIconMaterial(), 1.0,
                mineral.getBonusResistance(), mineral.getBonusDamageFlat());
    }

    public Smelt smeltFor(ItemStack stack) {
        if (stack == null) return null;
        Smelt smelt = smelts.get(stack.getType());
        return smelt != null && ItemRef.of(smelt.input()).matches(stack) ? smelt : null;
    }
}
