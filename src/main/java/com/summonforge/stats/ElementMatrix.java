package com.summonforge.stats;

import com.summonforge.core.SummonForge;
import com.summonforge.data.AsyncIO;
import com.summonforge.data.AtomicFiles;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/** Multiplicador de daño atacante>defensor, editable por GUI (data/elements.yml). NONE siempre vale 1. */
public final class ElementMatrix {
    private final SummonForge plugin;
    private final File file;
    private final Map<String, Double> map = new HashMap<>();

    public ElementMatrix(SummonForge plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/elements.yml");
        load();
    }

    public void load() {
        map.clear();
        if (!file.exists()) {
            defaults();
            save();
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) map.put(k, y.getDouble(k, 1.0));
    }

    private void defaults() {
        strong(Element.WATER, Element.FIRE);
        strong(Element.FIRE, Element.ICE);
        strong(Element.ICE, Element.WIND);
        strong(Element.WIND, Element.EARTH);
        strong(Element.EARTH, Element.LIGHTNING);
        strong(Element.LIGHTNING, Element.WATER);
        strong(Element.LIGHT, Element.SHADOW);
        strong(Element.SHADOW, Element.LIGHT);
        strong(Element.VOID, Element.LIGHT);
    }

    private void strong(Element a, Element d) {
        map.putIfAbsent(a.name() + ">" + d.name(), 1.5);
        map.putIfAbsent(d.name() + ">" + a.name(), 0.75);
    }

    public double get(Element attack, Element defense) {
        if (attack == Element.NONE || defense == Element.NONE) return 1.0;
        return map.getOrDefault(attack.name() + ">" + defense.name(), 1.0);
    }

    public void set(Element attack, Element defense, double value) {
        map.put(attack.name() + ">" + defense.name(), Math.max(0, Math.min(5, Math.round(value * 100.0) / 100.0)));
        save();
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        map.forEach(y::set);
        String content = y.saveToString();
        AsyncIO.run(plugin, () -> {
            try {
                AtomicFiles.write(file.toPath(), content);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo guardar elements.yml", e);
            }
        });
    }
}
