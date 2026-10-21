package com.customsmithing.item;

import com.customsmithing.data.ProcessStage;
import com.customsmithing.data.SmithingKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Lectura y escritura de los datos de una pieza intermedia (etapa, material base, puntaje). */
public final class PieceData {

    /** Puntaje que se asume si la pieza no trae uno guardado. */
    public static final double DEFAULT_SCORE = 50.0;

    public record Data(ProcessStage stage, String baseKey, double score) {
    }

    private PieceData() {
    }

    /** @return los datos de la pieza, o null si el item no es una pieza procesada */
    public static Data read(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        ProcessStage stage = ProcessStage.fromName(pdc.get(SmithingKeys.PROCESS_STATE, PersistentDataType.STRING));
        if (stage == null) return null;
        Double score = pdc.get(SmithingKeys.QUALITY_SCORE, PersistentDataType.DOUBLE);
        return new Data(stage, pdc.get(SmithingKeys.BASE_MATERIAL, PersistentDataType.STRING),
                score != null ? score : DEFAULT_SCORE);
    }

    /** ¿Es una pieza que ya pasó por alguna etapa? (tenga o no material base) */
    public static boolean isProcessed(ItemStack stack) {
        return read(stack) != null;
    }

    public static void write(ItemMeta meta, ProcessStage stage, String baseKey, double score) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(SmithingKeys.PROCESS_STATE, PersistentDataType.STRING, stage.name());
        pdc.set(SmithingKeys.QUALITY_SCORE, PersistentDataType.DOUBLE, score);
        if (baseKey != null) pdc.set(SmithingKeys.BASE_MATERIAL, PersistentDataType.STRING, baseKey);
    }
}
