package com.summonforge.damage;

import com.summonforge.core.SummonForge;
import com.summonforge.boss.BossDef;
import com.summonforge.stats.Element;
import com.summonforge.summon.SummonDef;
import com.summonforge.util.Attrs;
import com.summonforge.util.Tags;
import com.summonforge.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Todo el daño del plugin pasa por aquí (atribución correcta, reglas PvP, anti-recursión). */
public final class DamagePipeline {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private final SummonForge plugin;

    public DamagePipeline(SummonForge plugin) {
        this.plugin = plugin;
    }

    /** true mientras el plugin está aplicando daño propio (para que listeners futuros no re-disparen). */
    public static boolean active() {
        return ACTIVE.get();
    }

    public boolean canHit(DamageContext c) {
        LivingEntity t = c.target();
        if (t == null || t == c.attacker() || !t.isValid() || t.isDead() || t instanceof ArmorStand) return false;
        if (t instanceof Player p) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return false;
            if (c.owner() != null) {
                if (p.getUniqueId().equals(c.owner().getUniqueId())) return false;
                if (!plugin.getConfig().getBoolean("pvp.summons-damage-players", false)) return false;
            }
        }
        UUID targetOwner = Tags.summonOwner(t);
        if (targetOwner != null && c.owner() != null && targetOwner.equals(c.owner().getUniqueId())) return false;
        return !(Tags.isBoss(t) && Tags.isBoss(c.attacker()));
    }

    /** @return daño aplicado (0 si no se pudo golpear). */
    public double apply(DamageContext c) {
        if (!canHit(c)) return 0;
        double dmg = c.base();
        if (Double.isNaN(dmg) || Double.isInfinite(dmg) || dmg <= 0) return 0;
        dmg *= plugin.elements().get(c.element(), defenseOf(c.target()));
        if (dmg <= 0) return 0;
        double[] st = statsOf(c.attacker());
        boolean crit = st != null && st[0] > 0 && ThreadLocalRandom.current().nextDouble() * 100.0 < st[0];
        if (crit) dmg *= st[1];
        dmg = Math.min(dmg, 1_000_000.0);
        if (c.attacker() != null && c.attacker() != c.target() && plugin.statuses().has(c.target(), "reflejo")) {
            c.attacker().damage(dmg * 0.3);
        }

        DamageSource.Builder b = DamageSource.builder(DamageType.MOB_ATTACK).withDirectEntity(c.attacker());
        b.withCausingEntity(c.owner() != null ? c.owner() : c.attacker());
        ACTIVE.set(true);
        try {
            c.target().damage(dmg, b.build());
        } finally {
            ACTIVE.set(false);
        }
        if (st != null && st[2] > 0 && c.attacker().isValid()) {
            LivingEntity at = c.attacker();
            at.setHealth(Math.min(Attrs.maxHealth(at), at.getHealth() + dmg * st[2] / 100.0));
        }
        if (plugin.getConfig().getBoolean("damage-numbers", true)) number(c.target(), dmg, c, crit);
        return dmg;
    }

    /** {crítico %, multiplicador crítico, robo de vida %} del atacante, o null si no es un boss/invocación. */
    private double[] statsOf(LivingEntity e) {
        String b = Tags.bossId(e);
        if (b != null) {
            BossDef d = plugin.bossStore().get(b);
            return d == null ? null : new double[]{d.critChance, d.critMult, d.lifesteal};
        }
        String s = Tags.summonId(e);
        if (s != null) {
            SummonDef d = plugin.summonStore().get(s);
            return d == null ? null : new double[]{d.critChance, d.critMult, d.lifesteal};
        }
        return null;
    }

    private Element defenseOf(LivingEntity e) {
        String b = Tags.bossId(e);
        if (b != null) {
            BossDef d = plugin.bossStore().get(b);
            return d == null ? Element.NONE : d.element;
        }
        String s = Tags.summonId(e);
        if (s != null) {
            SummonDef d = plugin.summonStore().get(s);
            return d == null ? Element.NONE : d.element;
        }
        return Element.NONE;
    }

    private void number(LivingEntity target, double dmg, DamageContext c, boolean crit) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location l = target.getLocation().add(r.nextDouble(-0.3, 0.3), target.getHeight() + 0.3, r.nextDouble(-0.3, 0.3));
        String text = "<" + c.element().tag + "><b>" + Math.round(dmg) + (crit ? "!" : "");
        TextDisplay td = target.getWorld().spawn(l, TextDisplay.class, d -> {
            d.text(Text.mm(text));
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        });
        Bukkit.getScheduler().runTaskLater(plugin, td::remove, 20L);
    }
}
