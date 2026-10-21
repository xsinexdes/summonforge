package com.summonforge.gui;

import com.summonforge.ability.AbilityTemplates;
import com.summonforge.core.SummonForge;
import com.summonforge.util.Items;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** Plantillas de habilidades (poderes oculares, dimensiones, ilusiones...) para crear o rehacer una rápido. */
public final class TemplateMenu extends PagedMenu<AbilityTemplates.Template> {
    private final Consumer<AbilityTemplates.Template> onPick;
    private final Runnable onBack;

    public TemplateMenu(SummonForge plugin, Player viewer, Consumer<AbilityTemplates.Template> onPick, Runnable onBack) {
        super(plugin, viewer, "Plantillas rápidas");
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override protected List<AbilityTemplates.Template> entries() { return AbilityTemplates.ALL; }
    @Override protected String label(AbilityTemplates.Template t) { return t.label(); }
    @Override protected void picked(AbilityTemplates.Template t, ClickType c) { onPick.accept(t); }
    @Override protected void back() { onBack.run(); }

    @Override
    protected ItemStack render(AbilityTemplates.Template t) {
        return Items.icon(t.icon(), "<yellow>" + t.label(), "<gray>" + t.help(), "", "<yellow>Click: usar esta plantilla");
    }
}
