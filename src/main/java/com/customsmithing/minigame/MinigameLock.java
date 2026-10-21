package com.customsmithing.minigame;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MinigameLock {

    private static final Set<UUID> ACTIVE = new HashSet<>();

    public static boolean isLocked(UUID uuid) {
        return ACTIVE.contains(uuid);
    }

    public static void lock(UUID uuid) {
        ACTIVE.add(uuid);
    }

    public static void unlock(UUID uuid) {
        ACTIVE.remove(uuid);
    }
}
