package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Main plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final File dataFolder;

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, this::loadPlayerData);
    }

    public PlayerData getOrCreate(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, id -> {
            PlayerData data = loadPlayerData(id);
            return data != null ? data : new PlayerData(id);
        });
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return new PlayerData(uuid);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid);

        data.setDungeonsCompleted(config.getInt("dungeons-completed", 0));
        data.setDungeonsFailed(config.getInt("dungeons-failed", 0));
        data.setMobsKilled(config.getInt("mobs-killed", 0));
        data.setBossesKilled(config.getInt("bosses-killed", 0));
        data.setDeaths(config.getInt("deaths", 0));
        data.setTotalPlayTime(config.getLong("total-playtime", 0));
        data.setHighestRound(config.getInt("highest-round", 0));
        data.setPoints(config.getInt("points", 0));

        // Stats/Perks
        data.setVitality(config.getInt("stats.vitality", 0));
        data.setStrength(config.getInt("stats.strength", 0));
        data.setAgility(config.getDouble("stats.agility", 0));
        data.setPerkPoints(config.getInt("stats.perk-points", 0));
        data.setLevel(config.getInt("stats.level", 1));
        data.setExperience(config.getInt("stats.experience", 0));

        // Load enchant levels
        if (config.isConfigurationSection("enchants")) {
            for (String key : config.getConfigurationSection("enchants").getKeys(false)) {
                data.setEnchantLevel(key, config.getInt("enchants." + key, 0));
            }
        }

        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("dungeons-completed", data.getDungeonsCompleted());
        config.set("dungeons-failed", data.getDungeonsFailed());
        config.set("mobs-killed", data.getMobsKilled());
        config.set("bosses-killed", data.getBossesKilled());
        config.set("deaths", data.getDeaths());
        config.set("total-playtime", data.getTotalPlayTime());
        config.set("highest-round", data.getHighestRound());
        config.set("points", data.getPoints());

        // Stats/Perks
        config.set("stats.vitality", data.getVitality());
        config.set("stats.strength", data.getStrength());
        config.set("stats.agility", data.getAgility());
        config.set("stats.perk-points", data.getPerkPoints());
        config.set("stats.level", data.getLevel());
        config.set("stats.experience", data.getExperience());

        // Save enchant levels
        for (Map.Entry<String, Integer> entry : data.getWeaponEnchants().entrySet()) {
            config.set("enchants." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid);
            e.printStackTrace();
        }
    }

    public void saveAll() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerData(uuid);
        playerDataCache.remove(uuid);
    }

    public void loadPlayer(UUID uuid) {
        playerDataCache.put(uuid, loadPlayerData(uuid));
    }

    /**
     * Retorna todos os dados de jogadores (carrega do disco se necessário)
     * Usado para rankings
     */
    public Map<UUID, PlayerData> getAllData() {
        // Carregar todos os arquivos do disco que não estão no cache
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".yml", "");
                try {
                    UUID uuid = UUID.fromString(name);
                    if (!playerDataCache.containsKey(uuid)) {
                        PlayerData data = loadPlayerData(uuid);
                        if (data != null) {
                            playerDataCache.put(uuid, data);
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // Nome de arquivo inválido
                }
            }
        }
        return new ConcurrentHashMap<>(playerDataCache);
    }

    /**
     * Retorna o número total de jogadores com dados salvos
     */
    public int getTotalPlayers() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null ? files.length : 0;
    }
}
