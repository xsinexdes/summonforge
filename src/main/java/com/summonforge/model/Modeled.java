package com.summonforge.model;

/** Definiciones que llevan modelo BetterModel + animaciones por estado. */
public interface Modeled {
    String modelId();

    void modelId(String id);

    AnimationSet anims();
}
