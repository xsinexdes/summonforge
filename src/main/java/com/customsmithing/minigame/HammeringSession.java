package com.customsmithing.minigame;

/**
 * Mecanica generica de "barra con cursor" usada por los minijuegos de
 * Calentado, Golpeado y Enfriado. No depende de ninguna receta: solo
 * calcula puntaje segun que tan cerca del centro caen los golpes.
 */
public class HammeringSession {

    private int hitsDone = 0;
    private final int hitsRequired;
    private double accumulatedScore = 0.0;
    private int cursorPosition = 0;
    private boolean movingRight = true;
    private int taskId = -1;
    private final int perfectSlot = 4; // centro de una barra de 9 slots
    private int perfectZoneSize;
    private int goodZoneSize;
    private boolean failed = false;

    public HammeringSession(int hitsRequired, int perfectZoneSize, int goodZoneSize) {
        this.hitsRequired = hitsRequired;
        this.perfectZoneSize = perfectZoneSize;
        this.goodZoneSize = goodZoneSize;
    }

    public int getHitsDone() { return hitsDone; }
    public int getHitsRequired() { return hitsRequired; }
    public double getAccumulatedScore() { return accumulatedScore; }
    public int getCursorPosition() { return cursorPosition; }
    public void setCursorPosition(int pos) { this.cursorPosition = pos; }
    public boolean isMovingRight() { return movingRight; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public int getPerfectSlot() { return perfectSlot; }
    public int getPerfectZoneSize() { return perfectZoneSize; }
    public void setPerfectZoneSize(int size) { this.perfectZoneSize = size; }
    public int getGoodZoneSize() { return goodZoneSize; }
    public boolean isFailed() { return failed; }
    public void setFailed(boolean failed) { this.failed = failed; }
    public boolean isComplete() { return hitsDone >= hitsRequired; }

    /**
     * Registra un golpe segun la distancia del cursor al centro perfecto.
     * @return tipo de resultado: PERFECTO, BUENO o FALLO
     */
    public HitResult registerHit() {
        int distance = Math.abs(cursorPosition - perfectSlot);
        hitsDone++;
        HitResult result;
        if (distance <= perfectZoneSize) {
            accumulatedScore += 20;
            result = HitResult.PERFECTO;
        } else if (distance <= goodZoneSize) {
            accumulatedScore += 10;
            result = HitResult.BUENO;
        } else {
            accumulatedScore -= 5;
            result = HitResult.FALLO;
        }
        return result;
    }

    public enum HitResult { PERFECTO, BUENO, FALLO }
}
