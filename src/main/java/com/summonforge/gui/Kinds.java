package com.summonforge.gui;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.ability.Effect;
import com.summonforge.ability.Shape;
import com.summonforge.boss.BossDef;
import com.summonforge.cinematic.CinematicDef;
import com.summonforge.boss.Phase;
import com.summonforge.core.SummonForge;
import com.summonforge.data.Definition;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemFactory;
import com.summonforge.model.AnimationSet;
import com.summonforge.model.Modeled;
import com.summonforge.stats.Element;
import com.summonforge.status.StatusDef;
import com.summonforge.summon.SummonDef;
import com.summonforge.summon.SummonType;
import com.summonforge.util.Fx;
import com.summonforge.util.Mobs;
import com.summonforge.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Define, para cada tipo de contenido, qué campos se editan por GUI y qué hace "Probar". */
public final class Kinds {
    public final DefinitionKind<AbilityDef> ability;
    public final DefinitionKind<SummonDef> summon;
    public final DefinitionKind<BossDef> boss;
    public final DefinitionKind<ItemDef> item;
    public final DefinitionKind<StatusDef> status;
    public final DefinitionKind<CinematicDef> cinematic;

    public Kinds(SummonForge pl) {
        ability = new DefinitionKind<>("Habilidad", "Habilidades", Material.BLAZE_POWDER, pl.abilityStore(),
                AbilityDef::new, abilityFields(pl), (p, d) -> {
            CastContext cc = new CastContext(p, p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.0);
            pl.abilities().cast(cc, d, true);
        });
        status = new DefinitionKind<>("Estado", "Estados", Material.SPIDER_EYE, pl.statusStore(),
                StatusDef::new, statusFields(), (p, d) ->
                pl.statuses().apply(p, d.id, 100, 1, null));
        summon = new DefinitionKind<>("Invocación", "Invocaciones", Material.NETHER_STAR, pl.summonStore(),
                SummonDef::new, summonFields(pl), (p, d) -> pl.summons().preview(p, d));
        boss = new DefinitionKind<>("Boss", "Bosses", Material.WITHER_SKELETON_SKULL, pl.bossStore(),
                BossDef::new, bossFields(pl), (p, d) -> {
            Location l = p.getLocation().add(Fx.forward(p.getLocation()).multiply(6));
            if (pl.bosses().spawn(d, l)) pl.messages().send(p, "boss-spawned", "id", d.id);
        });
        cinematic = new DefinitionKind<>("Cinemática", "Cinemáticas", Material.FILLED_MAP, pl.cinematicStore(),
                CinematicDef::new, cinematicFields(pl), (p, d) -> pl.cinematics().play(d.id, List.of(p), p.getLocation()));
        item = new DefinitionKind<>("Objeto", "Objetos", Material.PAPER, pl.itemStore(),
                ItemDef::new, itemFields(pl), (p, d) -> {
            p.getInventory().addItem(ItemFactory.create(d));
            pl.messages().send(p, "item-given", "item", d.id, "player", p.getName());
        });
    }

