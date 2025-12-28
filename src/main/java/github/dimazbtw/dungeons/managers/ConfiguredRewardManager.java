package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.ConfiguredReward;
import github.dimazbtw.dungeons.models.PendingReward;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ConfiguredRewardManager {

    private final Main plugin;
    private final Map<String, ConfiguredReward> rewards;
    private FileConfiguration config;

    public ConfiguredRewardManager(Main plugin) {
        this.plugin = plugin;
        this.rewards = new HashMap<>();
        loadRewards();
    }

    private void loadRewards() {
        File file = new File(plugin.getDataFolder(), "rewards.yml");
        if (!file.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        rewards.clear();

        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection == null) return;

        for (String id : rewardsSection.getKeys(false)) {
            ConfigurationSection section = rewardsSection.getConfigurationSection(id);
            if (section == null) continue;

            try {
                ConfiguredReward reward = loadReward(id, section);
                if (reward != null) {
                    rewards.put(id, reward);
                    plugin.getLogger().info("Loaded reward: " + id);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load reward: " + id + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + rewards.size() + " configured rewards");
    }

    private ConfiguredReward loadReward(String id, ConfigurationSection section) {
        String typeStr = section.getString("type", "ITEM").toUpperCase();
        ConfiguredReward.RewardType type;
        
        try {
            type = ConfiguredReward.RewardType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = ConfiguredReward.RewardType.ITEM;
        }

        String displayName = section.getString("display-name", id);
        String description = section.getString("description", "");
        int amount = section.getInt("amount", 1);
        
        Material material = Material.CHEST;
        String materialStr = section.getString("material");
        if (materialStr != null) {
            try {
                material = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String command = section.getString("command", "");

        // Criar item customizado se necessário
        ItemStack customItem = null;
        if (type == ConfiguredReward.RewardType.ITEM && material != Material.CHEST) {
            customItem = new ItemStack(material, amount);
            ItemMeta meta = customItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize(displayName));
                if (!description.isEmpty()) {
                    meta.setLore(Collections.singletonList(ColorUtils.colorize(description)));
                }
                customItem.setItemMeta(meta);
            }
        }

        return new ConfiguredReward.Builder()
                .id(id)
                .type(type)
                .displayName(displayName)
                .description(description)
                .amount(amount)
                .material(material)
                .command(command)
                .customItem(customItem)
                .build();
    }

    public void reload() {
        loadRewards();
    }

    public ConfiguredReward getReward(String id) {
        return rewards.get(id);
    }

    public Collection<ConfiguredReward> getAllRewards() {
        return rewards.values();
    }

    /**
     * Processa uma recompensa por ID com chance
     * @return true se a recompensa foi dada
     */
    public boolean processReward(Player player, String rewardId, double chance, String dungeonId) {
        ConfiguredReward reward = rewards.get(rewardId);
        if (reward == null) {
            plugin.getLogger().warning("Reward not found: " + rewardId);
            return false;
        }

        // Verificar chance
        if (Math.random() * 100 > chance) {
            return false;
        }

        // Criar PendingReward baseado no ConfiguredReward
        PendingReward pending = createPendingReward(reward, dungeonId);
        plugin.getRewardManager().addPendingReward(player.getUniqueId(), pending);

        return true;
    }

    /**
     * Cria um PendingReward a partir de um ConfiguredReward
     */
    private PendingReward createPendingReward(ConfiguredReward reward, String dungeonId) {
        return switch (reward.getType()) {
            case ITEM -> new PendingReward(
                    PendingReward.RewardType.ITEM,
                    dungeonId,
                    reward.createItemStack()
            );
            case POINTS -> new PendingReward(
                    PendingReward.RewardType.POINTS,
                    dungeonId,
                    reward.getAmount()
            );
            case EXPERIENCE -> {
                PendingReward pending = new PendingReward(
                        PendingReward.RewardType.EXPERIENCE,
                        dungeonId,
                        0
                );
                pending.setExperience(reward.getAmount());
                yield pending;
            }
            case COMMAND -> {
                PendingReward pending = new PendingReward(
                        PendingReward.RewardType.COMMAND,
                        dungeonId,
                        reward.getCommand()
                );
                // Guardar informações extras para display no menu
                pending.setDisplayName(reward.getDisplayName());
                pending.setDisplayMaterial(reward.getMaterial());
                pending.setDescription(reward.getDescription());
                yield pending;
            }
        };
    }

    /**
     * Dá uma recompensa diretamente (sem ser pendente)
     */
    public void giveRewardDirectly(Player player, String rewardId) {
        ConfiguredReward reward = rewards.get(rewardId);
        if (reward == null) return;

        switch (reward.getType()) {
            case ITEM -> {
                ItemStack item = reward.createItemStack();
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(item);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            case POINTS -> {
                var data = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (data != null) {
                    data.addPoints(reward.getAmount());
                }
            }
            case EXPERIENCE -> {
                var data = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (data != null) {
                    data.addExperience(reward.getAmount());
                }
            }
            case COMMAND -> {
                String cmd = reward.getCommand().replace("{player}", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }
        }
    }
}
