package com.customsmithing;

import com.customsmithing.blueprints.BlueprintManager;
import com.customsmithing.alloys.AlloyManager;
import com.customsmithing.minerals.CustomMineralManager;
import com.customsmithing.bosses.BossManager;
import com.customsmithing.bosses.BossListener;
import com.customsmithing.structures.StructureManager;
import com.customsmithing.commands.SmithingCommand;
import com.customsmithing.commands.QuickGuiCommand;
import com.customsmithing.data.PlayerDataManager;
import com.customsmithing.data.SmithingKeys;
import com.customsmithing.environment.EnvironmentModifier;
import com.customsmithing.environment.SeasonManager;
import com.customsmithing.admin.TestBench;
import com.customsmithing.data.QualityConfig;
import com.customsmithing.forge.ForgeService;
import com.customsmithing.gui.GUIListener;
import com.customsmithing.gui.GuiService;
import com.customsmithing.item.GiveService;
import com.customsmithing.listeners.AttackSpeedTask;
import com.customsmithing.material.MaterialRegistry;
import com.customsmithing.migration.MigrationService;
import com.customsmithing.piece.PieceCatalog;
import com.customsmithing.skin.SkinService;
import com.customsmithing.unlock.ProgressService;
import com.customsmithing.unlock.UnlockService;
import org.bukkit.Bukkit;
import com.customsmithing.listeners.BlockInteractListener;
import com.customsmithing.listeners.CombatListener;
import com.customsmithing.listeners.PlayerLifecycleListener;
import com.customsmithing.listeners.GogglesListener;
import com.customsmithing.quests.QuestManager;
import com.customsmithing.recipes.RecipeManager;
import com.customsmithing.util.Messages;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class CustomSmithing extends JavaPlugin {

    private static CustomSmithing instance;

    private PlayerDataManager playerDataManager;
    private RecipeManager recipeManager;
    private BlueprintManager blueprintManager;
    private AlloyManager alloyManager;
    private CustomMineralManager customMineralManager;
    private BossManager bossManager;
    private StructureManager structureManager;
    private BossListener bossListener;
    private com.customsmithing.listeners.SetBonusTask setBonusTask;
    private QuestManager questManager;
    private EnvironmentModifier environmentModifier;
    private SeasonManager seasonManager;
    private QualityConfig qualityConfig = QualityConfig.defaults();
    private GiveService giveService;
    private MaterialRegistry materialRegistry;
    private UnlockService unlockService;
    private ProgressService progressService;
    private TestBench testBench;
    private PieceCatalog pieceCatalog;
    private ForgeService forgeService;
    private SmithingCommand smithingCommand;
    private int autosaveTaskId = -1;
    private AttackSpeedTask attackSpeedTask;
    private SkinService skinService;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        SmithingKeys.init(this);
        loadMessages();
        Messages.init(this);
        loadQualityConfig();

        // Antes de cargar nada: convierte ids antiguos no válidos (con copia .bak).
        MigrationService.run(this);

        this.giveService = new GiveService(this);
        this.skinService = new SkinService(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.recipeManager = new RecipeManager(this);
        this.blueprintManager = new BlueprintManager(this);
        this.alloyManager = new AlloyManager(this);
        this.customMineralManager = new CustomMineralManager(this);
        this.materialRegistry = new MaterialRegistry(this);
        this.unlockService = new UnlockService(this);
        this.progressService = new ProgressService(this);
        this.testBench = new TestBench(this);
        this.pieceCatalog = new PieceCatalog(this);
        this.forgeService = new ForgeService(this);
        this.bossManager = new BossManager(this);
        this.structureManager = new StructureManager(this);
        this.questManager = new QuestManager(this);
        this.seasonManager = new SeasonManager(this);
        this.environmentModifier = new EnvironmentModifier(this);

        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new BlockInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this), this);
        getServer().getPluginManager().registerEvents(new GogglesListener(this), this);
        getServer().getPluginManager().registerEvents(new com.customsmithing.listeners.DurabilityListener(this), this);
        this.setBonusTask = new com.customsmithing.listeners.SetBonusTask(this);
        setBonusTask.start();
        this.bossListener = new BossListener(this);
        getServer().getPluginManager().registerEvents(bossListener, this);
        bossListener.start();
        this.attackSpeedTask = new AttackSpeedTask(this);
        attackSpeedTask.start();
        restartAutosave();

        SmithingCommand command = new SmithingCommand(this);
        this.smithingCommand = command;
        getCommand("smithing").setExecutor(command);
        getCommand("smithing").setTabCompleter(command);

        getCommand("habilidades").setExecutor(new QuickGuiCommand(this, QuickGuiCommand.Type.SKILLS));
        getCommand("recetas").setExecutor(new QuickGuiCommand(this, QuickGuiCommand.Type.RECIPE_TREE));
        getCommand("planos").setExecutor(new QuickGuiCommand(this, QuickGuiCommand.Type.BLUEPRINTS));
        getCommand("molde").setExecutor(new QuickGuiCommand(this, QuickGuiCommand.Type.MOLDS));

        registerGogglesRecipe();

        getLogger().info("⚒ Custom Smithing habilitado correctamente. ¡A forjar!");
    }

    @Override
    public void onDisable() {
        // Primero se cierran los menús abiertos (devuelve los items que tuvieran dentro):
        // durante onDisable Bukkit ya no entrega eventos a este plugin.
        GuiService.closeAll();
        if (playerDataManager != null) playerDataManager.saveAll();
        if (questManager != null) questManager.saveProgress();
        if (bossListener != null) bossListener.stop();
        if (setBonusTask != null) setBonusTask.stop();
        if (attackSpeedTask != null) attackSpeedTask.stop();
        getLogger().info("⚒ Custom Smithing deshabilitado. Datos guardados.");
    }

    /**
     * Recarga segura: cierra los menús abiertos (para que nadie siga con datos viejos ni pierda items),
     * relee configuración, mensajes y archivos, y reinicia las tareas periódicas para que
     * respeten los valores nuevos.
     * Debe ejecutarse en el hilo principal.
     */
    public void reloadAll() {
        GuiService.closeAll();

        reloadConfig();
        loadMessages();
        Messages.init(this);
        loadQualityConfig();
        skinService.load();

        materialRegistry.load();
        pieceCatalog.load();
        recipeManager.load();
        blueprintManager.load();
        alloyManager.load();
        customMineralManager.load();
        bossManager.load();
        questManager.load();
        seasonManager.loadFromConfig();

        restartTasks();
    }

    /** Guarda a todos los jugadores cada guardado.minutos (0 = desactivado), no solo al salir o apagar. */
    private void restartAutosave() {
        if (autosaveTaskId != -1) Bukkit.getScheduler().cancelTask(autosaveTaskId);
        autosaveTaskId = -1;
        long minutes = getConfig().getLong("guardado.minutos", 5);
        if (minutes <= 0) return;
        long ticks = minutes * 60L * 20L;
        autosaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> playerDataManager.saveAll(), ticks, ticks);
    }

    private void restartTasks() {
        restartAutosave();
        bossListener.stop();
        bossListener.start();
        setBonusTask.stop();
        setBonusTask.start();
    }

    private void loadQualityConfig() {
        FileConfiguration cfg = getConfig();
        this.qualityConfig = QualityConfig.load(cfg::getDouble,
                message -> getLogger().warning("config.yml › " + message));
    }

    /** Mensaje de detalle: solo aparece en consola si general.debug está en true. */
    public void debug(String message) {
        if (getConfig().getBoolean("general.debug", false)) {
            getLogger().info("[debug] " + message);
        }
    }

    private void loadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Las claves que falten en el archivo del servidor se toman del que viene en el jar.
        try (InputStream in = getResource("messages.yml")) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(reader));
                }
            }
        } catch (IOException e) {
            getLogger().warning("No se pudieron leer los mensajes por defecto del jar: " + e.getMessage());
        }
    }

    public org.bukkit.inventory.ItemStack createGogglesItem() {
        org.bukkit.inventory.ItemStack goggles = new com.customsmithing.util.ItemBuilder(org.bukkit.Material.SPYGLASS)
                .name("&b&lLentes del Herrero")
                .lore(java.util.List.of(
                        "&7Clic derecho para ver la temperatura,",
                        "&7el clima y accesos rápidos a los menús."))
                .build();
        org.bukkit.inventory.meta.ItemMeta meta = goggles.getItemMeta();
        meta.getPersistentDataContainer().set(SmithingKeys.GOGGLES_ITEM, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        goggles.setItemMeta(meta);
        return goggles;
    }

    private void registerGogglesRecipe() {
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(this, "lentes_del_herrero");
        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(key, createGogglesItem());
        recipe.shape(" G ", "IRI", " G ");
        recipe.setIngredient('G', org.bukkit.Material.GLASS_PANE);
        recipe.setIngredient('I', org.bukkit.Material.IRON_INGOT);
        recipe.setIngredient('R', org.bukkit.Material.REDSTONE);
        getServer().addRecipe(recipe);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public static CustomSmithing getInstance() { return instance; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public BlueprintManager getBlueprintManager() { return blueprintManager; }
    public AlloyManager getAlloyManager() { return alloyManager; }
    public CustomMineralManager getCustomMineralManager() { return customMineralManager; }
    public BossManager getBossManager() { return bossManager; }
    public StructureManager getStructureManager() { return structureManager; }
    public QuestManager getQuestManager() { return questManager; }
    public EnvironmentModifier getEnvironmentModifier() { return environmentModifier; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public QualityConfig getQualityConfig() { return qualityConfig; }
    public GiveService getGiveService() { return giveService; }
    public MaterialRegistry getMaterialRegistry() { return materialRegistry; }
    public UnlockService getUnlockService() { return unlockService; }
    public ProgressService getProgressService() { return progressService; }
    public TestBench getTestBench() { return testBench; }
    public PieceCatalog getPieceCatalog() { return pieceCatalog; }
    public ForgeService getForgeService() { return forgeService; }
    public SmithingCommand getSmithingCommand() { return smithingCommand; }
    public AttackSpeedTask getAttackSpeedTask() { return attackSpeedTask; }
    public SkinService getSkinService() { return skinService; }
}
