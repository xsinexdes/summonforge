package com.summonforge.boss;

import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/** Fase de boss: se activa cuando la vida baja de pct %. pct 0 = fase desactivada. */
public final class Phase {
    public int pct = 0;
    public double dmgMult = 1.3;
    public double speedMult = 1.15;
    /** Habilidades que se añaden al entrar en la fase. */
    public final List<String> abilities = new ArrayList<>();
    public String message = "";
    /** Esbirros que aparecen al entrar en la fase (0 = ninguno). */
    /** Rellena el escudo al entrar en la fase. */
    public boolean refillShield = false;
    public int addsCount = 0;
    public EntityType addsMob = EntityType.HUSK;
}
