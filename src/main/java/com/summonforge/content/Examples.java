package com.summonforge.content;

import com.summonforge.ability.AbilityPresets;
import com.summonforge.boss.BossDef;
import com.summonforge.cinematic.CinematicDef;
import com.summonforge.core.SummonForge;
import com.summonforge.item.ItemDef;
import com.summonforge.summon.SummonDef;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Contenido de ejemplo para probar el flujo completo al instante (no pisa lo ya existente). */
public final class Examples {
    private Examples() {}

    public static void install(SummonForge pl) {
        AbilityPresets.install(pl.abilityStore());

        Set<String> models = pl.model().modelKeys();
        Iterator<String> it = models.iterator();
        String m1 = it.hasNext() ? it.next() : "";
        String m2 = it.hasNext() ? it.next() : m1;

        if (!pl.summonStore().has("guardian_espectral")) {
            SummonDef s = new SummonDef("guardian_espectral");
            s.name = "<gradient:#8e44ad:#3498db>Guardián Espectral</gradient>";
            s.icon = Material.SOUL_LANTERN;
            s.baseMob = EntityType.IRON_GOLEM;
            s.health = 80;
            s.damage = 9;
            s.abilities.addAll(List.of("tajo_giratorio", "bola_de_fuego", "curacion_en_area"));
            if (!m1.isEmpty()) {
                s.modelId = m1;
                s.anims.autoFill(pl.model().animations(m1));
            }
            pl.summonStore().save(s);
        }

        if (!pl.itemStore().has("pergamino_guardian")) {
            ItemDef i = new ItemDef("pergamino_guardian");
            i.name = "<gradient:#8e44ad:#3498db>Pergamino: Guardián Espectral</gradient>";
            i.material = Material.PAPER;
            i.lore = "Sellado con un antiguo pacto|Úsalo para obtener su invocación";
            i.summonId = "guardian_espectral";
            pl.itemStore().save(i);
        }

        if (!pl.cinematicStore().has("intro_bestia")) {
            CinematicDef c = new CinematicDef("intro_bestia");
            c.name = "Intro de la Bestia";
            c.steps.add("SOUND;entity.ender_dragon.growl");
            c.steps.add("PARTICLES;FLAME;40");
            c.steps.add("TITLE;<gradient:red:gold>Bestia de Ceniza;El suelo tiembla...;60");
            pl.cinematicStore().save(c);
        }

        if (!pl.bossStore().has("bestia_ceniza")) {
            BossDef b = new BossDef("bestia_ceniza");
            b.name = "<gradient:red:gold>Bestia de Ceniza</gradient>";
            b.baseMob = EntityType.HUSK;
            b.health = 400;
            b.damage = 12;
            b.armor = 6;
            b.scale = 1.6;
            b.abilities.addAll(List.of("pisoton_sismico", "zarpazo_triple", "bola_de_fuego", "onda_de_impacto"));
            b.dropItemId = "pergamino_guardian";
            b.introCinematic = "intro_bestia";
            b.element = com.summonforge.stats.Element.FIRE;
            b.phases[0].pct = 60;
            b.phases[0].abilities.add("explosion_igneo");
            b.phases[0].message = "<red>¡La Bestia de Ceniza se enfurece!";
            b.phases[1].pct = 25;
            b.phases[1].abilities.add("meteoro");
            b.phases[0].addsCount = 2;
            b.enrageSeconds = 240;
            b.shield = 150;
            b.phases[1].refillShield = true;
            b.phases[1].dmgMult = 1.5;
            b.phases[1].message = "<dark_red>¡La Bestia de Ceniza entra en furia total!";
            b.dropChance = 100; // 100 % para probar el flujo; bájalo desde el editor
            if (!m2.isEmpty()) {
                b.modelId = m2;
                b.anims.autoFill(pl.model().animations(m2));
            }
            pl.bossStore().save(b);
        }
    }
}
