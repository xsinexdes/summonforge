package com.summonforge.core;

import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.CastContext;
import com.summonforge.boss.BossDef;
import com.summonforge.content.Examples;
import com.summonforge.gui.BestiaryMenu;
import com.summonforge.gui.KeysMenu;
import com.summonforge.gui.MainMenu;
import com.summonforge.gui.TalentMenu;
import com.summonforge.gui.MySummonMenu;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemFactory;
import com.summonforge.util.Fx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class InvCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBS = List.of("menu", "yo", "invocar", "despedir", "habilidad",
            "ejemplos", "dar", "boss", "spawn", "probar", "modelos", "reload",
            "validar", "estado", "rescue", "dummy", "bestiario", "talentos", "evento", "cine", "teclas");

    private final SummonForge plugin;

    public InvCommand(SummonForge plugin) {
        this.plugin = plugin;
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("summonforge.admin")) return true;
        plugin.messages().send(s, "no-permission");
        return false;
    }

    private Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        plugin.messages().send(s, "player-only");
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? (sender.hasPermission("summonforge.admin") ? "menu" : "yo") : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu" -> {
                if (!admin(sender)) return true;
                Player p = player(sender);
                if (p != null) new MainMenu(plugin, p).open();
            }
            case "yo" -> {
                Player p = player(sender);
                if (p == null) return true;
                if (!p.hasPermission("summonforge.use")) plugin.messages().send(p, "no-permission");
                else new MySummonMenu(plugin, p).open();
            }
            case "invocar" -> {
                Player p = player(sender);
                if (p != null && p.hasPermission("summonforge.use")) plugin.summons().toggle(p);
            }
            case "despedir" -> {
                Player p = player(sender);
                if (p != null) {
                    plugin.summons().dismiss(p.getUniqueId());
                    plugin.messages().send(p, "summon-dismissed");
                }
            }
            case "habilidad" -> {
                Player p = player(sender);
                if (p == null || !p.hasPermission("summonforge.use")) return true;
                int n;
                try {
                    n = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                } catch (NumberFormatException e) {
                    n = 1;
                }
                plugin.summons().castSlot(p, n - 1);
            }
            case "ejemplos" -> {
                if (!admin(sender)) return true;
                Examples.install(plugin);
                plugin.messages().send(sender, "examples-done");
            }
            case "reload" -> {
                if (!admin(sender)) return true;
                plugin.reloadAll();
                plugin.messages().send(sender, "reloaded");
            }
            case "modelos" -> {
                if (!admin(sender)) return true;
                plugin.messages().send(sender, "models-list", "n", String.valueOf(plugin.model().modelKeys().size()),
                        "list", String.join(", ", plugin.model().modelKeys()));
            }
            case "dar" -> {
                if (!admin(sender)) return true;
                if (args.length < 3) {
                    plugin.messages().send(sender, "usage", "usage", "/inv dar <jugador> <objeto>");
                    return true;
                }
                Player t = Bukkit.getPlayerExact(args[1]);
                ItemDef d = plugin.itemStore().get(args[2]);
                if (t == null) plugin.messages().send(sender, "player-not-found");
                else if (d == null) plugin.messages().send(sender, "item-not-found");
                else {
                    t.getInventory().addItem(ItemFactory.create(d));
                    plugin.messages().send(sender, "item-given", "item", d.id, "player", t.getName());
                }
            }
            case "boss" -> {
                if (!admin(sender)) return true;
                if (args.length < 2) {
                    plugin.messages().send(sender, "usage", "usage", "/inv boss <spawn|kill> [id]");
                    return true;
                }
                if (args[1].equalsIgnoreCase("spawn")) {
                    Player p = player(sender);
                    if (p == null) return true;
                    BossDef d = args.length > 2 ? plugin.bossStore().get(args[2]) : null;
                    if (d == null) plugin.messages().send(p, "boss-not-found");
                    else {
                        Location l = p.getLocation().add(Fx.forward(p.getLocation()).multiply(6));
                        if (plugin.bosses().spawn(d, l)) plugin.messages().send(p, "boss-spawned", "id", d.id);
                    }
                } else if (args[1].equalsIgnoreCase("kill")) {
                    int n = plugin.bosses().kill(args.length > 2 ? args[2] : null);
                    plugin.messages().send(sender, "boss-removed", "n", String.valueOf(n));
                }
            }
            case "spawn" -> {
                if (!admin(sender)) return true;
                Player p = player(sender);
                if (p == null) return true;
                BossDef d = args.length > 2 && args[1].equalsIgnoreCase("set") ? plugin.bossStore().get(args[2]) : null;
                if (d == null) {
                    plugin.messages().send(p, "usage", "usage", "/inv spawn set <idBoss>");
                } else {
                    d.setSpawn(p.getLocation());
                    d.spawnEnabled = true;
                    plugin.bossStore().save(d);
                    plugin.messages().send(p, "boss-spawn-set", "id", d.id);
                }
            }
            case "probar" -> {
                if (!admin(sender)) return true;
                Player p = player(sender);
                if (p == null) return true;
                AbilityDef d = args.length > 1 ? plugin.abilityStore().get(args[1]) : null;
                if (d == null) plugin.messages().send(p, "ability-missing");
                else plugin.abilities().cast(new CastContext(p, p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.0), d, true);
            }
            case "teclas" -> {
                Player p = player(sender);
                if (p != null && p.hasPermission("summonforge.use")) new KeysMenu(plugin, p).open();
            }
            case "cine" -> {
                if (!admin(sender)) return true;
                Player p = player(sender);
                if (p == null) return true;
                if (args.length < 2 || !plugin.cinematics().play(args[1], List.of(p), p.getLocation())) {
                    plugin.messages().send(p, "cinematic-not-found");
                }
            }
            case "validar" -> {
                if (admin(sender)) plugin.admin().validate(sender);
            }
            case "estado" -> {
                if (admin(sender)) plugin.admin().status(sender);
            }
            case "rescue" -> {
                if (admin(sender)) plugin.messages().send(sender, "rescue-done", "n", String.valueOf(plugin.admin().rescue()));
            }
            case "dummy" -> {
                if (!admin(sender)) return true;
                Player p = player(sender);
                if (p != null) plugin.admin().spawnDummy(p);
            }
            case "bestiario" -> {
                Player p = player(sender);
                if (p != null && p.hasPermission("summonforge.use")) new BestiaryMenu(plugin, p).open();
            }
            case "talentos" -> {
                Player p = player(sender);
                if (p != null && p.hasPermission("summonforge.use")) new TalentMenu(plugin, p).open();
            }
            case "evento" -> {
                if (!admin(sender)) return true;
                BossDef d = args.length > 1 ? plugin.bossStore().get(args[1]) : null;
                List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (d == null) {
                    plugin.messages().send(sender, "boss-not-found");
                } else if (!online.isEmpty()) {
                    Player t = online.get(ThreadLocalRandom.current().nextInt(online.size()));
                    Location l = t.getLocation().add(Fx.forward(t.getLocation()).multiply(8));
                    if (plugin.bosses().spawn(d, l)) plugin.messages().send(sender, "boss-spawned", "id", d.id);
                }
            }
            default -> plugin.messages().send(sender, "usage", "usage", "/inv [" + String.join("|", SUBS) + "]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String cur = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> pool = new ArrayList<>();
        if (args.length == 2) {
            switch (sub) {
                case "dar" -> Bukkit.getOnlinePlayers().forEach(p -> pool.add(p.getName()));
                case "boss" -> pool.addAll(List.of("spawn", "kill"));
                case "evento" -> pool.addAll(plugin.bossStore().ids());
                case "cine" -> pool.addAll(plugin.cinematicStore().ids());
                case "spawn" -> pool.add("set");
                case "probar" -> pool.addAll(plugin.abilityStore().ids());
                default -> { }
            }
        } else if (args.length == 3) {
            switch (sub) {
                case "dar" -> pool.addAll(plugin.itemStore().ids());
                case "boss", "spawn" -> pool.addAll(plugin.bossStore().ids());
                default -> { }
            }
        }
        for (String s : pool) if (s.toLowerCase(Locale.ROOT).startsWith(cur)) out.add(s);
        return out;
    }
}
