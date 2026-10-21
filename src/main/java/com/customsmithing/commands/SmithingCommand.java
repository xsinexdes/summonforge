package com.customsmithing.commands;

import com.customsmithing.CustomSmithing;
import com.customsmithing.data.Specialization;
import com.customsmithing.gui.MainMenuGUI;
import com.customsmithing.gui.RecipeEditorGUI;
import com.customsmithing.gui.SkillTreeGUI;
import com.customsmithing.util.IdValidator;
import com.customsmithing.util.Messages;
import com.customsmithing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SmithingCommand implements CommandExecutor, TabCompleter {

    private final CustomSmithing plugin;
    private final java.util.Map<java.util.UUID, org.bukkit.Location> pos1 = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, org.bukkit.Location> pos2 = new java.util.HashMap<>();

    /** Olvida las esquinas marcadas por un jugador que se desconecta (antes se acumulaban para siempre). */
    public void forget(java.util.UUID player) {
        pos1.remove(player);
        pos2.remove(player);
    }

    public SmithingCommand(CustomSmithing plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) { sendHelp(sender); return true; }
            new MainMenuGUI(plugin, player).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "menu" -> {
                if (requirePlayer(sender) instanceof Player player) new MainMenuGUI(plugin, player).open(player);
            }
            case "skills" -> {
                if (requirePlayer(sender) instanceof Player player) new SkillTreeGUI(plugin, player).open(player);
            }
            case "tree" -> {
                if (requirePlayer(sender) instanceof Player player) new com.customsmithing.gui.RecipeTreeGUI(plugin, player).open(player);
            }
            case "seasons" -> {
                if (!sender.hasPermission("customsmithing.admin")) {
                    Messages.send(sender, "general.sin_permiso");
                } else if (requirePlayer(sender) instanceof Player player) {
                    new com.customsmithing.gui.SeasonConfigGUI(plugin, player).open(player);
                }
            }
            case "recipe" -> handleRecipe(sender, args);
            case "alloy" -> handleAlloy(sender, args);
            case "mineral" -> handleMineral(sender, args);
            case "boss" -> handleBoss(sender, args);
            case "spawnboss" -> handleSpawnBoss(sender, args);
            case "structure" -> handleStructure(sender, args);
            case "giveblueprint" -> handleGiveBlueprint(sender, args);
            case "givegoggles" -> handleGiveGoggles(sender, args);
            case "givebook" -> handleGiveBook(sender, args);
            case "spec" -> handleSpec(sender, args);
            case "tool" -> {
                if (!(sender instanceof Player p)) { Messages.send(sender, "general.solo_jugadores"); return true; }
                var type = args.length > 1 ? com.customsmithing.data.ToolType.parse(args[1]) : null;
                if (type == null) { Messages.send(sender, "herramientas.uso"); return true; }
                boolean free = p.hasPermission("customsmithing.admin");
                if (!free) {
                    org.bukkit.Material cost = org.bukkit.Material.matchMaterial(plugin.getConfig().getString("herramientas.costo_material", "IRON_INGOT"));
                    int amount = Math.max(0, plugin.getConfig().getInt("herramientas.costo_cantidad", 6));
                    if (cost != null && cost.isItem() && !cost.isAir() && amount > 0
                            && !com.customsmithing.item.ItemRef.of(cost).remove(p.getInventory(), amount)) {
                        Messages.send(p, "herramientas.sin_material", "%cantidad%", String.valueOf(amount), "%material%", Text.prettify(cost.name()));
                        return true;
                    }
                }
                plugin.getGiveService().give(p, com.customsmithing.item.Tools.create(plugin, type, com.customsmithing.item.Tools.defaultUses(plugin)));
                Messages.send(p, "herramientas.recibida", "%herramienta%", type.getLabel());
            }
            case "stations" -> {
                if (sender instanceof Player p) new com.customsmithing.gui.StationsGUI(plugin, p).open(p);
                else Messages.send(sender, "general.solo_jugadores");
            }
            case "salvage" -> {
                if (sender instanceof Player p) plugin.getForgeService().salvage(p);
                else Messages.send(sender, "general.solo_jugadores");
            }
            case "reforge" -> {
                if (sender instanceof Player p) plugin.getForgeService().reforge(p);
                else Messages.send(sender, "general.solo_jugadores");
            }
            case "debug" -> {
                if (!sender.hasPermission("customsmithing.admin")) { Messages.send(sender, "general.sin_permiso"); return true; }
                boolean now = !plugin.getConfig().getBoolean("general.debug", false);
                plugin.getConfig().set("general.debug", now);   // solo en memoria: vuelve al valor del archivo con /smithing reload
                sender.sendMessage("§7Modo debug: " + (now ? "§aactivado" : "§cdesactivado") + " §8(hasta el próximo reload)");
            }
            case "admin" -> {
                if (!sender.hasPermission("customsmithing.admin")) Messages.send(sender, "general.sin_permiso");
                else if (sender instanceof Player p) new com.customsmithing.gui.AdminHubGUI(plugin, p).open(p);
                else Messages.send(sender, "general.solo_jugadores");
            }
            case "testbench" -> {
                if (!sender.hasPermission("customsmithing.admin")) Messages.send(sender, "general.sin_permiso");
                else if (sender instanceof Player p) new com.customsmithing.gui.TestBenchGUI(plugin, p).open(p);
                else Messages.send(sender, "general.solo_jugadores");
            }
            case "quests" -> {
                if (sender instanceof Player p) new com.customsmithing.gui.QuestGUI(plugin, p).open(p);
                else Messages.send(sender, "general.solo_jugadores");
            }
            case "reload" -> handleReload(sender);
            case "validate" -> handleDiagnostics(sender, false);
            case "selftest" -> handleDiagnostics(sender, true);
            default -> sendHelp(sender);
        }
        return true;
    }

    /** Valida un id nuevo antes de guardarlo (YAML o nombre de archivo). Avisa y sugiere una alternativa. */
    private boolean validId(CommandSender sender, String id) {
        String problem = IdValidator.problem(id);
        if (problem == null) return true;
        Messages.send(sender, "general.id_invalido", "%id%", id, "%motivo%", problem);
        String suggestion = IdValidator.normalize(id);
        if (suggestion != null) Messages.send(sender, "general.id_sugerencia", "%id%", suggestion);
        return false;
    }

    private CommandSender requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "general.solo_jugadores");
            return null;
        }
        return sender;
    }

    private void handleRecipe(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "admin.solo_un_jugador_puede_abrir");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("new")) {
            Messages.send(player, "admin.uso_smithing_recipe_new_id");
            return;
        }
        String id = args[2];
        if (!validId(sender, id)) return;
        new RecipeEditorGUI(plugin, player, id).open(player);
    }

    private void handleGiveBlueprint(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (args.length < 3) {
            Messages.send(sender, "admin.uso_smithing_giveblueprint_receta_jugador");
            return;
        }
        String recipeId = args[1];
        if (!plugin.getRecipeManager().exists(recipeId)) {
            Messages.send(sender, "recetas.no_encontrada");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            Messages.send(sender, "admin.jugador_no_encontrado_o_desconectado");
            return;
        }
        plugin.getBlueprintManager().giveBlueprint(target, recipeId);
        sender.sendMessage("§aPlano entregado a " + target.getName() + ".");
    }

    private void handleAlloy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "admin.solo_un_jugador_puede_abrir_2");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("new")) {
            Messages.send(player, "admin.uso_smithing_alloy_new_id");
            return;
        }
        if (!validId(sender, args[2])) return;
        new com.customsmithing.gui.AlloyEditorGUI(plugin, player, args[2]).open(player);
    }

    private void handleGiveBook(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (args.length < 3) {
            Messages.send(sender, "admin.uso_smithing_givebook_encantamiento_nivel");
            Messages.send(sender, "admin.ejemplo_smithing_givebook_sharpness_5");
            return;
        }
        org.bukkit.enchantments.Enchantment enchant = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(args[1].toLowerCase()));
        if (enchant == null) {
            sender.sendMessage("§cEncantamiento desconocido: " + args[1] + " (usa el id vanilla, ej: sharpness, protection, unbreaking)");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            Messages.send(sender, "admin.el_nivel_debe_ser_un");
            return;
        }
        Player target;
        if (args.length >= 4) {
            target = plugin.getServer().getPlayer(args[3]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            Messages.send(sender, "admin.especifica_un_jugador");
            return;
        }
        if (target == null) {
            Messages.send(sender, "admin.jugador_no_encontrado_o_desconectado");
            return;
        }

        org.bukkit.inventory.ItemStack book = com.customsmithing.item.MagicBooks.create(enchant, level);
        plugin.getGiveService().give(target, book);
        sender.sendMessage("§aLibro Mágico entregado a " + target.getName() + ".");
    }

    private void handleMineral(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("edit")) {
            var existing = plugin.getCustomMineralManager().get(args[2]);
            if (existing == null) { sender.sendMessage("§cNo existe un mineral con id '" + args[2] + "'."); return; }
            if (sender instanceof Player p) new com.customsmithing.gui.MineralEditorGUI(plugin, p, existing).open(p);
            else Messages.send(sender, "general.solo_jugadores");
            return;
        }
        if (args.length >= 4 && args[1].equalsIgnoreCase("rename")) {
            var existing = plugin.getCustomMineralManager().get(args[2]);
            if (existing == null) { sender.sendMessage("§cNo existe un mineral con id '" + args[2] + "'."); return; }
            existing.setDisplayName(String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)));
            plugin.getCustomMineralManager().save(existing);
            sender.sendMessage("§aMineral renombrado a: §f" + existing.getDisplayName());
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("new")) {
            Messages.send(sender, "admin.uso_smithing_mineral_new_id");
            Messages.send(sender, "admin.ejemplo_smithing_mineral_new_adamantita");
            return;
        }
        String id = args[2];
        if (!validId(sender, id)) return;
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
        String name = nameBuilder.toString().trim();

        com.customsmithing.minerals.CustomMineral mineral = new com.customsmithing.minerals.CustomMineral(id);
        mineral.setDisplayName(name);
        if (sender instanceof Player player) {
            org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() != org.bukkit.Material.AIR) {
                mineral.setIconMaterial(hand.getType());
            }
        }
        plugin.getCustomMineralManager().save(mineral);
        sender.sendMessage("§aMineral '" + id + "' creado (" + name + "). Úsalo en el editor de recetas.");
        if (sender instanceof Player p) new com.customsmithing.gui.MineralEditorGUI(plugin, p, mineral).open(p);
    }

    private void handleBoss(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "admin.solo_un_jugador_puede_abrir_3");
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("new")) {
            Messages.send(player, "admin.uso_smithing_boss_new_id");
            return;
        }
        if (!validId(sender, args[2])) return;
        new com.customsmithing.gui.BossEditorGUI(plugin, player, args[2]).open(player);
    }

    private void handleSpawnBoss(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "admin.solo_un_jugador_puede_hacer");
            return;
        }
        if (args.length < 2) {
            Messages.send(player, "admin.uso_smithing_spawnboss_id");
            return;
        }
        if (!plugin.getBossManager().exists(args[1])) {
            Messages.send(player, "admin.ese_jefe_no_existe");
            return;
        }
        plugin.getBossManager().spawn(args[1], player.getLocation());
        player.sendMessage("§a¡Jefe '" + args[1] + "' invocado!");
    }

    private void handleStructure(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "admin.solo_un_jugador_puede_hacer");
            return;
        }
        if (args.length < 2) {
            Messages.send(player, "admin.uso_smithing_structure_pos1_pos2");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "pos1" -> {
                pos1.put(player.getUniqueId(), player.getLocation());
                Messages.send(player, "admin.esquina_1_marcada");
            }
            case "pos2" -> {
                pos2.put(player.getUniqueId(), player.getLocation());
                Messages.send(player, "admin.esquina_2_marcada");
            }
            case "save" -> {
                if (args.length < 3) { Messages.send(player, "admin.uso_smithing_structure_save_id"); return; }
                if (!validId(sender, args[2])) return;
                org.bukkit.Location p1 = pos1.get(player.getUniqueId());
                org.bukkit.Location p2 = pos2.get(player.getUniqueId());
                if (p1 == null || p2 == null) {
                    Messages.send(player, "admin.marca_las_dos_esquinas_primero");
                    return;
                }
                int count = plugin.getStructureManager().capture(args[2], p1, p2);
                if (count == com.customsmithing.structures.StructureManager.ERR_DIFFERENT_WORLDS) {
                    Messages.send(player, "admin.las_dos_esquinas_deben_estar");
                    return;
                }
                if (count == com.customsmithing.structures.StructureManager.ERR_TOO_BIG) {
                    player.sendMessage("§cLa región es demasiado grande (máximo " + plugin.getConfig().getLong("estructuras.max_volumen", 200_000) + " bloques, ver estructuras.max_volumen).");
                    return;
                }
                player.sendMessage("§aEstructura '" + args[2] + "' guardada (" + count + " bloques).");
            }
            case "list" -> {
                player.sendMessage("§7Estructuras guardadas: §f" + String.join(", ", plugin.getStructureManager().listIds()));
            }
            case "paste" -> {
                if (args.length < 3) { Messages.send(player, "admin.uso_smithing_structure_paste_id"); return; }
                var spawnPoint = plugin.getStructureManager().paste(args[2], player.getLocation(),
                        () -> Messages.send(player, "admin.estructura_pegada_por_completo"));
                if (spawnPoint == null) { Messages.send(player, "admin.esa_estructura_no_existe"); return; }
                Messages.send(player, "admin.pegando_la_estructura_por_lotes");
            }
            default -> Messages.send(player, "admin.uso_smithing_structure_pos1_pos2");
        }
    }

    private void handleGiveGoggles(CommandSender sender, String[] args) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            Messages.send(sender, "admin.uso_smithing_givegoggles_jugador");
            return;
        }
        if (target == null) {
            Messages.send(sender, "admin.jugador_no_encontrado_o_desconectado");
            return;
        }
        plugin.getGiveService().give(target, plugin.createGogglesItem());
        sender.sendMessage("§aLentes del Herrero entregados a " + target.getName() + ".");
    }

    private void handleSpec(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        if (args.length < 2) {
            Messages.send(player, "admin.uso_smithing_spec_espadas_armaduras");
            return;
        }
        Specialization spec;
        try {
            spec = Specialization.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            Messages.send(player, "especializacion.invalida");
            return;
        }
        long wait = plugin.getUnlockService().changeSpecialization(player, spec);
        if (wait > 0) Messages.send(player, "especializacion.cooldown", "%tiempo%", Text.formatDuration(wait));
        else Messages.send(player, "especializacion.cambiada", "%especializacion%", spec.getLabel());
    }

    private void handleDiagnostics(CommandSender sender, boolean selftest) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        var diagnostics = new com.customsmithing.admin.Diagnostics(plugin);
        for (String line : selftest ? diagnostics.selftest() : diagnostics.validate()) sender.sendMessage(line);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "general.sin_permiso");
            return;
        }
        plugin.reloadAll();
        Messages.send(sender, "general.recargado");
    }

    private void sendHelp(CommandSender sender) {
        Messages.send(sender, "admin.custom_smithing_comandos");
        Messages.send(sender, "admin.smithing_abre_el_menu_principal");
        Messages.send(sender, "admin.smithing_skills_arbol_de_habilidades");
        Messages.send(sender, "admin.smithing_tree_arbol_de_recetas");
        Messages.send(sender, "admin.smithing_quests_tus_misiones_y");
        Messages.send(sender, "admin.smithing_tool_tenazas_martillo_pide");
        Messages.send(sender, "admin.smithing_stations_mejora_tus_estaciones");
        Messages.send(sender, "admin.smithing_salvage_desguaza_el_objeto");
        Messages.send(sender, "admin.smithing_reforge_vuelve_a_tirar");
        Messages.send(sender, "admin.smithing_spec_tipo_elegir_especializacion");
        if (sender.hasPermission("customsmithing.admin")) {
            Messages.send(sender, "admin.smithing_recipe_new_id_editor");
            Messages.send(sender, "admin.smithing_alloy_new_id_editor");
            Messages.send(sender, "admin.smithing_mineral_new_id_nombre");
            Messages.send(sender, "admin.smithing_boss_new_id_editor");
            Messages.send(sender, "admin.smithing_spawnboss_id_invocar_un");
            Messages.send(sender, "admin.smithing_structure_pos1_pos2_save");
            Messages.send(sender, "admin.smithing_giveblueprint_receta_jugador_dar");
            Messages.send(sender, "admin.smithing_givegoggles_jugador_dar_los");
            Messages.send(sender, "admin.smithing_givebook_encantamiento_nivel_jugador");
            Messages.send(sender, "admin.smithing_seasons_configurar_estaciones_del");
            Messages.send(sender, "admin.smithing_debug_activa_o_desactiva");
            Messages.send(sender, "admin.smithing_admin_menu_de_administracion");
            Messages.send(sender, "admin.smithing_testbench_banco_de_pruebas");
            Messages.send(sender, "admin.smithing_validate_revisar_que_recetas");
            Messages.send(sender, "admin.smithing_selftest_probar_el_circuito");
            Messages.send(sender, "admin.smithing_reload_recargar_configuracion_admin");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        boolean admin = sender.hasPermission("customsmithing.admin");
        String sub = args.length >= 1 ? args[0].toLowerCase() : "";

        if (args.length == 1) {
            options.addAll(List.of("help", "menu", "skills", "tree", "quests", "stations", "tool", "spec", "salvage", "reforge"));
            if (admin) {
                options.addAll(List.of("recipe", "alloy", "mineral", "boss", "spawnboss", "structure", "giveblueprint",
                        "reload", "seasons", "givegoggles", "givebook", "validate", "selftest", "admin", "testbench", "debug"));
            }
        } else if (!admin) {
            // los jugadores normales solo tienen /smithing spec <especialización>
            if (args.length == 2 && sub.equals("spec")) {
                for (Specialization s : Specialization.values()) options.add(s.name().toLowerCase());
            } else if (args.length == 2 && sub.equals("tool")) {
                for (var t : com.customsmithing.data.ToolType.values()) options.add(t.name().toLowerCase());
            }
        } else if (args.length == 2) {
            switch (sub) {
                case "spec" -> { for (Specialization s : Specialization.values()) options.add(s.name().toLowerCase()); }
                case "tool" -> { for (var t : com.customsmithing.data.ToolType.values()) options.add(t.name().toLowerCase()); }
                case "recipe", "alloy", "boss" -> options.add("new");
                case "mineral" -> options.addAll(List.of("new", "edit", "rename"));
                case "giveblueprint" -> plugin.getRecipeManager().getAll().forEach(r -> options.add(r.getId()));
                case "spawnboss" -> plugin.getBossManager().getAll().forEach(b -> options.add(b.getId()));
                case "structure" -> options.addAll(List.of("pos1", "pos2", "save", "list", "paste"));
                case "givebook" -> org.bukkit.Registry.ENCHANTMENT.forEach(e -> options.add(e.getKey().getKey()));
                case "givegoggles" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                default -> { }
            }
        } else if (args.length == 3) {
            switch (sub) {
                case "giveblueprint" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                case "givebook" -> options.addAll(List.of("1", "2", "3", "4", "5"));
                case "mineral" -> { if (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("rename")) plugin.getCustomMineralManager().getAll().forEach(m -> options.add(m.getId())); }
                case "structure" -> { if (args[1].equalsIgnoreCase("paste")) options.addAll(plugin.getStructureManager().listIds()); }
                default -> { }
            }
        } else if (args.length == 4 && sub.equals("givebook")) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String current = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(current)).collect(Collectors.toList());
    }
}
