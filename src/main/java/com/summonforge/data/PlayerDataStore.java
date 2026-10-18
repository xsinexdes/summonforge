package com.summonforge.data;

import com.summonforge.core.SummonForge;
import com.summonforge.summon.KeyAction;
import com.summonforge.summon.KeyInput;
import com.summonforge.summon.SummonMode;
import com.summonforge.util.Enums;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerDataStore implements Listener {
    private final SummonForge plugin;
    private final Path dir;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public PlayerDataStore(SummonForge plugin) {
        this.plugin = plugin;
        this.dir = plugin.getDataFolder().toPath().resolve("data").resolve("players");
    }

    public PlayerData get(UUID id) {
        return cache.computeIfAbsent(id, this::load);
    }

    private PlayerData load(UUID id) {
        PlayerData d = new PlayerData(id);
        Path f = dir.resolve(id + ".yml");
        if (Files.exists(f)) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f.toFile());
            d.summonId = y.getString("summon", "");
            d.level = Math.max(1, Math.min(PlayerData.MAX_LEVEL, y.getInt("level", 1)));
            d.equipped.addAll(y.getStringList("equipped"));
            d.loyalty = Math.max(0, y.getInt("loyalty", 0));
            d.sync = Math.max(0, Math.min(100, y.getInt("sync", 0)));
            for (KeyAction a : KeyAction.values()) {
                String k = y.getString("keys." + a.name());
                if (k != null) d.keys.put(a, Enums.parse(KeyInput.class, k, d.keys.get(a)));
            }
            ConfigurationSection t = y.getConfigurationSection("talents");
            if (t != null) for (String k : t.getKeys(false)) d.talents.put(k, t.getInt(k));
            ConfigurationSection bk = y.getConfigurationSection("boss-kills");
            if (bk != null) for (String k : bk.getKeys(false)) d.bossKills.put(k, bk.getInt(k));
            d.mode = Enums.parse(SummonMode.class, y.getString("mode"), SummonMode.AGGRESSIVE);
        }
        return d;
    }

    public void save(PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("summon", d.summonId);
        y.set("level", d.level);
        y.set("equipped", d.equipped);
        y.set("loyalty", d.loyalty);
        y.set("sync", d.sync);
        d.keys.forEach((k, v) -> y.set("keys." + k.name(), v.name()));
        d.talents.forEach((k, v) -> y.set("talents." + k, v));
        d.bossKills.forEach((k, v) -> y.set("boss-kills." + k, v));
        y.set("mode", d.mode.name());
        String content = y.saveToString();
        Path f = dir.resolve(d.uuid + ".yml");
        AsyncIO.run(plugin, () -> {
            try {
                AtomicFiles.write(f, content);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo guardar el jugador " + d.uuid, e);
            }
        });
    }

    public void saveAll() {
        cache.values().forEach(this::save);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PlayerData d = cache.remove(e.getPlayer().getUniqueId());
        if (d != null) save(d);
    }
}
