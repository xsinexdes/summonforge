package com.customsmithing.alloys;

import com.customsmithing.item.ItemRef;

/** Aleación: N items de entrada -> M items de salida. Ambos pueden ser vanilla o custom (ItemRef). */
public class AlloyRecipe {

    private final String id;
    private String displayName;
    private ItemRef input;
    private int inputAmount = 1;
    private ItemRef output;
    private int outputAmount = 1;

    public AlloyRecipe(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public ItemRef getInput() { return input; }
    public void setInput(ItemRef input) { this.input = input; }

    public int getInputAmount() { return inputAmount; }
    public void setInputAmount(int inputAmount) { this.inputAmount = Math.max(1, inputAmount); }

    public ItemRef getOutput() { return output; }
    public void setOutput(ItemRef output) { this.output = output; }

    public int getOutputAmount() { return outputAmount; }
    public void setOutputAmount(int outputAmount) { this.outputAmount = Math.max(1, outputAmount); }
}