    private static List<Field<AbilityDef>> abilityFields(SummonForge pl) {
        Fields<AbilityDef> f = new Fields<>();
        f.text("Nombre", Material.NAME_TAG, "Admite MiniMessage", d -> d.name, (d, v) -> d.name = v);
        f.text("Descripción", Material.BOOK, "Texto corto", d -> d.description, (d, v) -> d.description = v);
        f.hand("Icono", Material.ITEM_FRAME, "Ítem que la representa", d -> d.icon, (d, m) -> d.icon = m);
        f.choice("Forma", Material.COMPASS, "Geometría del ataque", () -> List.of(Shape.values()), d -> d.shape, (d, v) -> d.shape = v);
        f.choice("Efecto", Material.BLAZE_ROD, "Qué hace al impactar", () -> List.of(Effect.values()), d -> d.effect, (d, v) -> d.effect = v);
        f.choice("Elemento", Material.AMETHYST_SHARD, "Elemento del daño", () -> List.of(Element.values()), d -> d.element, (d, v) -> d.element = v);
        f.choice("Habilidad especial", Material.NETHER_STAR, "Lógica propia (clones, teleport, cadenas...)", () -> {
            List<String> l = new ArrayList<>();
            l.add("");
            l.addAll(pl.specials().ids());
            return l;
        }, d -> d.special, (d, v) -> d.special = v);
        f.number("Daño / curación", Material.IRON_SWORD, "Valor base (escala con el nivel)", d -> d.damage, (d, v) -> d.damage = v, 0, 100000, 1);
        f.number("Radio", Material.HEART_OF_THE_SEA, "Círculo y explosión", d -> d.radius, (d, v) -> d.radius = v, 0, 40, 0.5);
        f.number("Alcance", Material.ARROW, "Cono, línea y proyectil", d -> d.range, (d, v) -> d.range = v, 1, 60, 1);
        f.number("Ángulo", Material.SPYGLASS, "Apertura del cono", d -> d.angle, (d, v) -> d.angle = v, 10, 360, 10);
        f.number("Velocidad proyectil", Material.FEATHER, "Bloques por tick", d -> d.speed, (d, v) -> d.speed = v, 0.2, 4, 0.1);
        f.number("Empuje", Material.PISTON, "Knockback / fuerza de empuje", d -> d.knockback, (d, v) -> d.knockback = v, 0, 5, 0.1);
        f.integer("Cooldown (ticks)", Material.CLOCK, "20 ticks = 1 segundo", d -> d.cooldownTicks, (d, v) -> d.cooldownTicks = v, 0, 72000, 20);
        f.integer("Aviso (ticks)", Material.REDSTONE_TORCH, "Zona telegrafiada antes del impacto", d -> d.warnTicks, (d, v) -> d.warnTicks = v, 0, 200, 5);
        f.choice("Partícula", Material.FIREWORK_STAR, "Visual principal", () -> Fx.PARTICLES, d -> d.particle, (d, v) -> d.particle = v);
        f.text("Sonido", Material.NOTE_BLOCK, "Clave vanilla, ej. entity.blaze.shoot", d -> d.sound, (d, v) -> d.sound = v);
        f.choice("Estado que aplica", Material.SPIDER_EYE, "Al impactar (o efecto Estado)", () -> {
            List<String> l = new ArrayList<>();
            l.add("");
            l.addAll(pl.statusStore().ids());
            return l;
        }, d -> d.status, (d, v) -> d.status = v);
        f.integer("Duración del estado (ticks)", Material.CLOCK, "20 ticks = 1 s", d -> d.statusTicks, (d, v) -> d.statusTicks = v, 20, 2400, 20);
        f.integer("Stacks por impacto", Material.GLOWSTONE_DUST, "Acumulación", d -> d.statusStacks, (d, v) -> d.statusStacks = v, 1, 10, 1);
        return f.list();
    }

    private static List<String> cineIds(SummonForge pl) {
        List<String> l = new ArrayList<>();
        l.add("");
        l.addAll(pl.cinematicStore().ids());
        return l;
    }

