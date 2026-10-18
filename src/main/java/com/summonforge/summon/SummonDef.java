package com.summonforge.summon;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.model.AnimationSet;
import com.summonforge.model.Modeled;
import com.summonforge.util.Enums;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/** Invocación tipo Compañero (sesión 1). Aura/aparición libre/montura llegarán en sesiones siguientes. */
public final class SummonDef implements Definition, Modeled {
    public final String id;
    public String name;
    public Material icon = Material.NETHER_STAR;
    public String modelId = "";
    public final AnimationSet anims = new AnimationSet();
    public EntityType baseMob = EntityType.IRON_GOLEM;
    public double health = 60;
    public double damage = 8;
    public double speed = 0.3;
    public double scale = 1.0;
    /** Mejora por nivel (0.15 = +15 % de vida, daño y habilidades por nivel). */
    public double perLevelPct = 0.15;
    public int upgradeXpLevels = 5;
    public int castIntervalTicks = 60;
    public String particle = "SOUL_FIRE_FLAME";
    public com.summonforge.stats.Element element = com.summonforge.stats.Element.NONE;
    /** Duración en segundos (0 = permanente). Útil para auras/avatares temporales tipo Susanoo. */
    public SummonType type = SummonType.COMPANION;
    /** Solo AURA: % de daño que se resta al dueño mientras está activa. */
    public double ownerDamageReduction = 0;
    public int durationSeconds = 0;
    /** Enfriamiento tras terminar o morir (segundos); 0 = usa summon.revive-cooldown-seconds. */
    public int cooldownSeconds = 0;
    public double critChance = 5;
    public double critMult = 1.5;
    public double lifesteal = 0;
    public final List<String> abilities = new ArrayList<>();

    public SummonDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return icon; }
    @Override public String modelId() { return modelId; }
    @Override public void modelId(String id) { this.modelId = id == null ? "" : id; }
    @Override public AnimationSet anims() { return anims; }

    public static final Codec<SummonDef> CODEC = new Codec<>() {
        @Override
        public SummonDef read(String id, ConfigurationSection s) {
            SummonDef d = new SummonDef(id);
            d.name = s.getString("name", id);
            d.icon = Enums.material(s.getString("icon"), Material.NETHER_STAR);
            d.modelId = s.getString("model", "");
            d.anims.read(s.getConfigurationSection("anims"));
            d.baseMob = Enums.parse(EntityType.class, s.getString("base-mob"), EntityType.IRON_GOLEM);
            d.health = s.getDouble("health", 60);
            d.damage = s.getDouble("damage", 8);
            d.speed = s.getDouble("speed", 0.3);
            d.scale = s.getDouble("scale", 1.0);
            d.perLevelPct = s.getDouble("per-level-pct", 0.15);
            d.upgradeXpLevels = s.getInt("upgrade-xp-levels", 5);
            d.castIntervalTicks = s.getInt("cast-interval-ticks", 60);
            d.particle = s.getString("particle", "SOUL_FIRE_FLAME");
            d.element = Enums.parse(com.summonforge.stats.Element.class, s.getString("element"), com.summonforge.stats.Element.NONE);
            d.type = Enums.parse(SummonType.class, s.getString("type"), SummonType.COMPANION);
            d.ownerDamageReduction = s.getDouble("owner-damage-reduction", 0);
            d.durationSeconds = s.getInt("duration-seconds", 0);
            d.cooldownSeconds = s.getInt("cooldown-seconds", 0);
            d.critChance = s.getDouble("crit-chance", 5);
            d.critMult = s.getDouble("crit-mult", 1.5);
            d.lifesteal = s.getDouble("lifesteal", 0);
            d.abilities.addAll(s.getStringList("abilities"));
            return d;
        }

        @Override
        public void write(SummonDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("icon", d.icon.name());
            s.set("model", d.modelId);
            d.anims.write(s.createSection("anims"));
            s.set("base-mob", d.baseMob.name());
            s.set("health", d.health);
            s.set("damage", d.damage);
            s.set("speed", d.speed);
            s.set("scale", d.scale);
            s.set("per-level-pct", d.perLevelPct);
            s.set("upgrade-xp-levels", d.upgradeXpLevels);
            s.set("cast-interval-ticks", d.castIntervalTicks);
            s.set("particle", d.particle);
            s.set("element", d.element.name());
            s.set("type", d.type.name());
            s.set("owner-damage-reduction", d.ownerDamageReduction);
            s.set("duration-seconds", d.durationSeconds);
            s.set("cooldown-seconds", d.cooldownSeconds);
            s.set("crit-chance", d.critChance);
            s.set("crit-mult", d.critMult);
            s.set("lifesteal", d.lifesteal);
            s.set("abilities", d.abilities);
        }
    };
}
