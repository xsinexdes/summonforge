package com.summonforge.gui;

import com.summonforge.data.Definition;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/** Constructor de campos editables (texto, número, booleano, selector, ítem en mano, personalizado). */
public final class Fields<T extends Definition> {
    private final List<Field<T>> list = new ArrayList<>();

    public List<Field<T>> list() {
        return list;
    }

    public void custom(String label, Material icon, String help, String hint,
                       Function<T, String> value, Field.FieldAction<T> action) {
        list.add(new Field<>(label, icon, help, hint, value, action));
    }

    public void text(String label, Material icon, String help, Function<T, String> g, BiConsumer<T, String> s) {
        list.add(new Field<>(label, icon, help, "Click: escribir en el chat", g, (ctx, click) ->
                ctx.prompt("Nuevo valor para " + label, in -> {
                    s.accept(ctx.def, in);
                    ctx.commit();
                })));
    }

    public void number(String label, Material icon, String help, ToDoubleFunction<T> g, ObjDoubleConsumer<T> s,
                       double min, double max, double step) {
        String hint = "Izq: escribir · Der: +" + fmt(step) + " · Shift+Der: -" + fmt(step) + " · rango " + fmt(min) + " a " + fmt(max);
        list.add(new Field<>(label, icon, help, hint, d -> fmt(g.applyAsDouble(d)), (ctx, click) -> {
            T d = ctx.def;
            switch (click) {
                case RIGHT -> {
                    s.accept(d, clamp(g.applyAsDouble(d) + step, min, max));
                    ctx.commit();
                }
                case SHIFT_RIGHT -> {
                    s.accept(d, clamp(g.applyAsDouble(d) - step, min, max));
                    ctx.commit();
                }
                default -> ctx.prompt("Nuevo valor para " + label + " (" + fmt(min) + " a " + fmt(max) + ")", in -> {
                    Double v = parse(in);
                    if (v == null || v < min || v > max) {
                        ctx.plugin.messages().send(ctx.player, "invalid-number", "v", in, "min", fmt(min), "max", fmt(max));
                        return;
                    }
                    s.accept(d, round(v));
                    ctx.commit();
                });
            }
        }));
    }

    public void integer(String label, Material icon, String help, ToIntFunction<T> g, ObjIntConsumer<T> s,
                        int min, int max, int step) {
        number(label, icon, help, d -> g.applyAsInt(d), (d, v) -> s.accept(d, (int) Math.round(v)), min, max, step);
    }

    public void bool(String label, Material icon, String help, Predicate<T> g, BiConsumer<T, Boolean> s) {
        list.add(new Field<>(label, icon, help, "Click: activar/desactivar", d -> g.test(d) ? "Sí" : "No", (ctx, click) -> {
            s.accept(ctx.def, !g.test(ctx.def));
            ctx.commit();
        }));
    }

    public <E> void choice(String label, Material icon, String help, Supplier<List<E>> options,
                           Function<T, E> g, BiConsumer<T, E> s) {
        list.add(new Field<>(label, icon, help, "Izq: siguiente · Der: anterior", d -> String.valueOf(g.apply(d)), (ctx, click) -> {
            List<E> opts = options.get();
            if (opts.isEmpty()) return;
            int dir = (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) ? -1 : 1;
            s.accept(ctx.def, Enums.cycle(opts, g.apply(ctx.def), dir));
            ctx.commit();
        }));
    }

    /** Toma el material del ítem que el admin lleva en la mano principal. */
    public void hand(String label, Material icon, String help, Function<T, Material> g, BiConsumer<T, Material> s) {
        list.add(new Field<>(label, icon, help, "Click: usar el ítem de tu mano", d -> g.apply(d).name(), (ctx, click) -> {
            ItemStack h = ctx.player.getInventory().getItemInMainHand();
            Material m = h.getType();
            if (m.isAir() || !m.isItem()) {
                ctx.plugin.messages().send(ctx.player, "hand-empty");
                return;
            }
            s.accept(ctx.def, m);
            ctx.commit();
        }));
    }

    private static Double parse(String in) {
        try {
            double v = Double.parseDouble(in.trim().replace(',', '.'));
            return Double.isNaN(v) || Double.isInfinite(v) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double clamp(double v, double min, double max) {
        return round(Math.max(min, Math.min(max, v)));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
