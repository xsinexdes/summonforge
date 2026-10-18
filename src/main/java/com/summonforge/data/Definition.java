package com.summonforge.data;

import org.bukkit.Material;

/** Contrato de toda definición editable por GUI (habilidad, invocación, boss, objeto...). */
public interface Definition {
    String id();

    /** Nombre mostrable (MiniMessage). */
    String displayName();

    Material icon();
}
