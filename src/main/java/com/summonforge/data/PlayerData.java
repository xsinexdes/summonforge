package com.summonforge.data;

import com.summonforge.summon.KeyAction;
import com.summonforge.summon.KeyInput;
import com.summonforge.summon.SummonMode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    public static final int MAX_LEVEL = 10;

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
    public final Map<KeyAction, KeyInput> keys = new EnumMap<>(KeyAction.class);
    public final Map<String, Integer> talents = new HashMap<>();
    public final Map<String, Integer> bossKills = new HashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        keys.put(KeyAction.TOGGLE, KeyInput.SHIFT_F);
        keys.put(KeyAction.ABILITY_1, KeyInput.F);
        keys.put(KeyAction.ABILITY_2, KeyInput.NONE);
        keys.put(KeyAction.ABILITY_3, KeyInput.NONE);
    }
}
