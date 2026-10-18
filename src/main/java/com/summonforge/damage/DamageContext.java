package com.summonforge.damage;

import com.summonforge.stats.Element;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** owner = jugador al que se le atribuye el daño (null si el atacante actúa solo, p. ej. un boss). */
public record DamageContext(LivingEntity attacker, Player owner, LivingEntity target,
                            double base, Element element, String source) {}
