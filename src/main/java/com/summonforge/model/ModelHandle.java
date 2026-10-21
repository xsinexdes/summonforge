package com.summonforge.model;

public interface ModelHandle {
    void play(String animation);

    void close();

    boolean isClosed();
}
