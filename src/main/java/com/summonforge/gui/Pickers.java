package com.summonforge.gui;

import com.summonforge.ability.AbilityDef;
import com.summonforge.core.SummonForge;
import com.summonforge.summon.SummonDef;
import com.summonforge.model.ModelPreview;
import com.summonforge.util.Items;
import com.summonforge.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Selectores reutilizables: modelo, animación, habilidades y confirmación. */
public final class Pickers {
    private Pickers() {}

    public static final class ModelPicker extends PagedMenu<String> {
        private final Consumer<String> onPick;
        private final Runnable onBack;

        public ModelPicker(SummonForge plugin, Player viewer, Consumer<String> onPick, Runnable onBack) {
            super(plugin, viewer, "Elegir modelo (BetterModel)");
            this.onPick = onPick;
            this.onBack = onBack;
        }

        @Override protected List<String> entries() { return new ArrayList<>(plugin.model().modelKeys()); }
        @Override protected String label(String k) { return k; }
        @Override protected void back() { onBack.run(); }
        @Override
        protected void picked(String k, ClickType c) {
            if (c == ClickType.RIGHT) {
                viewer.closeInventory();
                ModelPreview.show(plugin, viewer, k);
            } else {
                onPick.accept(k);
            }
        }

        @Override
        protected ItemStack render(String k) {
            return Items.icon(Material.ARMOR_STAND, "<white>" + Text.escape(k),
                    "<gray>Animaciones: <white>" + plugin.model().animations(k).size(), "<yellow>Izq: elegir", "<yellow>Der: previsualizar 15 s");
        }

        @Override
        protected void extra() {
            set(48, Items.icon(Material.GLASS, "<gray>(sin modelo)", "<yellow>Click: quitar el modelo"), (p, t) -> onPick.accept(""));
        }
    }

    public static final class AnimationPicker extends PagedMenu<String> {
        private final String modelId;
        private final Consumer<String> onPick;
        private final Runnable onBack;

        public AnimationPicker(SummonForge plugin, Player viewer, String modelId, Consumer<String> onPick, Runnable onBack) {
            super(plugin, viewer, "Elegir animación");
            this.modelId = modelId;
            this.onPick = onPick;
            this.onBack = onBack;
        }

        @Override protected List<String> entries() { return plugin.model().animations(modelId); }
        @Override protected String label(String k) { return k; }
        @Override protected void back() { onBack.run(); }
        @Override protected void picked(String k, ClickType c) { onPick.accept(k); }

        @Override
        protected ItemStack render(String k) {
            return Items.icon(Material.NOTE_BLOCK, "<white>" + Text.escape(k), "<yellow>Click: elegir");
        }

        @Override
        protected void extra() {
            set(48, Items.icon(Material.GLASS, "<gray>(ninguna)", "<yellow>Click: sin animación"), (p, t) -> onPick.accept(""));
        }
    }

    /** Alterna habilidades en la lista recibida; al volver, quien llama guarda. */
    public static final class AbilityPicker extends PagedMenu<AbilityDef> {
        private final List<String> selected;
        private final Runnable onBack;

        public AbilityPicker(SummonForge plugin, Player viewer, List<String> selected, Runnable onBack) {
            super(plugin, viewer, "Elegir habilidades");
            this.selected = selected;
            this.onBack = onBack;
        }

        @Override
        protected List<AbilityDef> entries() {
            List<AbilityDef> l = new ArrayList<>(plugin.abilityStore().all());
            l.sort(Comparator.comparing(a -> a.id));
            return l;
        }

        @Override protected String label(AbilityDef d) { return d.id + " " + Text.plain(Text.mm(d.name)); }
        @Override protected void back() { onBack.run(); }

        @Override
        protected ItemStack render(AbilityDef d) {
            boolean on = selected.contains(d.id);
            ItemStack s = Items.icon(d.icon, d.name, "<gray>" + Text.escape(d.description),
                    on ? "<green>✔ Equipada (orden " + (selected.indexOf(d.id) + 1) + ")" : "<gray>✘ No equipada", "<yellow>Click: alternar");
            return on ? Items.glow(s) : s;
        }

        @Override
        protected void picked(AbilityDef d, ClickType c) {
            if (!selected.remove(d.id)) selected.add(d.id);
            refresh();
        }
    }

    /** Alterna invocaciones en la lista recibida (ramas de evolución). */
    public static final class SummonPicker extends PagedMenu<SummonDef> {
        private final List<String> selected;
        private final Runnable onBack;

        public SummonPicker(SummonForge plugin, Player viewer, List<String> selected, Runnable onBack) {
            super(plugin, viewer, "Elegir invocaciones");
            this.selected = selected;
            this.onBack = onBack;
        }

        @Override
        protected List<SummonDef> entries() {
            List<SummonDef> l = new ArrayList<>(plugin.summonStore().all());
            l.sort(Comparator.comparing(s -> s.id));
            return l;
        }

        @Override protected String label(SummonDef d) { return d.id + " " + Text.plain(Text.mm(d.name)); }
        @Override protected void back() { onBack.run(); }

        @Override
        protected ItemStack render(SummonDef d) {
            boolean on = selected.contains(d.id);
            ItemStack s = Items.icon(d.icon, d.name, "<gray>ID: <white>" + d.id, on ? "<green>✔ Elegida" : "<gray>✘ No elegida", "<yellow>Click: alternar");
            return on ? Items.glow(s) : s;
        }

        @Override
        protected void picked(SummonDef d, ClickType c) {
            if (!selected.remove(d.id)) selected.add(d.id);
            refresh();
        }
    }

    public record Option(Material icon, String name, String lore, Runnable action) {}

    /** Menú de 2-3 opciones. */
    public static final class Choice extends AbstractMenu {
        private final List<Option> options;

        public Choice(SummonForge plugin, Player viewer, String title, List<Option> options) {
            super(plugin, viewer, title, 3);
            this.options = options;
        }

        @Override
        public String permission() {
            return "summonforge.use";
        }

        @Override
        protected void build() {
            for (int i = 0; i < 27; i++) set(i, filler(), null);
            int[] pos = options.size() == 2 ? new int[]{11, 15} : new int[]{10, 13, 16};
            for (int i = 0; i < options.size() && i < pos.length; i++) {
                Option o = options.get(i);
                set(pos[i], Items.icon(o.icon(), o.name(), o.lore()), (p, t) -> o.action().run());
            }
        }
    }

    public static final class Confirm extends AbstractMenu {
        private final String info;
        private final Runnable yes;
        private final Runnable no;

        public Confirm(SummonForge plugin, Player viewer, String title, String info, Runnable yes, Runnable no) {
            super(plugin, viewer, title, 3);
            this.info = info;
            this.yes = yes;
            this.no = no;
        }

        @Override
        public String permission() {
            return "summonforge.use";
        }

        @Override
        protected void build() {
            for (int i = 0; i < 27; i++) set(i, filler(), null);
            set(11, Items.icon(Material.LIME_CONCRETE, "<green>Confirmar", info), (p, t) -> yes.run());
            set(15, Items.icon(Material.RED_CONCRETE, "<red>Cancelar"), (p, t) -> no.run());
        }
    }
}
