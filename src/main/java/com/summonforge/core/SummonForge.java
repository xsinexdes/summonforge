package com.summonforge.core;

import com.summonforge.admin.AdminTools;
import com.summonforge.cinematic.CinematicDef;
import com.summonforge.cinematic.CinematicPlayer;
import com.summonforge.data.AsyncIO;
import com.summonforge.ability.AbilityDef;
import com.summonforge.ability.AbilityEngine;
import com.summonforge.ability.AbilityPresets;
import com.summonforge.ability.SpecialAbilities;
import com.summonforge.boss.BossDef;
import com.summonforge.boss.BossManager;
import com.summonforge.boss.GlobalEvents;
import com.summonforge.damage.DamagePipeline;
import com.summonforge.dimension.LimboManager;
import com.summonforge.data.DefinitionStore;
import com.summonforge.data.PlayerDataStore;
import com.summonforge.gui.ChatPrompt;
import com.summonforge.gui.Kinds;
import com.summonforge.gui.MenuManager;
import com.summonforge.item.ItemDef;
import com.summonforge.item.ItemListener;
import com.summonforge.model.BetterModelAdapter;
import com.summonforge.model.ModelAdapter;
import com.summonforge.model.NullModelAdapter;
import com.summonforge.model.VisualManager;
import com.summonforge.summon.SummonDef;
import com.summonforge.trigger.TriggerDef;
import com.summonforge.trigger.TriggerEngine;
import com.summonforge.stats.ElementMatrix;
import com.summonforge.status.StatusDef;
import com.summonforge.status.StatusEngine;
import com.summonforge.status.StatusPresets;
import com.summonforge.summon.SummonManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SummonForge extends JavaPlugin {
    private static SummonForge instance;

    private Messages messages;
    private ModelAdapter model = new NullModelAdapter();
    private DefinitionStore<AbilityDef> abilityStore;
    private DefinitionStore<SummonDef> summonStore;
    private DefinitionStore<BossDef> bossStore;
    private DefinitionStore<ItemDef> itemStore;
    private DefinitionStore<StatusDef> statusStore;
    private StatusEngine statuses;
    private ElementMatrix elements;
    private LimboManager limbo;
    private AdminTools admin;
    private GlobalEvents events;
    private ItemListener itemListener;
    private DefinitionStore<TriggerDef> triggerStore;
    private TriggerEngine triggers;
    private SpecialAbilities specials;
    private CinematicPlayer cinematics;
    private DefinitionStore<CinematicDef> cinematicStore;
    private PlayerDataStore players;
    private DamagePipeline damage;
    private AbilityEngine abilities;
    private VisualManager visuals;
    private SummonManager summons;
    private BossManager bosses;
    private ChatPrompt prompts;
    private Kinds kinds;
    private ModuleManager modules;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Keys.init(this);
        AsyncIO.start();
        messages = new Messages(this);
        elements = new ElementMatrix(this);

        abilityStore = new DefinitionStore<>(this, "abilities", AbilityDef.CODEC);
        summonStore = new DefinitionStore<>(this, "summons", SummonDef.CODEC);
        bossStore = new DefinitionStore<>(this, "bosses", BossDef.CODEC);
        itemStore = new DefinitionStore<>(this, "items", ItemDef.CODEC);
        statusStore = new DefinitionStore<>(this, "statuses", StatusDef.CODEC);
        cinematicStore = new DefinitionStore<>(this, "cinematics", CinematicDef.CODEC);
        triggerStore = new DefinitionStore<>(this, "triggers", TriggerDef.CODEC);
        loadStores();

        players = new PlayerDataStore(this);
        prompts = new ChatPrompt(this);
        damage = new DamagePipeline(this);
        abilities = new AbilityEngine(this, damage);
        specials = new SpecialAbilities(this);
        cinematics = new CinematicPlayer(this);
        statuses = new StatusEngine(this);
        limbo = new LimboManager(this);
        admin = new AdminTools(this);
        events = new GlobalEvents(this);
        triggers = new TriggerEngine(this);
        visuals = new VisualManager(this);
        summons = new SummonManager(this);
        bosses = new BossManager(this);
        kinds = new Kinds(this);

        Bukkit.getPluginManager().registerEvents(new MenuManager(this), this);
        Bukkit.getPluginManager().registerEvents(prompts, this);
        Bukkit.getPluginManager().registerEvents(players, this);

        modules = new ModuleManager(this);
        modules.register("model", () -> {
            if (Bukkit.getPluginManager().getPlugin("BetterModel") == null) {
                getLogger().warning("BetterModel no está instalado: las entidades usarán el modo sin modelo.");
                return;
            }
            try {
                model = new BetterModelAdapter();
                getLogger().info("BetterModel detectado: " + model.modelKeys().size() + " modelos.");
            } catch (Throwable t) {
                model = new NullModelAdapter();
                getLogger().warning("No se pudo enlazar con BetterModel (" + t + "): modo sin modelo.");
            }
        }, () -> model = new NullModelAdapter());
        modules.register("dimension", () -> limbo.start(), () -> limbo.stop());
        modules.register("events", () -> events.start(), () -> events.stop());
        modules.register("admin", () -> admin.start(), () -> admin.stop());
        modules.register("visual", () -> visuals.start(), () -> visuals.stop());
        modules.register("status", () -> {
            statuses.start();
            StatusPresets.install(statusStore);
        }, () -> statuses.stop());
        modules.register("ability", () -> {
            abilities.start();
            AbilityPresets.install(abilityStore);
        }, () -> abilities.stop());
        modules.register("summon", () -> summons.start(), () -> summons.stop());
        modules.register("boss", () -> bosses.start(), () -> bosses.stop());
        modules.register("item", () -> {
            itemListener = new ItemListener(this);
            itemListener.start();
        }, () -> {
            if (itemListener != null) itemListener.stop();
        });
        modules.register("trigger", () -> triggers.start(), () -> triggers.stop());
        modules.enableAll();

        PluginCommand cmd = getCommand("invocaciones");
        if (cmd != null) {
            InvCommand ic = new InvCommand(this);
            cmd.setExecutor(ic);
            cmd.setTabCompleter(ic);
        }
    }

    @Override
    public void onDisable() {
        if (modules != null) modules.disableAll();
        if (players != null) players.saveAll();
        AsyncIO.shutdown();
    }

    private void loadStores() {
        abilityStore.load();
        summonStore.load();
        bossStore.load();
        itemStore.load();
        statusStore.load();
        cinematicStore.load();
        triggerStore.load();
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        elements.load();
        loadStores();
    }

    public static SummonForge get() { return instance; }
    public Difficulty difficulty() { return Difficulty.of(getConfig().getString("difficulty", "NORMAL")); }
    public Messages messages() { return messages; }
    public ModelAdapter model() { return model; }
    public DefinitionStore<AbilityDef> abilityStore() { return abilityStore; }
    public DefinitionStore<SummonDef> summonStore() { return summonStore; }
    public DefinitionStore<BossDef> bossStore() { return bossStore; }
    public DefinitionStore<ItemDef> itemStore() { return itemStore; }
    public DefinitionStore<StatusDef> statusStore() { return statusStore; }
    public StatusEngine statuses() { return statuses; }
    public ElementMatrix elements() { return elements; }
    public LimboManager limbo() { return limbo; }
    public AdminTools admin() { return admin; }
    public SpecialAbilities specials() { return specials; }
    public CinematicPlayer cinematics() { return cinematics; }
    public DefinitionStore<TriggerDef> triggerStore() { return triggerStore; }
    public TriggerEngine triggers() { return triggers; }
    public DefinitionStore<CinematicDef> cinematicStore() { return cinematicStore; }
    public PlayerDataStore players() { return players; }
    public DamagePipeline damage() { return damage; }
    public AbilityEngine abilities() { return abilities; }
    public VisualManager visuals() { return visuals; }
    public SummonManager summons() { return summons; }
    public BossManager bosses() { return bosses; }
    public ChatPrompt prompts() { return prompts; }
    public Kinds kinds() { return kinds; }
}
