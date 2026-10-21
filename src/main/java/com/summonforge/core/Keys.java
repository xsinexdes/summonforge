package com.summonforge.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public static NamespacedKey BOSS;
    public static NamespacedKey SUMMON_OWNER;
    public static NamespacedKey SUMMON_ID;
    public static NamespacedKey ITEM_ID;
    public static NamespacedKey ITEM_UID;
    public static NamespacedKey DUMMY;

    private Keys() {}

    public static void init(Plugin p) {
        BOSS = new NamespacedKey(p, "boss");
        SUMMON_OWNER = new NamespacedKey(p, "summon_owner");
        SUMMON_ID = new NamespacedKey(p, "summon_id");
        ITEM_ID = new NamespacedKey(p, "item_id");
        ITEM_UID = new NamespacedKey(p, "item_uid");
        DUMMY = new NamespacedKey(p, "dummy");
    }
}
