package com.summonforge.core;

import com.summonforge.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/** Mensajes editables (messages.yml, MiniMessage). Las claves que falten se rellenan solas. */
public final class Messages {
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        d("prefix", "<dark_gray>[<gradient:#8e44ad:#3498db>SummonForge</gradient><dark_gray>] <gray>");
        d("no-permission", "<red>No tienes permiso para eso.");
        d("player-only", "<red>Solo un jugador puede usar esto.");
        d("reloaded", "<green>Configuración y definiciones recargadas.");
        d("usage", "<yellow>Uso: <white>{usage}");
        d("prompt-hint", "<yellow>{what}\n<gray>Escribe el valor en el chat, o <white>cancelar</white> para salir (30 s).");
        d("prompt-cancelled", "<gray>Cancelado.");
        d("prompt-expired", "<gray>Tiempo agotado, entrada cancelada.");
        d("invalid-number", "<red>Valor inválido: <white>{v}</white> <gray>(rango {min} a {max})");
        d("invalid-id", "<red>ID inválido: usa solo a-z, 0-9 y _ (máx. 32).");
        d("id-exists", "<red>Ya existe una definición con ese ID.");
        d("created", "<green>Creado: <white>{id}");
        d("deleted", "<green>Borrado: <white>{id}");
        d("duplicated", "<green>Duplicado como: <white>{id}");
        d("need-model", "<red>Primero elige un modelo.");
        d("hand-empty", "<red>Sostén un ítem en la mano principal.");
        d("summon-got", "<green>Has obtenido la invocación <white>{name}</white><green>.");
        d("summon-already", "<yellow>Ya tienes esa invocación.");
        d("summon-spawned", "<green>{name} <gray>(Nv. {level}) <green>ha sido invocado.");
        d("summon-dismissed", "<gray>Tu invocación se retiró.");
        d("summon-none", "<red>No tienes ninguna invocación.");
        d("summon-dead", "<red>Tu invocación cayó. Podrás volver a invocarla en {s} s.");
        d("summon-revive-wait", "<red>Tu invocación aún se recupera: {s} s.");
        d("summon-levelup", "<green>Tu invocación ahora es nivel <white>{level}</white><green>.");
        d("summon-max", "<yellow>Tu invocación ya está al nivel máximo.");
        d("summon-no-xp", "<red>Necesitas {cost} niveles de experiencia.");
        d("summon-not-active", "<red>Invoca primero a tu invocación (Shift + F).");
        d("ability-cooldown", "<red>Habilidad en enfriamiento: {s} s");
        d("ability-missing", "<red>Esa habilidad no existe o no está equipada.");
        d("boss-announce", "<gradient:red:gold>¡{name}<gradient:red:gold> ha aparecido en {world}!");
        d("boss-killed", "<gold>{name}<gold> ha caído. <gray>Top daño: <white>{top}");
        d("boss-drop", "<light_purple>¡{player} obtuvo {item}<light_purple> de {boss}<light_purple>!");
        d("boss-not-found", "<red>No existe ese boss.");
        d("boss-spawned", "<green>Boss invocado: <white>{id}");
        d("boss-removed", "<green>Bosses eliminados: <white>{n}");
        d("boss-spawn-set", "<green>Punto de spawn guardado para <white>{id}</white>.");
        d("item-given", "<green>Entregado <white>{item}</white> a <white>{player}</white>.");
        d("item-not-found", "<red>No existe ese objeto.");
        d("player-not-found", "<red>Jugador no encontrado.");
        d("model-unavailable", "<yellow>BetterModel no está disponible: se usará el modo sin modelo.");
        d("models-list", "<gray>Modelos de BetterModel detectados (<white>{n}</white>): <white>{list}");
        d("examples-done", "<green>Contenido de ejemplo instalado. Abre <white>/inv</white> para verlo.");
        d("loyalty-up", "<light_purple>Tu lealtad con la invocación sube al nivel <white>{level}</white><light_purple>.");
        d("talent-none", "<red>No tienes puntos de talento libres (ganas 1 por nivel).");
        d("talent-max", "<yellow>Ese talento ya está al máximo.");
        d("talent-applied", "<green>Talento mejorado. Se aplica al volver a invocar.");
        d("talent-reset", "<yellow>Talentos reiniciados.");
        d("rescue-done", "<green>Entidades huérfanas eliminadas: <white>{n}");
        d("preview-started", "<gray>Vista previa de <white>{model}</white> (15 s, {n} animaciones).");
        d("invalid-step", "<red>Paso no válido. Tipos: TITLE, SOUND, MESSAGE, WAIT, FREEZE, PARTICLES.");
        d("cinematic-not-found", "<red>No existe esa cinemática.");
        d("awaken-up", "<light_purple>Tu invocación despierta: nivel de despertar <white>{level}</white><light_purple>.");
        d("awaken-max", "<yellow>Tu invocación ya alcanzó el despertar máximo.");
        d("item-cooldown", "<red>Aún no puedes usarlo: {s} s.");
        d("power-used", "<gold>Poder activado durante {s} s. Después sufrirás fatiga.");
        d("power-fatigue", "<gray>Fatiga: estás agotado durante {s} s.");
        d("family-complete", "<gold>¡Has derrotado a toda la familia <white>{family}</white><gold>! Recibes una recompensa.");
        d("arena-protected", "<red>No puedes alterar bloques durante el combate.");
        d("altar-alive", "<red>El guardián de este altar ya está presente.");
        d("altar-needs", "<red>Necesitas {item}<red> en la mano para activar el altar.");
        d("no-mana", "<red>No tienes maná suficiente ({cost}).");
        d("mark-ok", "<green>Objetivo marcado para tu invocación.");
        d("mark-fail", "<red>Mira a un enemigo (a menos de 30 bloques) con tu invocación activa.");
        d("no-ultimate", "<red>No tienes ninguna habilidad ultimate equipada.");
        d("overuse", "<dark_red>¡Sobreuso! Tu cuerpo no lo soporta: ceguera y náuseas.");
        d("hearts-lost", "<dark_red>Pierdes {n} corazón(es) durante {m} min.");
        d("hearts-restored", "<green>Tus corazones se han restaurado.");
        d("visible-done", "<green>Entidades vueltas visibles: <white>{n}");
        d("awaken-stage", "<light_purple>Etapa alcanzada: <white>{stage}");
        d("awaken-req-kills", "<red>Tu invocación necesita {n} kills para despertar (lleva {have}).");
        d("awaken-req-sync", "<red>Necesitas {n}% de sincronización (tienes {have}%).");
        d("evolved", "<gold>¡Tu invocación evoluciona a <white>{name}</white><gold>!");
        d("evolve-level", "<red>Necesitas nivel {n} para evolucionar.");
        d("evolve-item", "<red>Necesitas {item}<red> en tu inventario para evolucionar.");
        d("fused", "<light_purple>Fusión completada: bonus permanente total +{bonus}%.");
        d("fusion-max", "<yellow>Tu invocación ya tiene el bonus de fusión máximo.");
        d("rune-full", "<red>No tienes slots de runa libres. Quita una desde Mi Invocación > Runas.");
        d("rune-equipped", "<green>Runa equipada: <white>{rune}");
        d("perfect-start", "<gradient:#ffd700:#ff5555>¡MODO PERFECTO! <gray>Tu sincronización con la invocación es total.");
        d("perfect-end", "<red>El Modo Perfecto termina: quedas agotado y la sincronización baja.");
        d("difficulty-set", "<green>Dificultad: <white>{name}</white> <gray>(vida x{hp}, daño x{dmg}, drops x{drop}, respawn x{respawn})");
        d("summon-expired", "<gray>Tu invocación terminó. Podrás invocarla de nuevo en {s} s.");
        d("equip-full", "<red>Solo puedes equipar {n} habilidades. Quita una primero.");
        d("boss-enrage", "<dark_red>¡{name}<dark_red> entra en ENRAGE!");
        d("mode-changed", "<gray>Modo de la invocación: <white>{mode}");
        d("replace-info", "<gray>Reemplazarás tu invocación actual.");
    }

    private final SummonForge plugin;
    private final File file;
    private YamlConfiguration cfg;

    public Messages(SummonForge plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    private static void d(String k, String v) {
        DEFAULTS.put(k, v);
    }

    public void reload() {
        cfg = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            if (!cfg.contains(e.getKey())) {
                cfg.set(e.getKey(), e.getValue());
                changed = true;
            }
        }
        if (changed) {
            try {
                plugin.getDataFolder().mkdirs();
                cfg.save(file);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "No se pudo guardar messages.yml", ex);
            }
        }
    }

    public String raw(String key, String... kv) {
        String s = cfg.getString(key, DEFAULTS.getOrDefault(key, key));
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        return s;
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(Text.mm(raw("prefix") + raw(key, kv)));
    }
}
