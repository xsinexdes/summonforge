package com.customsmithing.gui;

import com.customsmithing.CustomSmithing;
import com.customsmithing.quests.Quest;
import com.customsmithing.util.Theme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Lista de misiones con el progreso del jugador. Solo lectura. */
public class QuestGUI extends PagedMenu<Quest> {

    public QuestGUI(CustomSmithing plugin, Player player) {
        super(plugin, player, "\u2694 Misiones de Herrería");
    }

    @Override
    protected List<Quest> entries() {
        return new ArrayList<>(plugin.getQuestManager().getAll());
    }

    @Override
    protected String emptyText() {
        return "No hay misiones configuradas.";
    }

    @Override
    protected ItemStack entryIcon(Quest quest) {
        var quests = plugin.getQuestManager();
        boolean done = quests.isCompleted(player.getUniqueId(), quest.getId());
        int progress = Math.min(quests.progressOf(player.getUniqueId(), quest.getId()), quest.getTargetAmount());
        String goal = switch (quest.getType()) {
            case CRAFT_ITEM -> "Fabricar la receta indicada";
            case REACH_QUALITY -> "Fabricar objetos de calidad " + (quest.getTargetQuality() == null ? "?" : quest.getTargetQuality());
            case CRAFT_COUNT -> "Fabricar objetos";
        };
        return icon("plano.pendiente", done ? Material.WRITTEN_BOOK : Material.WRITABLE_BOOK,
                (done ? Theme.OK : Theme.WHITE) + quest.getName(),
                Theme.muted(goal),
                Theme.field("Progreso", progress + "/" + quest.getTargetAmount()),
                done ? Theme.ok("\u2714 Completada (premio ya entregado)") : Theme.warn("En curso"));
    }
}
