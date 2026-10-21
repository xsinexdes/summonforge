package com.customsmithing.piece;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.ProcessStage;
import com.customsmithing.skin.SkinService;
import com.customsmithing.util.ItemBuilder;
import com.customsmithing.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantillas de las piezas intermedias (nombre, lore y skin de cada etapa), editables en piezas.yml.
 * Si falta una clave en el archivo del servidor se usa la que trae el jar. Marcadores disponibles:
 * %material%, %puntaje% y %nota% (solo Tallado/Tejido: texto distinto según la calidad del trabajo).
 */
public final class PieceCatalog {

    private final CustomSmithing plugin;
    private final File file;
    private YamlConfiguration yml;

    public PieceCatalog(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "piezas.yml");
        load();
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("piezas.yml", false);
        yml = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("piezas.yml")) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    yml.setDefaults(YamlConfiguration.loadConfiguration(reader));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudieron leer los valores por defecto de piezas.yml: " + e.getMessage());
        }
    }

    /** Id de skin configurado para una etapa (puede no existir en skins.yml). */
    public String skinOf(ProcessStage stage) {
        return yml.getString("piezas." + stage.name() + ".skin", "");
    }

    /**
     * Construye la pieza de una etapa.
     * @param materialName nombre legible del material (ya resuelto, p. ej. "Oro")
     * @param icon         item base si no hay skin activa
     */
    public ItemStack build(ProcessStage stage, String baseKey, String materialName, Material icon, double score) {
        String path = "piezas." + stage.name();
        String name = yml.getString(path + ".nombre", "&7Pieza de %material%");

        double goodFrom = plugin.getConfig().getDouble("calidad.umbral_raro", 40) + 25;
        String note = yml.getString(path + (score >= goodFrom ? ".nota_buena" : ".nota_mala"), "");

        List<String> lore = new ArrayList<>();
        for (String line : yml.getStringList(path + ".lore")) {
            lore.add(line.replace("%material%", materialName).replace("%puntaje%", Text.fmt1(score)).replace("%nota%", note));
        }

        SkinService.Skin skin = plugin.getSkinService().skinFor(yml.getString(path + ".skin", ""));
        ItemStack piece = new ItemBuilder(skin != null ? skin.material() : icon)
                .name(name.replace("%material%", materialName))
                .lore(lore)
                .markSmithingItem()
                .piece(stage, baseKey, score)
                .build();
        if (skin != null) {
            ItemMeta meta = piece.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(skin.modelData());
                piece.setItemMeta(meta);
            }
        }
        return piece;
    }
}
