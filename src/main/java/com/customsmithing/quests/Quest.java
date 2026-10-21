package com.customsmithing.quests;

public class Quest {

    public enum Type { CRAFT_ITEM, REACH_QUALITY, CRAFT_COUNT }

    private final String id;
    private final String name;
    private final Type type;
    private final String targetRecipeId; // usado por CRAFT_ITEM
    private final String targetQuality;  // usado por REACH_QUALITY
    private final int targetAmount;      // usado por CRAFT_COUNT
    private final String rewardCommand;

    public Quest(String id, String name, Type type, String targetRecipeId, String targetQuality, int targetAmount, String rewardCommand) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.targetRecipeId = targetRecipeId;
        this.targetQuality = targetQuality;
        this.targetAmount = targetAmount;
        this.rewardCommand = rewardCommand;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public String getTargetRecipeId() { return targetRecipeId; }
    public String getTargetQuality() { return targetQuality; }
    public int getTargetAmount() { return targetAmount; }
    public String getRewardCommand() { return rewardCommand; }
}