    private static List<Field<CinematicDef>> cinematicFields(SummonForge pl) {
        Fields<CinematicDef> f = new Fields<>();
        f.text("Nombre", Material.NAME_TAG, "Admite MiniMessage", d -> d.name, (d, v) -> d.name = v);
        f.hand("Icono", Material.ITEM_FRAME, "Ítem que la representa", d -> d.icon, (d, m) -> d.icon = m);
        f.custom("Pasos", Material.WRITABLE_BOOK, "Secuencia de la cinemática", "Izq: añadir · Der: quitar último · Shift+Der: vaciar",
                d -> d.steps.size() + " pasos", (ctx, click) -> {
                    switch (click) {
                        case RIGHT -> {
                            if (!ctx.def.steps.isEmpty()) ctx.def.steps.remove(ctx.def.steps.size() - 1);
                            ctx.commit();
                        }
                        case SHIFT_RIGHT -> {
                            ctx.def.steps.clear();
                            ctx.commit();
                        }
                        default -> ctx.prompt("Paso: TITLE;título;subtítulo;ticks | SOUND;clave | MESSAGE;texto | WAIT;ticks | FREEZE;ticks | PARTICLES;nombre;cantidad", in -> {
                            String type = in.split(";", 2)[0].trim().toUpperCase();
                            if (!List.of("TITLE", "SOUND", "MESSAGE", "WAIT", "FREEZE", "PARTICLES").contains(type)) {
                                pl.messages().send(ctx.player, "invalid-step");
                                return;
                            }
                            ctx.def.steps.add(in);
                            ctx.commit();
                        });
                    }
                });
        f.custom("Ver pasos", Material.BOOK, "Los muestra en el chat", "Click: listar", d -> "", (ctx, click) -> {
            ctx.player.closeInventory();
            for (int i = 0; i < ctx.def.steps.size(); i++) {
                ctx.player.sendMessage(Text.mm("<gray>" + (i + 1) + ". <white>" + Text.escape(ctx.def.steps.get(i))));
            }
        });
        return f.list();
    }

    private static List<Field<StatusDef>> statusFields() {
        Fields<StatusDef> f = new Fields<>();
        f.text("Nombre", Material.NAME_TAG, "Admite MiniMessage", d -> d.name, (d, v) -> d.name = v);
        f.hand("Icono", Material.ITEM_FRAME, "Ítem que lo representa", d -> d.icon, (d, m) -> d.icon = m);
        f.choice("Elemento", Material.AMETHYST_SHARD, "Elemento del daño continuo", () -> List.of(Element.values()), d -> d.element, (d, v) -> d.element = v);
        f.number("Daño por segundo", Material.IRON_SWORD, "Por cada stack", d -> d.dotDamage, (d, v) -> d.dotDamage = v, 0, 1000, 0.5);
        f.integer("Stacks máximos", Material.GLOWSTONE_DUST, "Acumulación máxima", d -> d.maxStacks, (d, v) -> d.maxStacks = v, 1, 20, 1);
        f.integer("Lentitud (nivel, -1 = no)", Material.COBWEB, "Efecto de Lentitud", d -> d.slowAmp, (d, v) -> d.slowAmp = v, -1, 10, 1);
        f.integer("Debilidad (nivel, -1 = no)", Material.FERMENTED_SPIDER_EYE, "Efecto de Debilidad", d -> d.weakAmp, (d, v) -> d.weakAmp = v, -1, 10, 1);
        f.bool("Aturdir", Material.GOLD_NUGGET, "Casi inmoviliza al objetivo", d -> d.stun, (d, v) -> d.stun = v);
        f.choice("Partícula", Material.FIREWORK_STAR, "Visual del estado", () -> Fx.PARTICLES, d -> d.particle, (d, v) -> d.particle = v);
        return f.list();
    }

