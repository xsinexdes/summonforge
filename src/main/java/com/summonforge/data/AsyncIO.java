package com.summonforge.data;

import com.summonforge.core.SummonForge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Un solo hilo de E/S en orden: los guardados no bloquean el servidor y no se pisan entre sí. */
public final class AsyncIO {
    private static ExecutorService executor;

    private AsyncIO() {}

    public static synchronized void start() {
        if (executor != null && !executor.isShutdown()) return;
        executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "SummonForge-IO"));
    }

    /** Si el hilo ya no existe (apagado), ejecuta en el momento para no perder datos. */
    public static synchronized void run(SummonForge plugin, Runnable task) {
        if (executor == null || executor.isShutdown()) {
            task.run();
            return;
        }
        executor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Error de E/S en segundo plano", t);
            }
        });
    }

    /** Espera a que terminen los guardados pendientes (se llama al apagar el plugin). */
    public static synchronized void shutdown() {
        if (executor == null) return;
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor = null;
    }
}
