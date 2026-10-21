package com.summonforge.ability;

import com.summonforge.core.Keys;
import com.summonforge.core.SummonForge;
import com.summonforge.damage.DamageContext;
import com.summonforge.dimension.Realm;
import com.summonforge.util.Attrs;
import com.summonforge.util.Fx;
import com.summonforge.util.Potions;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Habilidades con lógica propia (teletransporte, clones, cadenas, minas...). Se asignan a una AbilityDef
 * con el campo "special". Parámetros reutilizados de la definición:
 * damage = valor base, radius, range, statusTicks = duración (ticks), statusStacks = cantidad, speed, knockback.
 */
public final class SpecialAbilities {
    @FunctionalInterface
    private interface Handler {
        void run(X x);
    }

    /** Contexto de una ejecución con utilidades comunes. */
    private final class X {
        final CastContext c;
        final AbilityDef d;

        X(CastContext c, AbilityDef d) {
            this.c = c;
            this.d = d;
        }

        LivingEntity me() { return c.caster(); }
        World w() { return c.caster().getWorld(); }
        Location at() { return c.caster().getLocation(); }
        int dur() { return Math.max(20, d.statusTicks); }
        int count(int def) { return d.statusStacks > 1 ? d.statusStacks : def; }

        Vector flat() {
            Vector v = c.direction().clone().setY(0);
            if (v.lengthSquared() < 1.0E-4) v = new Vector(1, 0, 0);
            return v.normalize();
        }

        List<LivingEntity> enemies(Location center, double r) {
            List<LivingEntity> out = new ArrayList<>();
            for (LivingEntity e : center.getWorld().getNearbyLivingEntities(center, r, r, r)) {
                if (e instanceof ArmorStand || e == c.caster() || plugin.abilities().ally(c, e)) continue;
                if (!plugin.damage().canHit(new DamageContext(c.caster(), c.owner(), e, 1, d.element, d.id))) continue;
                out.add(e);
            }
            return out;
        }

        List<LivingEntity> allies(Location center, double r) {
            List<LivingEntity> out = new ArrayList<>();
            for (LivingEntity e : center.getWorld().getNearbyLivingEntities(center, r, r, r)) {
                if (!(e instanceof ArmorStand) && !e.isDead() && plugin.abilities().ally(c, e)) out.add(e);
            }
            return out;
        }

        LivingEntity nearestEnemy(Location from, double r, Set<UUID> exclude) {
            LivingEntity best = null;
            double bd = Double.MAX_VALUE;
            for (LivingEntity e : enemies(from, r)) {
                if (exclude != null && exclude.contains(e.getUniqueId())) continue;
                double dd = e.getLocation().distanceSquared(from);
                if (dd < bd) {
                    bd = dd;
                    best = e;
                }
            }
            return best;
        }

        LivingEntity nearestEnemy(Location from, double r) {
            return nearestEnemy(from, r, null);
        }

        /** Daño = mult x damage de la definición (escala con el nivel). */
        double hit(LivingEntity t, double mult) {
            return plugin.damage().apply(new DamageContext(c.caster(), c.owner(), t, d.damage * mult * c.scale(), d.element, d.id));
        }

        void push(LivingEntity t, Location from, double power, double up) {
            Vector v = t.getLocation().toVector().subtract(from.toVector()).setY(0);
            if (v.lengthSquared() < 1.0E-4) v = new Vector(0, 0, 0);
            else v.normalize().multiply(power);
            v.setY(up);
            t.setVelocity(t.getVelocity().add(v));
        }

        void fx(Location l, String particle, int n, double spread) {
            Fx.spawn(l.getWorld(), particle, l, n, spread, spread, spread, 0.02);
        }

        void ring(Location l, double r) {
            Fx.ring(l.getWorld(), l, r, d.particle);
        }

        void later(long ticks, Runnable r) {
            Bukkit.getScheduler().runTaskLater(plugin, r, Math.max(1, ticks));
        }

