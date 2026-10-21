package com.customsmithing.unlock;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.PlayerData;
import com.customsmithing.data.Specialization;
import com.customsmithing.recipes.RecipeTree;
import com.customsmithing.recipes.SmithingRecipe;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reglas de desbloqueo de recetas. Fabricar algo YA NO desbloquea nada: una receta se aprende
 * investigando su plano (si lo requiere) o gastando puntos de receta en el Árbol de Recetas.
 * Puntos disponibles = puntos iniciales + (nivel - 1) * puntos por nivel - puntos gastados.
 */
public final class UnlockService {

    public enum Status {
        LEARNED, AVAILABLE, NEEDS_PREREQUISITE, NEEDS_LEVEL, NEEDS_BLUEPRINT, NEEDS_POINTS
    }

    private final CustomSmithing plugin;

    public UnlockService(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    /** Costo de desbloquear una receta: costo_por_tier x (profundidad del nodo en el árbol + 1). */
    public int cost(SmithingRecipe recipe) {
        Map<String, String> parents = new LinkedHashMap<>();
        for (SmithingRecipe r : plugin.getRecipeManager().getAll()) parents.put(r.getId(), r.getPrerequisiteId());
        int tier = RecipeTree.tier(parents, recipe.getId()) + 1;
        return Math.max(0, plugin.getConfig().getInt("desbloqueo.costo_por_tier", 1)) * tier;
    }

    public int availablePoints(PlayerData data) {
        int initial = plugin.getConfig().getInt("desbloqueo.puntos_iniciales", 0);
        int perLevel = plugin.getConfig().getInt("desbloqueo.puntos_por_nivel", 1);
        return Math.max(0, initial + data.getLevel() * perLevel - data.getSpentPoints());
    }

    /**
     * Cambia la especialización respetando el enfriamiento (la primera elección es libre).
     * @return segundos que faltan para poder cambiar, o 0 si el cambio se hizo
     */
    public long changeSpecialization(Player player, Specialization spec) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        long cooldownMs = Math.max(0, plugin.getConfig().getLong("especializacion.cooldown_horas", 12)) * 3_600_000L;
        long now = System.currentTimeMillis();
        if (data.getSpecialization() != null && data.getSpecialization() != spec && cooldownMs > 0) {
            long remaining = data.getSpecChangedAt() + cooldownMs - now;
            if (remaining > 0) return (remaining + 999) / 1000;
        }
        if (data.getSpecialization() != spec) {
            data.setSpecialization(spec);
            data.setSpecChangedAt(now);
            plugin.getPlayerDataManager().save(player);
        }
        return 0;
    }

    /** Casillas extra de zona perfecta en los minijuegos de golpe, según los nodos alcanzados. */
    public int extraPerfectSlots(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        java.util.List<Integer> levels = plugin.getConfig().getIntegerList("habilidades.niveles");
        return com.customsmithing.data.Passives.extraPerfectSlots(
                com.customsmithing.data.Passives.tiersReached(data.getLevel(), levels),
                plugin.getConfig().getInt("habilidades.zona_perfecta_cada", 2));
    }

    public Status statusOf(PlayerData data, SmithingRecipe recipe) {
        if (data.hasLearned(recipe.getId())) return Status.LEARNED;
        String prerequisite = recipe.getPrerequisiteId();
        if (prerequisite != null && !data.hasLearned(prerequisite)) return Status.NEEDS_PREREQUISITE;
        if (data.getLevel() < recipe.getRequiredLevel()) return Status.NEEDS_LEVEL;
        if (recipe.isRequiresBlueprint()) return Status.NEEDS_BLUEPRINT;
        if (availablePoints(data) < cost(recipe)) return Status.NEEDS_POINTS;
        return Status.AVAILABLE;
    }

    /**
     * Intenta desbloquear la receta gastando puntos. Guarda al jugador de inmediato.
     * @return el estado que impidió el desbloqueo, o LEARNED si se desbloqueó ahora
     */
    public Status unlock(Player player, SmithingRecipe recipe) {
        PlayerData data = plugin.getPlayerDataManager().get(player);
        Status status = statusOf(data, recipe);
        if (status != Status.AVAILABLE) return status;
        data.spendPoints(cost(recipe));
        data.learnRecipe(recipe.getId());
        plugin.getPlayerDataManager().save(player);
        return Status.LEARNED;
    }
}
