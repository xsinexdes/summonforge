package com.customsmithing.quests;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Quality;
import com.customsmithing.recipes.SmithingRecipe;
import com.customsmithing.util.IdValidator;
import com.customsmithing.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QuestManager {

    private final CustomSmithing plugin;
    private final File file;
    private final File progressFile;
    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Set<String> completed = new HashSet<>();

    public QuestManager(CustomSmithing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        this.progressFile = new File(plugin.getDataFolder(), "quests_progress.yml");
        load();
        loadProgress();
    }

    /** El progreso y las misiones completadas sobreviven a reinicios (antes se perdían y los premios se repetían). */
    private void loadProgress() {
        progress.clear();
        completed.clear();
        if (!progressFile.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(progressFile);
        ConfigurationSection players = yml.getConfigurationSection("jugadores");
        if (players == null) return;
        for (String uuidText : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("quests_progress.yml: '" + uuidText + "' no es un UUID válido, se ignora.");
                continue;
            }
            for (String questId : players.getStringList(uuidText + ".completadas")) completed.add(uuid + ":" + questId);
            ConfigurationSection prog = players.getConfigurationSection(uuidText + ".progreso");
            if (prog == null) continue;
            Map<String, Integer> map = new HashMap<>();
            for (String questId : prog.getKeys(false)) map.put(questId, prog.getInt(questId));
            progress.put(uuid, map);
        }
    }

    public void saveProgress() {
        YamlConfiguration yml = new YamlConfiguration();
        Map<UUID, List<String>> done = new HashMap<>();
        for (String key : completed) {
            int idx = key.indexOf(':');
            if (idx < 0) continue;
            try {
                done.computeIfAbsent(UUID.fromString(key.substring(0, idx)), u -> new ArrayList<>()).add(key.substring(idx + 1));
            } catch (IllegalArgumentException ignored) {
                // clave dañada: no se guarda
            }
        }
        for (var entry : done.entrySet()) yml.set("jugadores." + entry.getKey() + ".completadas", entry.getValue());
        for (var entry : progress.entrySet()) {
            for (var q : entry.getValue().entrySet()) yml.set("jugadores." + entry.getKey() + ".progreso." + q.getKey(), q.getValue());
        }
        try {
            yml.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar quests_progress.yml: " + e.getMessage());
        }
    }

    public boolean isCompleted(UUID player, String questId) {
        return completed.contains(player + ":" + questId);
    }

    public int progressOf(UUID player, String questId) {
        return progress.getOrDefault(player, Map.of()).getOrDefault(questId, 0);
    }

    public void load() {
        if (!file.exists()) plugin.saveResource("quests.yml", false);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        quests.clear();

        ConfigurationSection root = yml.getConfigurationSection("misiones");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String idProblem = IdValidator.problem(id);
            if (idProblem != null) {
                plugin.getLogger().warning("quests.yml › misiones." + id + ": id no válido (" + idProblem + ") — misión omitida.");
                continue;
            }
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                plugin.getLogger().warning("quests.yml › misiones." + id + ": no es una sección — misión omitida.");
                continue;
            }
            Quest.Type type;
            try {
                type = Quest.Type.valueOf(sec.getString("tipo", "CRAFT_COUNT"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("quests.yml › misiones." + id + ".tipo: '" + sec.getString("tipo")
                        + "' no existe — misión omitida.");
                continue;
            }
            Quest quest = new Quest(
                    id,
                    sec.getString("nombre", id),
                    type,
                    sec.getString("receta", null),
                    sec.getString("calidad", null),
                    sec.getInt("cantidad", 1),
                    sec.getString("recompensa_comando", null)
            );
            quests.put(id, quest);
        }
    }

    public Collection<Quest> getAll() { return quests.values(); }

    public void onItemCrafted(Player player, SmithingRecipe recipe, Quality quality) {
        for (Quest quest : quests.values()) {
            String key = player.getUniqueId() + ":" + quest.getId();
            if (completed.contains(key)) continue;

            boolean matches = switch (quest.getType()) {
                case CRAFT_ITEM -> recipe.getId().equals(quest.getTargetRecipeId());
                case REACH_QUALITY -> quest.getTargetQuality() != null && quality.name().equals(quest.getTargetQuality());
                case CRAFT_COUNT -> true;
            };
            if (!matches) continue;

            Map<String, Integer> playerProgress = progress.computeIfAbsent(player.getUniqueId(), u -> new HashMap<>());
            int current = playerProgress.getOrDefault(quest.getId(), 0) + 1;
            playerProgress.put(quest.getId(), current);
            boolean justCompleted = current >= quest.getTargetAmount();

            if (justCompleted) {
                completed.add(key);
                Messages.send(player, "quests.completada", "%nombre%", quest.getName());
                if (quest.getRewardCommand() != null && !quest.getRewardCommand().isBlank()) {
                    String cmd = quest.getRewardCommand().replace("%player%", player.getName());
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                }
            }
            saveProgress();
        }
    }
}
