package com.summonforge.data;

import com.summonforge.core.SummonForge;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Registro en memoria + persistencia YAML (un archivo por definición, guardado atómico).
 * Antes de cada sobrescritura guarda una versión en backups/ (máx. 10 por definición).
 */
public final class DefinitionStore<T extends Definition> {
    public static final Pattern ID = Pattern.compile("[a-z0-9_]{1,32}");
    private static final int MAX_BACKUPS = 10;

    private final SummonForge plugin;
    private final String kind;
    private final Codec<T> codec;
    private final Path dir;
    private final Path backups;
    private final Map<String, T> map = new LinkedHashMap<>();

    public DefinitionStore(SummonForge plugin, String kind, Codec<T> codec) {
        this.plugin = plugin;
        this.kind = kind;
        this.codec = codec;
        Path root = plugin.getDataFolder().toPath();
        this.dir = root.resolve("data").resolve(kind);
        this.backups = root.resolve("backups").resolve(kind);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear " + dir, e);
        }
    }

    public void load() {
        map.clear();
        File[] files = dir.toFile().listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - 4);
            try {
                map.put(id, codec.read(id, YamlConfiguration.loadConfiguration(f)));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "No se pudo cargar " + kind + "/" + f.getName(), ex);
            }
        }
    }

    public T get(String id) {
        return id == null ? null : map.get(id);
    }

    public boolean has(String id) {
        return map.containsKey(id);
    }

    public Collection<T> all() {
        return map.values();
    }

    public List<String> ids() {
        return new ArrayList<>(map.keySet());
    }

    public int size() {
        return map.size();
    }

    public boolean save(T d) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            codec.write(d, y);
            String content = y.saveToString();
            String id = d.id();
            Path f = dir.resolve(id + ".yml");
            map.put(id, d);
            AsyncIO.run(plugin, () -> {
                backup(f, id);
                try {
                    AtomicFiles.write(f, content);
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + kind + "/" + id, ex);
                }
            });
            return true;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + kind + "/" + d.id(), ex);
            return false;
        }
    }

    public void delete(String id) {
        map.remove(id);
        Path f = dir.resolve(id + ".yml");
        AsyncIO.run(plugin, () -> {
            backup(f, id);
            try {
                Files.deleteIfExists(f);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "No se pudo borrar " + kind + "/" + id, ex);
            }
        });
    }

    /** Versiones guardadas de una definición, de la más reciente a la más antigua. */
    public List<File> versions(String id) {
        File[] files = backups.toFile().listFiles((d, n) -> n.startsWith(id + "-") && n.endsWith(".yml"));
        List<File> out = new ArrayList<>();
        if (files == null) return out;
        Arrays.sort(files);
        for (int i = files.length - 1; i >= 0; i--) out.add(files[i]);
        return out;
    }

    /** Restaura una versión (la actual se guarda antes como una versión más). */
    public T restore(String id, File version) {
        T d = codec.read(id, YamlConfiguration.loadConfiguration(version));
        save(d);
        return d;
    }

    /** Duplica pasando por la memoria con el mismo codec (sin código extra por tipo). */
    public T copy(T d, String newId) {
        MemoryConfiguration m = new MemoryConfiguration();
        codec.write(d, m);
        T c = codec.read(newId, m);
        save(c);
        return c;
    }

    private void backup(Path f, String id) {
        try {
            if (!Files.exists(f)) return;
            Files.createDirectories(backups);
            Files.copy(f, backups.resolve(id + "-" + System.currentTimeMillis() + ".yml"), StandardCopyOption.REPLACE_EXISTING);
            File[] old = backups.toFile().listFiles((d, n) -> n.startsWith(id + "-"));
            if (old != null && old.length > MAX_BACKUPS) {
                Arrays.sort(old);
                for (int i = 0; i < old.length - MAX_BACKUPS; i++) old[i].delete();
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.FINE, "Backup omitido para " + id, ex);
        }
    }
}
