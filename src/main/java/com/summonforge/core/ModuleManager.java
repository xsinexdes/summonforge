package com.summonforge.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** Activa solo los módulos implementados/habilitados; un módulo que falla no tumba al resto. */
public final class ModuleManager {
    @FunctionalInterface
    public interface Task {
        void run() throws Exception;
    }

    private record Entry(String id, Task enable, Runnable disable) {}

    private final SummonForge plugin;
    private final List<Entry> modules = new ArrayList<>();
    private final List<Entry> enabled = new ArrayList<>();

    public ModuleManager(SummonForge plugin) {
        this.plugin = plugin;
    }

    public void register(String id, Task enable, Runnable disable) {
        modules.add(new Entry(id, enable, disable));
    }

    public void enableAll() {
        for (Entry m : modules) {
            if (!plugin.getConfig().getBoolean("modules." + m.id(), true)) {
                plugin.getLogger().info("Módulo desactivado por config: " + m.id());
                continue;
            }
            try {
                m.enable().run();
                enabled.add(m);
                plugin.getLogger().info("Módulo activo: " + m.id());
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "El módulo '" + m.id() + "' falló y queda desactivado", t);
            }
        }
    }

    public void disableAll() {
        for (int i = enabled.size() - 1; i >= 0; i--) {
            try {
                enabled.get(i).disable().run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Error al apagar el módulo " + enabled.get(i).id(), t);
            }
        }
        enabled.clear();
    }

    public boolean isEnabled(String id) {
        return enabled.stream().anyMatch(m -> m.id().equals(id));
    }
}
