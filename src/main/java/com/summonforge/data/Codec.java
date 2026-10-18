package com.summonforge.data;

import org.bukkit.configuration.ConfigurationSection;

public interface Codec<T extends Definition> {
    T read(String id, ConfigurationSection s);

    void write(T d, ConfigurationSection s);
}