    /** Modelo + animaciones por estado + autoasignación (compartido por invocaciones y bosses). */
    private static <T extends Definition & Modeled> void modelFields(Fields<T> f, SummonForge pl) {
        f.custom("Modelo", Material.ARMOR_STAND, "Modelo de BetterModel", "Click: elegir de la lista",
                d -> d.modelId().isEmpty() ? "(ninguno)" : d.modelId(), (ctx, click) ->
                        ctx.open(new Pickers.ModelPicker(pl, ctx.player, key -> {
                            ctx.def.modelId(key);
                            ctx.def.anims().autoFill(pl.model().animations(key));
                            ctx.commit();
                            ctx.reopen();
                        }, ctx::reopen)));
        f.custom("Autoasignar animaciones", Material.ENDER_EYE, "Busca idle/walk/attack... por nombre", "Click: reasignar",
                d -> "", (ctx, click) -> {
                    ctx.def.anims().autoFill(pl.model().animations(ctx.def.modelId()));
                    ctx.commit();
                });
        for (String st : AnimationSet.STATES) {
            f.custom("Animación: " + st, Material.NOTE_BLOCK, "Animación del estado " + st, "Click: elegir",
                    d -> d.anims().get(st).isEmpty() ? "(ninguna)" : d.anims().get(st), (ctx, click) -> {
                        if (ctx.def.modelId().isEmpty()) {
                            pl.messages().send(ctx.player, "need-model");
                            return;
                        }
                        ctx.open(new Pickers.AnimationPicker(pl, ctx.player, ctx.def.modelId(), name -> {
                            ctx.def.anims().set(st, name);
                            ctx.commit();
                            ctx.reopen();
                        }, ctx::reopen));
                    });
        }
    }

    private static List<Field<SummonDef>> summonFields(SummonForge pl) {
        Fields<SummonDef> f = new Fields<>();
        f.text("Nombre", Material.NAME_TAG, "Admite MiniMessage", d -> d.name, (d, v) -> d.name = v);
        f.hand("Icono", Material.ITEM_FRAME, "Ítem que la representa", d -> d.icon, (d, m) -> d.icon = m);
        modelFields(f, pl);
        f.choice("Mob base", Material.ZOMBIE_HEAD, "Cuerpo de la invocación", () -> Mobs.ALLOWED, d -> d.baseMob, (d, v) -> d.baseMob = v);
        f.choice("Tipo", Material.ENDER_EYE, "Compañero / Aura (estilo Susanoo) / Libre (donde miras)", () -> List.of(SummonType.values()), d -> d.type, (d, v) -> d.type = v);
        f.number("Reducción de daño al dueño (%)", Material.SHIELD, "Solo tipo Aura", d -> d.ownerDamageReduction, (d, v) -> d.ownerDamageReduction = v, 0, 90, 5);
        f.number("Vida", Material.GOLDEN_APPLE, "Vida base (nivel 1)", d -> d.health, (d, v) -> d.health = v, 10, 2048, 10);
        f.number("Daño", Material.IRON_SWORD, "Daño cuerpo a cuerpo base", d -> d.damage, (d, v) -> d.damage = v, 1, 500, 1);
        f.number("Velocidad", Material.SUGAR, "Movimiento (0.3 = normal)", d -> d.speed, (d, v) -> d.speed = v, 0.1, 0.8, 0.02);
        f.number("Escala", Material.SLIME_BALL, "Tamaño de la entidad", d -> d.scale, (d, v) -> d.scale = v, 0.3, 6, 0.1);
        f.integer("Duración (segundos, 0 = permanente)", Material.CLOCK, "Para avatares/auras temporales", d -> d.durationSeconds, (d, v) -> d.durationSeconds = v, 0, 600, 5);
        f.integer("Enfriamiento (segundos)", Material.SNOWBALL, "Tras terminar/caer; 0 = el de config", d -> d.cooldownSeconds, (d, v) -> d.cooldownSeconds = v, 0, 3600, 5);
        f.number("Mejora por nivel", Material.EXPERIENCE_BOTTLE, "0.15 = +15% por nivel", d -> d.perLevelPct, (d, v) -> d.perLevelPct = v, 0, 1, 0.05);
        f.integer("Coste de mejora (niveles XP)", Material.ENCHANTED_BOOK, "Se multiplica por el nivel actual", d -> d.upgradeXpLevels, (d, v) -> d.upgradeXpLevels = v, 0, 100, 1);
        f.integer("Intervalo de habilidades (ticks)", Material.CLOCK, "Cada cuánto usa una sola", d -> d.castIntervalTicks, (d, v) -> d.castIntervalTicks = v, 10, 1200, 10);
        f.choice("Partícula (sin modelo)", Material.FIREWORK_STAR, "Visual de respaldo", () -> Fx.PARTICLES, d -> d.particle, (d, v) -> d.particle = v);
        f.choice("Elemento (defensa)", Material.AMETHYST_SHARD, "Resistencias según la matriz", () -> List.of(Element.values()), d -> d.element, (d, v) -> d.element = v);
        f.number("Crítico (%)", Material.GOLDEN_SWORD, "Probabilidad de golpe crítico", d -> d.critChance, (d, v) -> d.critChance = v, 0, 100, 5);
        f.number("Multiplicador crítico", Material.NETHERITE_SWORD, "Daño extra en crítico", d -> d.critMult, (d, v) -> d.critMult = v, 1, 5, 0.1);
        f.number("Robo de vida (%)", Material.GLISTERING_MELON_SLICE, "% del daño que cura", d -> d.lifesteal, (d, v) -> d.lifesteal = v, 0, 100, 5);
        f.custom("Habilidades", Material.BLAZE_POWDER, "Las que puede usar (orden = slot)", "Click: elegir",
                d -> d.abilities.isEmpty() ? "(ninguna)" : String.join(", ", d.abilities), (ctx, click) ->
                        ctx.open(new Pickers.AbilityPicker(pl, ctx.player, ctx.def.abilities, () -> {
                            ctx.commit();
                            ctx.reopen();
                        })));
        return f.list();
    }

