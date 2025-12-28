package github.dimazbtw.dungeons;

import github.dimazbtw.dungeons.commands.DungeonCommand;
import github.dimazbtw.dungeons.hooks.PlaceholderHook;
import github.dimazbtw.dungeons.listeners.DungeonListener;
import github.dimazbtw.dungeons.listeners.MobListener;
import github.dimazbtw.dungeons.listeners.PlayerListener;
import github.dimazbtw.dungeons.listeners.WeaponListener;
import github.dimazbtw.dungeons.managers.*;
import github.dimazbtw.lib.inventories.InventoryListener;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    private static Main instance;

    private MessageManager messageManager;
    private DungeonManager dungeonManager;
    private MobManager mobManager;
    private BossManager bossManager;
    private SessionManager sessionManager;
    private WeaponManager weaponManager;
    private PlayerDataManager playerDataManager;
    private MenuManager menuManager;
    private ScoreboardManager scoreboardManager;
    private BossBarManager bossBarManager;
    private RewardManager rewardManager;
    private EquipmentManager equipmentManager;
    private EffectsManager effectsManager;
    private ConfiguredRewardManager configuredRewardManager;
    private RankingManager rankingManager;

    private FileConfiguration dungeonsConfig;
    private FileConfiguration mobsConfig;
    private FileConfiguration bossesConfig;
    private FileConfiguration weaponConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfigs();

        // Load configurations (except locations)
        loadConfigurations();

        // Initialize managers that don't need worlds
        initializeEarlyManagers();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Schedule location loading after worlds are loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Initialize managers that need worlds
            initializeLateManagers();

            // Register PlaceholderAPI
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new PlaceholderHook(this).register();
                getLogger().info("PlaceholderAPI hook registrado!");
            }

            getLogger().info("Dungeons carregado com sucesso!");
        }, 1L); // 1 tick delay to ensure worlds are loaded

        getLogger().info("Dungeons iniciando...");
    }

    @Override
    public void onDisable() {
        // End all active sessions
        if (sessionManager != null) {
            sessionManager.endAllSessions();
        }

        // Remove all boss bars
        if (bossBarManager != null) {
            bossBarManager.removeAllBossBars();
        }

        // Save player data
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        // Save equipment data
        if (equipmentManager != null) {
            equipmentManager.saveAll();
        }

        getLogger().info("Dungeons desabilitado!");
    }

    private void saveDefaultConfigs() {
        saveDefaultConfig();
        saveResource("dungeons.yml", false);
        saveResource("mobs.yml", false);
        saveResource("bosses.yml", false);
        saveResource("weapon.yml", false);
        saveResource("messages.yml", false);
        saveResource("menus/main.yml", false);
        saveResource("menus/dungeons.yml", false);
    }

    private void loadConfigurations() {
        reloadConfig();
        dungeonsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "dungeons.yml"));
        mobsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "mobs.yml"));
        bossesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "bosses.yml"));
        weaponConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "weapon.yml"));
    }

    private void initializeEarlyManagers() {
        // Managers that don't need worlds loaded
        this.messageManager = new MessageManager(this);
        this.mobManager = new MobManager(this);
        this.bossManager = new BossManager(this);
        this.weaponManager = new WeaponManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.menuManager = new MenuManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.rewardManager = new RewardManager(this);
        this.equipmentManager = new EquipmentManager(this);
        this.effectsManager = new EffectsManager(this);
        this.configuredRewardManager = new ConfiguredRewardManager(this);
        this.rankingManager = new RankingManager(this);
    }

    private void initializeLateManagers() {
        // Managers that need worlds loaded (locations)
        this.dungeonManager = new DungeonManager(this);
        this.sessionManager = new SessionManager(this);

        getLogger().info("Carregadas " + dungeonManager.getDungeonCount() + " dungeons!");
    }

    private void registerCommands() {
        BukkitFrame frame = new BukkitFrame(this);
        frame.registerCommands(new DungeonCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getServer().getPluginManager().registerEvents(new github.dimazbtw.dungeons.listeners.EquipmentMenuListener(this), this);
    }

    public void reload() {
        loadConfigurations();
        messageManager.reload();
        mobManager.reload();
        bossManager.reload();
        dungeonManager.reload();
        weaponManager.reload();
        menuManager.reload();
        scoreboardManager.reload();
    }

    // Getters
    public static Main getInstance() {
        return instance;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public WeaponManager getWeaponManager() {
        return weaponManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public EquipmentManager getEquipmentManager() {
        return equipmentManager;
    }

    public EffectsManager getEffectsManager() {
        return effectsManager;
    }

    public ConfiguredRewardManager getConfiguredRewardManager() {
        return configuredRewardManager;
    }

    public RankingManager getRankingManager() {
        return rankingManager;
    }

    public FileConfiguration getDungeonsConfig() {
        return dungeonsConfig;
    }

    public FileConfiguration getMobsConfig() {
        return mobsConfig;
    }

    public FileConfiguration getBossesConfig() {
        return bossesConfig;
    }

    public FileConfiguration getWeaponConfig() {
        return weaponConfig;
    }
}
