package com.customsmithing.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private int level = 1;
    private int xp = 0;
    private Specialization specialization = null;
    private double avgTimingAccuracy = 50.0; // 0-100, usado por la IA adaptativa
    private int totalHits = 0;
    private int totalCrafts = 0;
    private final Set<String> learnedRecipes = new HashSet<>();
    /** Puntos de receta ya gastados en desbloquear. Los disponibles se calculan con el nivel (ver UnlockService). */
    private int spentPoints = 0;
    /** Nivel de mejora de cada estación (clave = "calentador", "forja"...). */
    private final java.util.Map<String, Integer> stationLevels = new java.util.HashMap<>();
    /** Piezas procesadas por material registrado (clave = id del material, p. ej. "hierro"). */
    private final java.util.Map<String, Integer> mastery = new java.util.HashMap<>();
    /** Momento (ms) del último cambio de especialización; 0 = nunca. */
    private long specChangedAt = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public Specialization getSpecialization() { return specialization; }
    public void setSpecialization(Specialization specialization) { this.specialization = specialization; }

    public double getAvgTimingAccuracy() { return avgTimingAccuracy; }
    public void setAvgTimingAccuracy(double value) { this.avgTimingAccuracy = Math.max(0, Math.min(100, value)); }

    public int getTotalHits() { return totalHits; }
    public void setTotalHits(int value) { this.totalHits = Math.max(0, value); }

    public java.util.Map<String, Integer> getStationLevels() { return stationLevels; }
    public java.util.Map<String, Integer> getMastery() { return mastery; }
    public int getStationLevel(String station) { return stationLevels.getOrDefault(station, 0); }
    public void setStationLevel(String station, int level) { stationLevels.put(station, Math.max(0, level)); }
    public int getMasteryCount(String materialId) { return mastery.getOrDefault(materialId, 0); }
    public void addMastery(String materialId) { mastery.merge(materialId, 1, Integer::sum); }

    public long getSpecChangedAt() { return specChangedAt; }
    public void setSpecChangedAt(long value) { this.specChangedAt = Math.max(0, value); }

    public int getSpentPoints() { return spentPoints; }
    public void setSpentPoints(int value) { this.spentPoints = Math.max(0, value); }
    public void spendPoints(int amount) { this.spentPoints += Math.max(0, amount); }

    public int getTotalCrafts() { return totalCrafts; }
    public void incrementCrafts() { totalCrafts++; }
    public void setTotalCrafts(int value) { this.totalCrafts = Math.max(0, value); }

    public Set<String> getLearnedRecipes() { return learnedRecipes; }
    public boolean hasLearned(String recipeId) { return learnedRecipes.contains(recipeId); }
    public void learnRecipe(String recipeId) { learnedRecipes.add(recipeId); }

    /**
     * IA de fabricacion (heuristica simple): actualiza el promedio movil
     * de precision del jugador cada vez que golpea en el minijuego.
     */
    public void registerHitAccuracy(double accuracyThisHit) {
        totalHits++;
        double weight = Math.min(1.0 / totalHits, 0.2); // se estabiliza con el tiempo
        avgTimingAccuracy = avgTimingAccuracy * (1 - weight) + accuracyThisHit * weight;
    }

    /**
     * Añade XP y sube de nivel usando una curva simple (100 * nivel).
     * @return true si subio al menos un nivel
     */
    public boolean addXp(int amount) {
        xp += amount;
        boolean leveled = false;
        int required = requiredXpForNextLevel();
        while (xp >= required) {
            xp -= required;
            level++;
            leveled = true;
            required = requiredXpForNextLevel();
        }
        return leveled;
    }

    public int requiredXpForNextLevel() {
        return 100 * level;
    }
}