        void repeat(int ticks, int period, Runnable body) {
            new BukkitRunnable() {
                int t = 0;

                @Override
                public void run() {
                    if (t >= ticks || !c.caster().isValid()) {
                        cancel();
                        return;
                    }
                    body.run();
                    t += period;
                }
            }.runTaskTimer(plugin, 0L, period);
        }
    }

    private final SummonForge plugin;
    private final Map<String, Handler> handlers = new LinkedHashMap<>();
    private final Map<UUID, Location> tpMarks = new HashMap<>();

    public SpecialAbilities(SummonForge plugin) {
        this.plugin = plugin;
        movement();
        summoning();
        defense();
        area();
        duration();
        targeted();
        realms();
        extras();
        summonPack();
    }

    public List<String> ids() {
        return new ArrayList<>(handlers.keySet());
    }

    /** @return true si existía y se ejecutó. */
    public boolean run(String id, CastContext c, AbilityDef d) {
        Handler h = handlers.get(id);
        if (h == null) return false;
        h.run(new X(c, d));
        return true;
    }

    // ================= movimiento =================
    private void movement() {
        handlers.put("blink", x -> {
            Location from = x.at();
            Vector dir = x.flat();
            Location last = from.clone();
            for (double t = 0.5; t <= x.d.range; t += 0.5) {
                Location p = from.clone().add(dir.clone().multiply(t));
                if (!p.getBlock().isPassable() || !p.clone().add(0, 1, 0).getBlock().isPassable()) break;
                last = p;
            }
            x.fx(from.clone().add(0, 1, 0), x.d.particle, 20, 0.4);
            x.me().teleport(last);
            x.fx(last.clone().add(0, 1, 0), x.d.particle, 20, 0.4);
        });
        handlers.put("teleport_tras", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            Location tl = t.getLocation();
            Vector back = tl.getDirection().setY(0);
            if (back.lengthSquared() < 1.0E-4) back = new Vector(1, 0, 0);
            back.normalize().multiply(-1.5);
            Location dest = tl.clone().add(back);
            dest.setDirection(tl.toVector().subtract(dest.toVector()));
            x.fx(x.at().add(0, 1, 0), x.d.particle, 20, 0.4);
            x.me().teleport(dest);
            x.fx(dest.clone().add(0, 1, 0), x.d.particle, 20, 0.4);
            x.hit(t, 1);
        });
        handlers.put("intercambio", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            Location a = x.at();
            Location b = t.getLocation();
            x.me().teleport(b);
            t.teleport(a);
            x.fx(a.clone().add(0, 1, 0), "PORTAL", 20, 0.4);
            x.fx(b.clone().add(0, 1, 0), "PORTAL", 20, 0.4);
            x.hit(t, 1);
        });
        handlers.put("embestida", x -> {
            LivingEntity m = x.me();
            m.setVelocity(x.flat().multiply(Math.max(0.8, x.d.speed * 1.5)).setY(0.15));
            Set<UUID> hit = new HashSet<>();
            x.repeat(10, 2, () -> {
                for (LivingEntity t : x.enemies(m.getLocation(), 2.5)) {
                    if (hit.add(t.getUniqueId())) {
                        x.hit(t, 1);
                        x.push(t, m.getLocation(), x.d.knockback, 0.3);
                    }
                }
                x.fx(m.getLocation().add(0, 1, 0), x.d.particle, 4, 0.3);
            });
        });
        handlers.put("salto_sismico", x -> {
            LivingEntity m = x.me();
            m.setVelocity(x.flat().multiply(0.6).setY(0.9));
            x.later(14, () -> {
                Location l = m.getLocation();
                x.ring(l, x.d.radius);
                x.fx(l, "EXPLOSION", 1, 0);
                Fx.sound(l, "entity.generic.explode", 1f, 1f);
                for (LivingEntity t : x.enemies(l, x.d.radius)) {
                    x.hit(t, 1);
                    x.push(t, l, x.d.knockback, 0.4);
                }
            });
        });
        handlers.put("tornado", x -> {
            Vector step = x.flat().multiply(0.5);
            Location pos = x.at().add(x.flat().multiply(2));
            x.repeat(x.dur(), 4, () -> {
                pos.add(step.clone().multiply(2));
                for (double h = 0; h < 4; h += 0.5) x.fx(pos.clone().add(0, h, 0), "CLOUD", 2, 0.4);
                for (LivingEntity t : x.enemies(pos, Math.max(2.5, x.d.radius))) {
                    t.setVelocity(new Vector(step.getX() * 0.3, 0.6, step.getZ() * 0.3));
                    x.hit(t, 0.25);
                }
            });
        });
        handlers.put("latigazo", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            Vector v = x.at().toVector().subtract(t.getLocation().toVector());
            if (v.lengthSquared() > 1.0E-4) t.setVelocity(v.normalize().multiply(1.1).setY(0.3));
            Fx.line(x.w(), x.me().getEyeLocation(), t.getLocation().add(0, 1, 0).toVector().subtract(x.me().getEyeLocation().toVector()),
                    Math.min(x.d.range, x.me().getEyeLocation().distance(t.getLocation())), x.d.particle);
            x.hit(t, 1);
        });
    }

    // ================= invocación de aliados y clones =================
    private void spawnTemp(X x, EntityType type, int count, double hpMul, double dmgMul, int ticks, String name) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Location l = x.at().add(r.nextDouble(-2, 2), 0, r.nextDouble(-2, 2));
            Entity e = x.w().spawnEntity(l, type);
            if (!(e instanceof LivingEntity le)) {
                e.remove();
                continue;
            }
            le.setPersistent(false);
            le.setRemoveWhenFarAway(false);
            double hp = Math.max(10, Attrs.maxHealth(x.me()) * hpMul);
            Attrs.set(le, "max_health", hp);
            le.setHealth(Math.min(Attrs.maxHealth(le), hp));
            Attrs.set(le, "attack_damage", Math.max(1, Attrs.get(x.me(), "attack_damage", 5) * dmgMul));
            le.customName(Text.mm(name));
            le.setCustomNameVisible(true);
            if (x.c.owner() != null) {
                le.getPersistentDataContainer().set(Keys.SUMMON_OWNER, PersistentDataType.STRING, x.c.owner().getUniqueId().toString());
                if (le instanceof IronGolem g) g.setPlayerCreated(true);
            }
            LivingEntity tgt = x.nearestEnemy(x.at(), 20);
            if (le instanceof Mob m && tgt != null) m.setTarget(tgt);
            x.fx(l.clone().add(0, 1, 0), x.d.particle, 12, 0.4);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (le.isValid()) le.remove();
            }, ticks);
        }
    }

    private void summoning() {
        handlers.put("clones", x -> spawnTemp(x, x.me().getType(), x.count(2), 0.4, 0.6, x.dur(), "<gray>Clon"));
        handlers.put("aliados", x -> spawnTemp(x, x.c.owner() != null ? EntityType.IRON_GOLEM : EntityType.HUSK,
                x.count(2), 0.5, 0.8, x.dur(), "<gray>Aliado"));
    }

    // ================= defensa y soporte =================
    private void shield(X x, LivingEntity t, double amount) {
        t.setAbsorptionAmount(Math.max(t.getAbsorptionAmount(), amount));
        x.fx(t.getLocation().add(0, 1, 0), "END_ROD", 12, 0.5);
        x.later(x.dur(), () -> {
            if (t.isValid()) t.setAbsorptionAmount(0);
        });
    }

    private void defense() {
        handlers.put("escudo", x -> {
            shield(x, x.me(), x.d.damage);
            if (x.c.owner() != null && x.c.owner() != x.me()) shield(x, x.c.owner(), x.d.damage);
        });
        handlers.put("escudo_grupal", x -> {
            for (LivingEntity a : x.allies(x.at(), x.d.radius)) shield(x, a, x.d.damage);
        });
        handlers.put("invisibilidad", x -> {
            Potions.add(x.me(), "invisibility", x.dur(), 0);
            x.fx(x.at().add(0, 1, 0), "SMOKE", 25, 0.5);
        });
        handlers.put("furia", x -> {
            for (LivingEntity a : new LivingEntity[]{x.me(), x.c.owner()}) {
                if (a == null) continue;
                Potions.add(a, "strength", x.dur(), 1);
                Potions.add(a, "speed", x.dur(), 1);
                x.fx(a.getLocation().add(0, 1, 0), "FLAME", 20, 0.5);
            }
        });
        handlers.put("grito_de_guerra", x -> {
            for (LivingEntity a : x.allies(x.at(), x.d.radius)) {
                Potions.add(a, "strength", x.dur(), 1);
                x.fx(a.getLocation().add(0, 1, 0), "CRIT", 10, 0.4);
            }
            x.ring(x.at(), x.d.radius);
        });
        handlers.put("curacion_porcentual", x -> {
            for (LivingEntity a : x.allies(x.at(), x.d.radius)) {
                double max = Attrs.maxHealth(a);
                a.setHealth(Math.min(max, a.getHealth() + max * x.d.damage / 100.0));
                x.fx(a.getLocation().add(0, 1.5, 0), "HEART", 4, 0.3);
            }
        });
    }

    // ================= áreas con varias fases =================
    private void area() {
        handlers.put("tormenta_de_rayos", x -> {
            List<LivingEntity> es = x.enemies(x.at(), x.d.radius);
            if (es.isEmpty()) return;
            int n = x.count(4);
            for (int i = 0; i < n; i++) {
                LivingEntity t = es.get(ThreadLocalRandom.current().nextInt(es.size()));
                x.later(i * 5L + 1, () -> {
                    if (!t.isValid()) return;
                    x.w().strikeLightningEffect(t.getLocation());
                    x.fx(t.getLocation().add(0, 1, 0), "ELECTRIC_SPARK", 15, 0.4);
                    x.hit(t, 1);
                });
            }
        });
        handlers.put("lluvia_de_meteoros", x -> {
            Location center = x.at().add(x.flat().multiply(x.d.range / 2));
            int n = x.count(6);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < n; i++) {
                Location p = center.clone().add(r.nextDouble(-x.d.radius, x.d.radius), 0, r.nextDouble(-x.d.radius, x.d.radius));
                x.later(i * 6L + 1, () -> {
                    Fx.ring(p.getWorld(), p, 3, "FLAME");
                    x.later(20, () -> {
                        x.fx(p, "EXPLOSION", 1, 0);
                        Fx.sound(p, "entity.generic.explode", 1f, 1f);
                        for (LivingEntity t : x.enemies(p, 3.5)) {
                            x.hit(t, 1);
                            x.push(t, p, 0.8, 0.4);
                        }
                    });
                });
            }
        });
        handlers.put("terremoto", x -> {
            Set<UUID> hit = new HashSet<>();
            for (int i = 1; i <= 3; i++) {
                double r = x.d.radius * i / 3.0;
                x.later(i * 12L, () -> {
                    Location l = x.at();
                    x.ring(l, r);
                    Fx.sound(l, "entity.generic.explode", 1f, 0.7f);
                    for (LivingEntity t : x.enemies(l, r)) {
                        if (hit.add(t.getUniqueId())) {
                            x.hit(t, 1);
                            x.push(t, l, x.d.knockback, 0.5);
                        }
                    }
                });
            }
        });
        handlers.put("lluvia_de_flechas", x -> {
            Location center = x.at().add(x.flat().multiply(x.d.range / 2));
            for (int i = 0; i < 3; i++) {
                x.later(i * 10L + 1, () -> {
                    Fx.sound(center, "entity.arrow.shoot", 1f, 1f);
                    x.fx(center.clone().add(0, 2, 0), "CRIT", 40, x.d.radius / 2);
                    for (LivingEntity t : x.enemies(center, x.d.radius)) x.hit(t, 1);
                });
            }
        });
        handlers.put("explosion_nuclear", x -> {
            Location l = x.at();
            x.ring(l, x.d.radius);
            x.fx(l.clone().add(0, 1, 0), "EXPLOSION", 6, x.d.radius / 3);
            Fx.sound(l, "entity.generic.explode", 2f, 0.5f);
            for (LivingEntity t : x.enemies(l, x.d.radius)) {
                double falloff = 1 - Math.min(1, t.getLocation().distance(l) / x.d.radius) * 0.5;
                x.hit(t, falloff);
                x.push(t, l, x.d.knockback, 0.7);
            }
        });
        handlers.put("congelacion_total", x -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) {
                x.hit(t, 1);
                plugin.statuses().apply(t, "congelacion", x.dur(), 2, x.me());
                Potions.add(t, "slowness", x.dur(), 2);
            }
        });
        handlers.put("drenar_vida", x -> {
            double total = 0;
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) total += x.hit(t, 1);
            LivingEntity m = x.me();
            m.setHealth(Math.min(Attrs.maxHealth(m), m.getHealth() + total * 0.5));
            x.fx(m.getLocation().add(0, 1.5, 0), "HEART", 6, 0.4);
        });
        handlers.put("gravedad_inversa", x -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) {
                Potions.add(t, "levitation", 40, 1);
                x.hit(t, 1);
            }
        });
        handlers.put("terror", x -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) {
                x.push(t, x.at(), x.d.knockback, 0.3);
                Potions.add(t, "slowness", x.dur(), 1);
                Potions.add(t, "weakness", x.dur(), 0);
            }
        });
        handlers.put("sellado", x -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) {
                plugin.statuses().apply(t, "debilidad", x.dur(), 1, x.me());
                Potions.add(t, "slowness", x.dur(), 2);
            }
        });
        handlers.put("tiempo_lento", x -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) Potions.add(t, "slowness", x.dur(), 3);
            for (LivingEntity a : x.allies(x.at(), x.d.radius)) Potions.add(a, "speed", x.dur(), 1);
        });
    }

    // ================= efectos sostenidos =================
    private void duration() {
        handlers.put("espiral_de_fuego", x -> {
            int[] step = {0};
            x.repeat(x.dur(), 2, () -> {
                step[0]++;
                for (int k = 0; k < 3; k++) {
                    double ang = step[0] * 0.5 + k * 2.094;
                    Location p = x.at().add(Math.cos(ang) * x.d.radius * 0.8, 0.3 + (step[0] % 20) * 0.1, Math.sin(ang) * x.d.radius * 0.8);
                    x.fx(p, x.d.particle, 1, 0);
                }
                if (step[0] % 5 == 0) for (LivingEntity t : x.enemies(x.at(), x.d.radius)) x.hit(t, 1);
            });
        });
        handlers.put("torbellino_continuo", x -> x.repeat(x.dur(), 5, () -> {
            Location l = x.at();
            x.ring(l, x.d.radius);
            for (LivingEntity t : x.enemies(l, x.d.radius)) {
                Vector v = l.toVector().subtract(t.getLocation().toVector()).setY(0.1);
                if (v.lengthSquared() > 1.0E-4) t.setVelocity(v.normalize().multiply(0.5));
                x.hit(t, 0.2);
            }
        }));
        handlers.put("campo_de_curacion", x -> x.repeat(x.dur(), 20, () -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity a : x.allies(x.at(), x.d.radius)) {
                a.setHealth(Math.min(Attrs.maxHealth(a), a.getHealth() + x.d.damage * x.c.scale()));
                x.fx(a.getLocation().add(0, 1.5, 0), "HEART", 2, 0.3);
            }
        }));
        handlers.put("aura_de_dano", x -> x.repeat(x.dur(), 10, () -> {
            x.ring(x.at(), x.d.radius);
            for (LivingEntity t : x.enemies(x.at(), x.d.radius)) x.hit(t, 1);
        }));
        handlers.put("mina", x -> {
            Location p = x.at().add(x.flat().multiply(3));
            new BukkitRunnable() {
                int t = 0;

                @Override
                public void run() {
                    if (t >= x.dur() || !x.me().isValid()) {
                        cancel();
                        return;
                    }
                    x.fx(p.clone().add(0, 0.2, 0), "CRIT", 2, 0.2);
                    if (t >= 20 && !x.enemies(p, 2.5).isEmpty()) {
                        x.fx(p, "EXPLOSION", 1, 0);
                        Fx.sound(p, "entity.generic.explode", 1f, 1f);
                        for (LivingEntity e : x.enemies(p, Math.max(2.5, x.d.radius))) {
                            x.hit(e, 1);
                            x.push(e, p, x.d.knockback, 0.5);
                        }
                        cancel();
                        return;
                    }
                    t += 5;
                }
            }.runTaskTimer(plugin, 0L, 5L);
        });
    }

    // ================= objetivo concreto =================
    private static List<LivingEntity> pick(List<LivingEntity> l, int max) {
        return l.size() <= max ? l : new ArrayList<>(l.subList(0, max));
    }

    // ================= dimensiones e ilusiones =================
    private void realms() {
        handlers.put("dimension_duelo", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            Potions.add(x.me(), "strength", x.dur(), 0);
            plugin.limbo().enter(x.me(), true, List.of(t), Realm.ARENA, x.dur() / 20, x.d.damage * x.c.scale(), x.d.element, x.d.id);
        });
        handlers.put("kamui_objetivo", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            plugin.limbo().enter(x.me(), false, List.of(t), Realm.LIMBO, x.dur() / 20, x.d.damage * x.c.scale(), x.d.element, x.d.id);
        });
        handlers.put("kamui_ambos", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            plugin.limbo().enter(x.me(), true, List.of(t), Realm.LIMBO, x.dur() / 20, x.d.damage * x.c.scale(), x.d.element, x.d.id);
        });
        handlers.put("tsukuyomi", x -> {
            List<LivingEntity> es = pick(x.enemies(x.at(), x.d.radius), x.count(3));
            if (es.isEmpty()) return;
            plugin.limbo().enter(x.me(), false, es, Realm.ILLUSION, x.dur() / 20, x.d.damage * x.c.scale(), x.d.element, x.d.id);
        });
        handlers.put("dominio_real", x -> {
            List<LivingEntity> es = pick(x.enemies(x.at(), x.d.radius), x.count(6));
            if (es.isEmpty()) return;
            plugin.limbo().enter(x.me(), true, es, Realm.ARENA, x.dur() / 20, x.d.damage * x.c.scale(), x.d.element, x.d.id);
        });
    }

    // ================= extras de combate =================
    private void extras() {
        handlers.put("reflejo", x -> {
            plugin.statuses().apply(x.me(), "reflejo", x.dur(), 1, x.me());
            x.fx(x.at().add(0, 1, 0), "END_ROD", 25, 0.5);
        });
        handlers.put("torreta", x -> {
            Location l = x.at().add(x.flat().multiply(2));
            Entity e = x.w().spawnEntity(l, EntityType.HUSK);
            if (!(e instanceof Mob m)) {
                e.remove();
                return;
            }
            m.setAI(false);
            m.setPersistent(false);
            m.setSilent(true);
            m.customName(Text.mm("<gray>Torreta"));
            m.setCustomNameVisible(true);
            if (x.c.owner() != null) {
                m.getPersistentDataContainer().set(Keys.SUMMON_OWNER, PersistentDataType.STRING, x.c.owner().getUniqueId().toString());
            } else {
                m.getPersistentDataContainer().set(Keys.BOSS, PersistentDataType.STRING, "torreta");
            }
            new BukkitRunnable() {
                int t = 0;

                @Override
                public void run() {
                    if (t >= x.dur() || !m.isValid() || !x.me().isValid()) {
                        m.remove();
                        cancel();
                        return;
                    }
                    LivingEntity tg = x.nearestEnemy(m.getLocation(), Math.max(8, x.d.range));
                    if (tg != null) {
                        Location eye = m.getEyeLocation();
                        Vector to = tg.getLocation().add(0, 1, 0).toVector().subtract(eye.toVector());
                        if (to.lengthSquared() > 1.0E-4) Fx.line(x.w(), eye, to, to.length(), x.d.particle);
                        x.hit(tg, 1);
                    }
                    t += 20;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        });
        handlers.put("oleada_minions", x -> spawnTemp(x, x.c.owner() != null ? EntityType.IRON_GOLEM : EntityType.HUSK,
                x.count(4), 0.3, 0.5, x.dur(), "<gray>Minion"));
        handlers.put("marca_tp", x -> {
            tpMarks.put(x.me().getUniqueId(), x.at());
            x.fx(x.at().add(0, 1, 0), "PORTAL", 30, 0.5);
            Fx.sound(x.at(), "block.beacon.activate", 1f, 1.5f);
        });
        handlers.put("recall_tp", x -> {
            Location m = tpMarks.get(x.me().getUniqueId());
            if (m == null || m.getWorld() == null) return;
            x.fx(x.at().add(0, 1, 0), "PORTAL", 30, 0.5);
            x.me().teleport(m);
            x.fx(m.clone().add(0, 1, 0), "PORTAL", 30, 0.5);
        });
        handlers.put("rastreo", x -> {
            List<LivingEntity> es = x.enemies(x.at(), x.d.radius > 0 ? x.d.radius : 20);
            for (LivingEntity e : es) e.setGlowing(true);
            x.later(x.dur(), () -> {
                for (LivingEntity e : es) if (e.isValid()) e.setGlowing(false);
            });
        });
        handlers.put("campo_minado", x -> {
            Handler mine = handlers.get("mina");
            if (mine == null) return;
            for (int i = -2; i <= 2; i++) {
                Vector dir = x.flat();
                dir.rotateAroundY(Math.toRadians(i * 30));
                mine.run(new X(new CastContext(x.c.caster(), x.c.owner(), x.c.origin(), dir, x.c.scale()), x.d));
            }
        });
        handlers.put("berserk", x -> {
            handlers.get("furia").run(x);
            x.later(x.dur(), () -> {
                if (!x.me().isValid()) return;
                Potions.add(x.me(), "weakness", 100, 0);
                Potions.add(x.me(), "slowness", 100, 0);
            });
        });
        handlers.put("armadura_roca", x -> {
            shield(x, x.me(), x.d.damage);
            Potions.add(x.me(), "resistance", x.dur(), 1);
        });
        handlers.put("sacrificio_vida", x -> {
            LivingEntity m = x.me();
            double max = Attrs.maxHealth(m);
            m.setHealth(Math.max(1, m.getHealth() - max * 0.3));
            Potions.add(m, "strength", x.dur(), 2);
            Potions.add(m, "speed", x.dur(), 1);
            x.fx(m.getLocation().add(0, 1, 0), "CRIMSON_SPORE".equals(x.d.particle) ? "FLAME" : x.d.particle, 25, 0.5);
        });
        handlers.put("chidori", x -> {
            LivingEntity m = x.me();
            m.setVelocity(x.flat().multiply(Math.max(1.0, x.d.speed * 1.5)).setY(0.1));
            Set<UUID> hit = new HashSet<>();
            x.repeat(10, 2, () -> {
                x.fx(m.getLocation().add(0, 1, 0), "ELECTRIC_SPARK", 8, 0.4);
                for (LivingEntity t : x.enemies(m.getLocation(), 2.5)) {
                    if (hit.add(t.getUniqueId())) {
                        x.hit(t, 1);
                        plugin.statuses().apply(t, "aturdimiento", 40, 1, m);
                    }
                }
            });
        });
        handlers.put("amaterasu", x -> {
            LivingEntity[] cur = {x.nearestEnemy(x.at(), x.d.range)};
            if (cur[0] == null) return;
            x.repeat(x.dur(), 20, () -> {
                if (cur[0] == null || !cur[0].isValid() || cur[0].isDead()) cur[0] = x.nearestEnemy(x.at(), x.d.range);
                if (cur[0] == null) return;
                plugin.statuses().apply(cur[0], "quemadura_negra", 60, 2, x.me());
                x.fx(cur[0].getLocation().add(0, 1, 0), "SOUL_FIRE_FLAME", 15, 0.4);
            });
        });
    }

    // ================= habilidades pensadas para invocaciones =================
    private void summonPack() {
        handlers.put("vinculo_vital", x -> {
            org.bukkit.entity.Player o = x.c.owner();
            if (o == null) return;
            LivingEntity m = x.me();
            double cost = Math.min(m.getHealth() - 1, Attrs.maxHealth(m) * 0.15);
            if (cost <= 0) return;
            m.setHealth(m.getHealth() - cost);
            o.setHealth(Math.min(Attrs.maxHealth(o), o.getHealth() + cost * 2 * x.c.scale()));
            x.fx(o.getLocation().add(0, 1.5, 0), "HEART", 8, 0.4);
        });
        handlers.put("coordinado", x -> {
            Location base = x.c.owner() != null ? x.c.owner().getLocation() : x.at();
            LivingEntity t = x.nearestEnemy(base, x.d.range);
            if (t == null) return;
            Location tl = t.getLocation();
            Vector back = tl.getDirection().setY(0);
            if (back.lengthSquared() < 1.0E-4) back = new Vector(1, 0, 0);
            back.normalize().multiply(-1.5);
            Location dest = tl.clone().add(back);
            dest.setDirection(tl.toVector().subtract(dest.toVector()));
            x.me().teleport(dest);
            x.fx(dest.clone().add(0, 1, 0), x.d.particle, 20, 0.4);
            x.hit(t, 1);
            if (x.c.owner() != null) Potions.add(x.c.owner(), "strength", x.dur(), 0);
        });
        handlers.put("regreso_dueno", x -> {
            org.bukkit.entity.Player o = x.c.owner();
            if (o == null) return;
            x.fx(x.at().add(0, 1, 0), "CLOUD", 20, 0.4);
            x.me().teleport(o.getLocation());
            shield(x, o, x.d.damage);
        });
        handlers.put("proteccion_guardian", x -> {
            org.bukkit.entity.Player o = x.c.owner();
            if (o == null) return;
            for (LivingEntity t : x.enemies(o.getLocation(), x.d.radius)) x.push(t, o.getLocation(), x.d.knockback, 0.3);
            shield(x, o, x.d.damage);
            Potions.add(o, "resistance", x.dur(), 1);
            x.ring(o.getLocation(), x.d.radius);
        });
    }

    private void targeted() {
        handlers.put("cadena_de_rayos", x -> {
            LivingEntity cur = x.nearestEnemy(x.at(), x.d.range);
            Set<UUID> seen = new HashSet<>();
            Location from = x.me().getEyeLocation();
            double mult = 1;
            int n = x.count(4);
            for (int i = 0; i < n && cur != null; i++) {
                Vector to = cur.getLocation().add(0, 1, 0).toVector().subtract(from.toVector());
                if (to.lengthSquared() > 1.0E-4) Fx.line(x.w(), from, to, to.length(), "ELECTRIC_SPARK");
                x.hit(cur, mult);
                seen.add(cur.getUniqueId());
                from = cur.getLocation().add(0, 1, 0);
                mult *= 0.85;
                cur = x.nearestEnemy(cur.getLocation(), 7, seen);
            }
            Fx.sound(x.at(), "entity.lightning_bolt.impact", 1f, 1.4f);
        });
        handlers.put("marca_de_muerte", x -> {
            LivingEntity t = x.nearestEnemy(x.at(), x.d.range);
            if (t == null) return;
            x.repeat(x.dur(), 10, () -> x.fx(t.getLocation().add(0, t.getHeight() + 0.5, 0), "SMOKE", 6, 0.3));
            x.later(x.dur(), () -> {
                if (!t.isValid()) return;
                x.fx(t.getLocation().add(0, 1, 0), "EXPLOSION", 1, 0);
                Fx.sound(t.getLocation(), "entity.wither.break_block", 1f, 1f);
                x.hit(t, 1);
            });
        });
    }
}
