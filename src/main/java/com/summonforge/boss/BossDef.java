package com.summonforge.boss;

import com.summonforge.data.Codec;
import com.summonforge.data.Definition;
import com.summonforge.model.AnimationSet;
import com.summonforge.model.Modeled;
import com.summonforge.summon.SquadEntry;
import com.summonforge.summon.Squadded;
import com.summonforge.util.Enums;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public final class BossDef implements Definition, Modeled, Squadded {
    public final String id;
    public String name;
    public Material icon = Material.WITHER_SKELETON_SKULL;
    public String modelId = "";
    /** Oculta el mob base con setInvisible. Déjalo apagado: BetterModel ya lo gestiona. */
    public boolean hideBase = false;
    public final AnimationSet anims = new AnimationSet();
    public EntityType baseMob = EntityType.HUSK;
    public double health = 300;
    public double damage = 10;
    public double armor = 4;
    public double speed = 0.26;
    public double scale = 1.5;
    public double knockbackResist = 0.6;
    public double detectRange = 24;
    public double leashRadius = 40;
    public int abilityIntervalTicks = 50;
    public String barColor = "RED";
    public String particle = "SOUL_FIRE_FLAME";
    public com.summonforge.stats.Element element = com.summonforge.stats.Element.NONE;
    public double critChance = 0;
    public double critMult = 1.5;
    public double lifesteal = 0;
    /** Segundos tras el spawn para el enrage (0 = desactivado). */
    /** Puntos de absorción (escudo) con los que aparece. */
    public double shield = 0;
    /** Escolta: mobs extra que aparecen con el boss (lo activa el admin por definición). */
    public boolean escortEnabled = false;
    public final SquadEntry[] escort = {new SquadEntry(), new SquadEntry(), new SquadEntry()};
    /** Familia de bosses: al derrotarlos todos, el jugador recibe familyRewardItem (una sola vez). */
    public String family = "";
    public String familyRewardItem = "";
    /** Kills consecutivas sin drop tras las que el drop está garantizado (0 = sin pity). */
    public int pityAfter = 0;
    /** Arena: mientras el boss vive no se pueden romper/colocar bloques en este radio de su ancla. */
    public boolean arenaProtect = false;
    public double arenaRadius = 30;
    /** Altar: bloque que, al hacer clic derecho (con altarItem si se define), invoca al boss. */
    public String altarWorld = "";
    public int altarX, altarY, altarZ;
    public String altarItem = "";
    /** Ids de cinemática al aparecer / al morir ("" = ninguna). */
    public String introCinematic = "";
    public String outroCinematic = "";
    /** Multiplicador de daño de proyectiles que impactan la cabeza (1 = sin punto débil). */
    public double headshotMult = 1;
    public int enrageSeconds = 0;
    public double enrageDmgMult = 1.5;
    public double enrageSpeedMult = 1.2;
    public final Phase[] phases = {new Phase(), new Phase()};
    public final List<String> abilities = new ArrayList<>();
    /** Tabla de drops (hasta 5 líneas, cada una con su % y cantidad). El pity garantiza la línea 1. */
    public final DropEntry[] drops = {new DropEntry(), new DropEntry(), new DropEntry(), new DropEntry(), new DropEntry()};
    /** Puntos de spawn extra "mundo;x;y;z": se elige uno al azar entre el principal y estos. */
    public final java.util.List<String> extraSpawns = new ArrayList<>();
    /** Radio aleatorio alrededor del punto elegido (0 = exacto). */
    public double spawnRadius = 0;
    /** Mínimo de jugadores conectados para que reaparezca solo. */
    public int spawnMinPlayers = 0;
    /** Si es mayor que respawnMinutes, el respawn dura un tiempo al azar entre ambos. */
    public int respawnMaxMinutes = 0;
    public boolean spawnEnabled = false;
    public String spawnWorld = "";
    public double spawnX, spawnY, spawnZ;
    public int respawnMinutes = 30;

    public BossDef(String id) {
        this.id = id;
        this.name = id;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public Material icon() { return icon; }
    @Override public String modelId() { return modelId; }
    @Override public void modelId(String id) { this.modelId = id == null ? "" : id; }
    @Override public AnimationSet anims() { return anims; }
    @Override public boolean squadEnabled() { return escortEnabled; }
    @Override public void squadEnabled(boolean enabled) { this.escortEnabled = enabled; }
    @Override public SquadEntry[] squad() { return escort; }

    public void setSpawn(Location l) {
        spawnWorld = l.getWorld().getName();
        spawnX = l.getX();
        spawnY = l.getY();
        spawnZ = l.getZ();
    }

    public static final Codec<BossDef> CODEC = new Codec<>() {
        @Override
        public BossDef read(String id, ConfigurationSection s) {
            BossDef d = new BossDef(id);
            d.name = s.getString("name", id);
            d.icon = Enums.material(s.getString("icon"), Material.WITHER_SKELETON_SKULL);
            d.modelId = s.getString("model", "");
            d.hideBase = s.getBoolean("hide-base", false);
            d.anims.read(s.getConfigurationSection("anims"));
            d.baseMob = Enums.parse(EntityType.class, s.getString("base-mob"), EntityType.HUSK);
            d.health = s.getDouble("health", 300);
            d.damage = s.getDouble("damage", 10);
            d.armor = s.getDouble("armor", 4);
            d.speed = s.getDouble("speed", 0.26);
            d.scale = s.getDouble("scale", 1.5);
            d.knockbackResist = s.getDouble("knockback-resist", 0.6);
            d.detectRange = s.getDouble("detect-range", 24);
            d.leashRadius = s.getDouble("leash-radius", 40);
            d.abilityIntervalTicks = s.getInt("ability-interval-ticks", 50);
            d.barColor = s.getString("bar-color", "RED");
            d.particle = s.getString("particle", "SOUL_FIRE_FLAME");
            d.element = Enums.parse(com.summonforge.stats.Element.class, s.getString("element"), com.summonforge.stats.Element.NONE);
            d.critChance = s.getDouble("crit-chance", 0);
            d.critMult = s.getDouble("crit-mult", 1.5);
            d.lifesteal = s.getDouble("lifesteal", 0);
            d.shield = s.getDouble("shield", 0);
            d.escortEnabled = s.getBoolean("escort.enabled", false);
            for (int i = 0; i < d.escort.length; i++) d.escort[i].read(s.getConfigurationSection("escort.slot" + i));
            d.family = s.getString("family", "");
            d.familyRewardItem = s.getString("family-reward-item", "");
            d.pityAfter = s.getInt("pity-after", 0);
            d.arenaProtect = s.getBoolean("arena.protect", false);
            d.arenaRadius = s.getDouble("arena.radius", 30);
            d.altarWorld = s.getString("altar.world", "");
            d.altarX = s.getInt("altar.x");
            d.altarY = s.getInt("altar.y");
            d.altarZ = s.getInt("altar.z");
            d.altarItem = s.getString("altar.item", "");
            d.introCinematic = s.getString("intro-cinematic", "");
            d.outroCinematic = s.getString("outro-cinematic", "");
            d.headshotMult = s.getDouble("headshot-mult", 1);
            d.enrageSeconds = s.getInt("enrage.seconds", 0);
            d.enrageDmgMult = s.getDouble("enrage.dmg-mult", 1.5);
            d.enrageSpeedMult = s.getDouble("enrage.speed-mult", 1.2);
            for (int i = 0; i < d.phases.length; i++) {
                String k = "phase" + (i + 1) + ".";
                Phase ph = d.phases[i];
                ph.pct = s.getInt(k + "pct", 0);
                ph.dmgMult = s.getDouble(k + "dmg-mult", 1.3);
                ph.speedMult = s.getDouble(k + "speed-mult", 1.15);
                ph.message = s.getString(k + "message", "");
                ph.refillShield = s.getBoolean(k + "refill-shield", false);
                ph.addsCount = s.getInt(k + "adds-count", 0);
                ph.addsMob = Enums.parse(EntityType.class, s.getString(k + "adds-mob"), EntityType.HUSK);
                ph.abilities.addAll(s.getStringList(k + "abilities"));
            }
            d.abilities.addAll(s.getStringList("abilities"));
            ConfigurationSection ds = s.getConfigurationSection("drops");
            if (ds != null) {
                for (int i = 0; i < d.drops.length; i++) d.drops[i].read(ds.getConfigurationSection("slot" + i));
            } else if (!s.getString("drop-item", "").isEmpty()) {
                d.drops[0].itemId = s.getString("drop-item", "");
                d.drops[0].chance = s.getDouble("drop-chance", 25);
            }
            d.extraSpawns.addAll(s.getStringList("spawn.extra"));
            d.spawnRadius = s.getDouble("spawn.radius", 0);
            d.spawnMinPlayers = s.getInt("spawn.min-players", 0);
            d.respawnMaxMinutes = s.getInt("spawn.respawn-max-minutes", 0);
            d.spawnEnabled = s.getBoolean("spawn.enabled", false);
            d.spawnWorld = s.getString("spawn.world", "");
            d.spawnX = s.getDouble("spawn.x");
            d.spawnY = s.getDouble("spawn.y");
            d.spawnZ = s.getDouble("spawn.z");
            d.respawnMinutes = s.getInt("spawn.respawn-minutes", 30);
            return d;
        }

        @Override
        public void write(BossDef d, ConfigurationSection s) {
            s.set("name", d.name);
            s.set("icon", d.icon.name());
            s.set("model", d.modelId);
            s.set("hide-base", d.hideBase);
            d.anims.write(s.createSection("anims"));
            s.set("base-mob", d.baseMob.name());
            s.set("health", d.health);
            s.set("damage", d.damage);
            s.set("armor", d.armor);
            s.set("speed", d.speed);
            s.set("scale", d.scale);
            s.set("knockback-resist", d.knockbackResist);
            s.set("detect-range", d.detectRange);
            s.set("leash-radius", d.leashRadius);
            s.set("ability-interval-ticks", d.abilityIntervalTicks);
            s.set("bar-color", d.barColor);
            s.set("particle", d.particle);
            s.set("element", d.element.name());
            s.set("crit-chance", d.critChance);
            s.set("crit-mult", d.critMult);
            s.set("lifesteal", d.lifesteal);
            s.set("shield", d.shield);
            s.set("escort.enabled", d.escortEnabled);
            for (int i = 0; i < d.escort.length; i++) d.escort[i].write(s.createSection("escort.slot" + i));
            s.set("family", d.family);
            s.set("family-reward-item", d.familyRewardItem);
            s.set("pity-after", d.pityAfter);
            s.set("arena.protect", d.arenaProtect);
            s.set("arena.radius", d.arenaRadius);
            s.set("altar.world", d.altarWorld);
            s.set("altar.x", d.altarX);
            s.set("altar.y", d.altarY);
            s.set("altar.z", d.altarZ);
            s.set("altar.item", d.altarItem);
            s.set("intro-cinematic", d.introCinematic);
            s.set("outro-cinematic", d.outroCinematic);
            s.set("headshot-mult", d.headshotMult);
            s.set("enrage.seconds", d.enrageSeconds);
            s.set("enrage.dmg-mult", d.enrageDmgMult);
            s.set("enrage.speed-mult", d.enrageSpeedMult);
            for (int i = 0; i < d.phases.length; i++) {
                String k = "phase" + (i + 1) + ".";
                Phase ph = d.phases[i];
                s.set(k + "pct", ph.pct);
                s.set(k + "dmg-mult", ph.dmgMult);
                s.set(k + "speed-mult", ph.speedMult);
                s.set(k + "message", ph.message);
                s.set(k + "refill-shield", ph.refillShield);
                s.set(k + "adds-count", ph.addsCount);
                s.set(k + "adds-mob", ph.addsMob.name());
                s.set(k + "abilities", ph.abilities);
            }
            s.set("abilities", d.abilities);
            for (int i = 0; i < d.drops.length; i++) d.drops[i].write(s.createSection("drops.slot" + i));
            s.set("spawn.extra", d.extraSpawns);
            s.set("spawn.radius", d.spawnRadius);
            s.set("spawn.min-players", d.spawnMinPlayers);
            s.set("spawn.respawn-max-minutes", d.respawnMaxMinutes);
            s.set("spawn.enabled", d.spawnEnabled);
            s.set("spawn.world", d.spawnWorld);
            s.set("spawn.x", d.spawnX);
            s.set("spawn.y", d.spawnY);
            s.set("spawn.z", d.spawnZ);
            s.set("spawn.respawn-minutes", d.respawnMinutes);
        }
    };
}
