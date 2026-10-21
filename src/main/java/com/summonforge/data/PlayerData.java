package com.summonforge.data;

import com.summonforge.summon.KeyAction;
import com.summonforge.summon.KeyInput;
import com.summonforge.summon.SummonMode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {
    public static final int MAX_LEVEL = 10;
    public static final int MAX_AWAKENING = 3;

    public final UUID uuid;
    public String summonId = "";
    public int level = 1;
    /** Habilidades equipadas (vacío = las primeras N de la invocación). */
    public final List<String> equipped = new ArrayList<>();
    public SummonMode mode = SummonMode.AGGRESSIVE;
    /** Puntos de lealtad (sube al matar enemigos con la invocación; 100 = 1 nivel, máx. 10). */
    public int loyalty = 0;
    /** Sincronización 0-100: sube con el tiempo que la invocación lucha a tu lado (+25 % de daño al máximo). */
    public int sync = 0;
    /** Enemigos derrotados por la invocación (requisito de despertar). */
    public int kills = 0;
    /** Bonus permanente (%) acumulado por fusiones. */
    public int fusion = 0;
    /** Runas equipadas (ids de objeto). */
    public final List<String> runes = new ArrayList<>();
    /** Maná del dueño (recurso de las habilidades). */
    public double mana = 100;
    /** Corazones perdidos por sobreuso de objetos y hasta cuándo (epoch ms). */
    public int heartsLost = 0;
    public long heartsLostUntil = 0;
    /** Fatiga acumulada por objeto (id -> usos recientes). */
    public final Map<String, Integer> fatigue = new HashMap<>();
    /** Despertar 0-3: +10 % de daño de la invocación por nivel. */
    public int awakening = 0;
    /** Kills consecutivas sin drop por boss (pity). */
    public final Map<String, Integer> pity = new HashMap<>();
    /** Familias de bosses completadas (recompensa ya entregada). */
    public final Set<String> families = new HashSet<>();
    public final Map<KeyAction, KeyInput> keys = new EnumMap<>(KeyAction.class);
    public final Map<String, Integer> talents = new HashMap<>();
    public final Map<String, Integer> bossKills = new HashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (KeyAction a : KeyAction.values()) keys.put(a, KeyInput.NONE);
        keys.put(KeyAction.TOGGLE, KeyInput.SHIFT_F);
        keys.put(KeyAction.ABILITY_1, KeyInput.F);
        keys.put(KeyAction.ABILITY_2, KeyInput.NONE);
        keys.put(KeyAction.ABILITY_3, KeyInput.NONE);
    }
}