    private static List<Field<BossDef>> bossFields(SummonForge pl) {
        Fields<BossDef> f = new Fields<>();
        f.text("Nombre", Material.NAME_TAG, "Admite MiniMessage", d -> d.name, (d, v) -> d.name = v);
        f.hand("Icono", Material.ITEM_FRAME, "Ítem en el menú", d -> d.icon, (d, m) -> d.icon = m);
        modelFields(f, pl);
        f.choice("Mob base", Material.ZOMBIE_HEAD, "Cuerpo del boss", () -> Mobs.ALLOWED, d -> d.baseMob, (d, v) -> d.baseMob = v);
        f.number("Vida", Material.GOLDEN_APPLE, "Máx. 2048 (límite de spigot.yml)", d -> d.health, (d, v) -> d.health = v, 10, 2048, 50);
        f.number("Daño", Material.IRON_SWORD, "Daño cuerpo a cuerpo", d -> d.damage, (d, v) -> d.damage = v, 1, 1000, 1);
        f.number("Punto débil: daño x a la cabeza", Material.TARGET, "Solo proyectiles; 1 = sin punto débil", d -> d.headshotMult, (d, v) -> d.headshotMult = v, 1, 5, 0.25);
        f.number("Escudo (absorción)", Material.SHIELD, "Puntos extra de vida al aparecer", d -> d.shield, (d, v) -> d.shield = v, 0, 2000, 50);
        f.number("Armadura", Material.IRON_CHESTPLATE, "Puntos de armadura", d -> d.armor, (d, v) -> d.armor = v, 0, 30, 1);
        f.number("Velocidad", Material.SUGAR, "Movimiento", d -> d.speed, (d, v) -> d.speed = v, 0.1, 0.8, 0.02);
        f.number("Escala", Material.SLIME_BALL, "Tamaño", d -> d.scale, (d, v) -> d.scale = v, 0.3, 8, 0.1);
        f.number("Resist. empuje", Material.ANVIL, "0 a 1", d -> d.knockbackResist, (d, v) -> d.knockbackResist = v, 0, 1, 0.1);
        f.number("Rango de detección", Material.SPYGLASS, "Bloques", d -> d.detectRange, (d, v) -> d.detectRange = v, 8, 80, 2);
        f.number("Radio de ancla (leash)", Material.LEAD, "Si se aleja más, vuelve y se cura", d -> d.leashRadius, (d, v) -> d.leashRadius = v, 10, 120, 5);
        f.integer("Intervalo de habilidades (ticks)", Material.CLOCK, "Entre habilidades", d -> d.abilityIntervalTicks, (d, v) -> d.abilityIntervalTicks = v, 10, 1200, 10);
        f.custom("Habilidades", Material.BLAZE_POWDER, "Las que puede usar", "Click: elegir",
                d -> d.abilities.isEmpty() ? "(ninguna)" : String.join(", ", d.abilities), (ctx, click) ->
                        ctx.open(new Pickers.AbilityPicker(pl, ctx.player, ctx.def.abilities, () -> {
                            ctx.commit();
                            ctx.reopen();
                        })));
        f.choice("Color de barra", Material.RED_DYE, "BossBar", () -> List.of("RED", "PINK", "BLUE", "GREEN", "YELLOW", "PURPLE", "WHITE"),
                d -> d.barColor, (d, v) -> d.barColor = v);
        f.choice("Partícula (sin modelo)", Material.FIREWORK_STAR, "Visual de respaldo", () -> Fx.PARTICLES, d -> d.particle, (d, v) -> d.particle = v);
        f.choice("Objeto que suelta", Material.CHEST, "Objeto de invocación", () -> {
            List<String> l = new ArrayList<>();
            l.add("");
            l.addAll(pl.itemStore().ids());
            return l;
        }, d -> d.dropItemId, (d, v) -> d.dropItemId = v);
        f.choice("Elemento (defensa)", Material.AMETHYST_SHARD, "Resistencias según la matriz", () -> List.of(Element.values()), d -> d.element, (d, v) -> d.element = v);
        f.number("Crítico (%)", Material.GOLDEN_SWORD, "Probabilidad de golpe crítico", d -> d.critChance, (d, v) -> d.critChance = v, 0, 100, 5);
        f.number("Multiplicador crítico", Material.NETHERITE_SWORD, "Daño extra en crítico", d -> d.critMult, (d, v) -> d.critMult = v, 1, 5, 0.1);
        f.number("Robo de vida (%)", Material.GLISTERING_MELON_SLICE, "% del daño que cura", d -> d.lifesteal, (d, v) -> d.lifesteal = v, 0, 100, 5);
        f.integer("Enrage tras (segundos, 0 = no)", Material.CLOCK, "Si el combate se alarga, se enfurece", d -> d.enrageSeconds, (d, v) -> d.enrageSeconds = v, 0, 3600, 30);
        f.number("Enrage: daño x", Material.IRON_SWORD, "Multiplicador al enfurecer", d -> d.enrageDmgMult, (d, v) -> d.enrageDmgMult = v, 1, 5, 0.1);
        f.number("Enrage: velocidad x", Material.SUGAR, "Multiplicador al enfurecer", d -> d.enrageSpeedMult, (d, v) -> d.enrageSpeedMult = v, 1, 3, 0.05);
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            String n = "Fase " + (idx + 2);
            f.integer(n + ": umbral de vida (%)", Material.REDSTONE_TORCH, "0 = fase desactivada", d -> d.phases[idx].pct, (d, v) -> d.phases[idx].pct = v, 0, 95, 5);
            f.number(n + ": daño x", Material.IRON_SWORD, "Multiplica el daño al entrar", d -> d.phases[idx].dmgMult, (d, v) -> d.phases[idx].dmgMult = v, 0.5, 5, 0.1);
            f.number(n + ": velocidad x", Material.SUGAR, "Multiplica la velocidad", d -> d.phases[idx].speedMult, (d, v) -> d.phases[idx].speedMult = v, 0.5, 3, 0.05);
            f.custom(n + ": habilidades extra", Material.BLAZE_POWDER, "Se añaden al entrar", "Click: elegir",
                    d -> d.phases[idx].abilities.isEmpty() ? "(ninguna)" : String.join(", ", d.phases[idx].abilities), (ctx, click) ->
                            ctx.open(new Pickers.AbilityPicker(pl, ctx.player, ctx.def.phases[idx].abilities, () -> {
                                ctx.commit();
                                ctx.reopen();
                            })));
            f.bool(n + ": rellenar escudo", Material.SHIELD, "Recupera el escudo al entrar", d -> d.phases[idx].refillShield, (d, v) -> d.phases[idx].refillShield = v);
            f.integer(n + ": esbirros", Material.ZOMBIE_HEAD, "Cuántos aparecen al entrar (máx. 20 por boss)", d -> d.phases[idx].addsCount, (d, v) -> d.phases[idx].addsCount = v, 0, 10, 1);
            f.choice(n + ": mob de esbirros", Material.SKELETON_SKULL, "Tipo de esbirro", () -> Mobs.HOSTILE, d -> d.phases[idx].addsMob, (d, v) -> d.phases[idx].addsMob = v);
            f.text(n + ": mensaje", Material.OAK_SIGN, "Aviso a los jugadores cercanos", d -> d.phases[idx].message, (d, v) -> d.phases[idx].message = v);
        }
        f.choice("Cinemática de inicio", Material.FILLED_MAP, "Se reproduce al aparecer", () -> cineIds(pl), d -> d.introCinematic, (d, v) -> d.introCinematic = v);
        f.choice("Cinemática de muerte", Material.FILLED_MAP, "Se reproduce al morir", () -> cineIds(pl), d -> d.outroCinematic, (d, v) -> d.outroCinematic = v);
        f.number("Prob. de drop (%)", Material.DIAMOND, "0 a 100", d -> d.dropChance, (d, v) -> d.dropChance = v, 0, 100, 5);
        f.bool("Spawn automático", Material.SPAWNER, "Reaparece solo en su punto", d -> d.spawnEnabled, (d, v) -> d.spawnEnabled = v);
        f.integer("Respawn (minutos)", Material.CLOCK, "Tras morir", d -> d.respawnMinutes, (d, v) -> d.respawnMinutes = v, 1, 10080, 5);
        f.custom("Punto de spawn", Material.COMPASS, "Dónde aparece", "Izq: fijar aquí · Der: teletransportarme",
                d -> d.spawnWorld.isBlank() ? "(sin definir)" : d.spawnWorld + " " + Math.round(d.spawnX) + " " + Math.round(d.spawnY) + " " + Math.round(d.spawnZ),
                (ctx, click) -> {
                    if (click == org.bukkit.event.inventory.ClickType.RIGHT) {
                        org.bukkit.World w = org.bukkit.Bukkit.getWorld(ctx.def.spawnWorld);
                        if (w != null) ctx.player.teleport(new Location(w, ctx.def.spawnX, ctx.def.spawnY, ctx.def.spawnZ));
                    } else {
                        ctx.def.setSpawn(ctx.player.getLocation());
                        ctx.commit();
                    }
                });
        return f.list();
    }

    private static List<Field<ItemDef>> itemFields(SummonForge pl) {
        Fields<ItemDef> f = new Fields<>();
        f.text("Nombre", Material.NAME_TAG, "Admite MiniMessage", d -> d.name, (d, v) -> d.name = v);
        f.hand("Material", Material.ITEM_FRAME, "Ítem base", d -> d.material, (d, m) -> d.material = m);
        f.text("Lore", Material.BOOK, "Líneas separadas por |", d -> d.lore, (d, v) -> d.lore = v);
        f.integer("CustomModelData", Material.PAINTING, "0 = ninguno", d -> d.customModelData, (d, v) -> d.customModelData = v, 0, 9999999, 1);
        f.choice("Invocación que otorga", Material.NETHER_STAR, "Al clic derecho", () -> {
            List<String> l = new ArrayList<>();
            l.add("");
            l.addAll(pl.summonStore().ids());
            return l;
        }, d -> d.summonId, (d, v) -> d.summonId = v);
        return f.list();
    }
}
