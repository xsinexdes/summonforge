package com.customsmithing.util;

import com.customsmithing.CustomSmithing;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * Textos visibles desde messages.yml. Lee SIEMPRE la configuración vigente del plugin (así
 * /smithing reload sí recarga los mensajes) y, si el servidor tiene un messages.yml antiguo sin
 * alguna clave nueva, usa el texto que trae el jar. Un mensaje inexistente nunca se muestra
 * como ruta cruda: el jugador ve un aviso neutro y la consola registra cuál falta (una sola vez).
 */
public final class Messages {

    private static final String MISSING = "&cMensaje no configurado. Avisa a un administrador.";

    private static CustomSmithing plugin;
    private static final Set<String> WARNED = new HashSet<>();

    private Messages() {
    }

    public static void init(CustomSmithing owner) {
        plugin = owner;
        WARNED.clear();
    }

    /** Texto ya coloreado, con {prefix} y los reemplazos aplicados (pares clave, valor). */
    public static String get(String path, String... replacements) {
        FileConfiguration cfg = plugin.getMessagesConfig();
        String raw = cfg.getString(path);
        if (raw == null) {
            if (WARNED.add(path)) {
                plugin.getLogger().warning("messages.yml: falta el mensaje '" + path + "'.");
            }
            raw = MISSING;
        }
        raw = raw.replace("{prefix}", cfg.getString("prefix", ""));
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static void send(CommandSender target, String path, String... replacements) {
        target.sendMessage(get(path, replacements));
    }
}
