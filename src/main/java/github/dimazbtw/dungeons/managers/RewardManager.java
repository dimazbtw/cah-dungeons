package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.dungeons.models.PendingReward;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RewardManager {

    private final Main plugin;
    private final Map<UUID, List<PendingReward>> pendingRewards;
    private final File rewardsFile;

    public RewardManager(Main plugin) {
        this.plugin = plugin;
        this.pendingRewards = new ConcurrentHashMap<>();
        this.rewardsFile = new File(plugin.getDataFolder(), "pending_rewards.yml");
        loadPendingRewards();
    }

    /**
     * Processa recompensas ao completar dungeon
     */
    public void processDungeonRewards(Player player, DungeonSession session) {
        String dungeonId = session.getDungeon().getId();
        List<PendingReward> rewards = new ArrayList<>();

        // Pontos base por completar
        int basePoints = plugin.getConfig().getInt("rewards.base-points", 100);
        int roundBonus = session.getCurrentRound() * plugin.getConfig().getInt("rewards.points-per-round", 10);
        int totalPoints = basePoints + roundBonus;
        
        rewards.add(PendingReward.createPointsReward(dungeonId, totalPoints));

        // Experiência base
        int baseXp = plugin.getConfig().getInt("rewards.base-experience", 50);
        int xpBonus = session.getMobsKilled() * plugin.getConfig().getInt("rewards.xp-per-mob", 5);
        int totalXp = baseXp + xpBonus;
        
        rewards.add(PendingReward.createExperienceReward(dungeonId, totalXp));

        // Processar recompensas configuradas da dungeon
        ConfigurationSection dungeonRewards = plugin.getDungeonsConfig()
                .getConfigurationSection("dungeons." + dungeonId + ".rewards");
        
        if (dungeonRewards != null) {
            // Comandos
            for (String cmd : dungeonRewards.getStringList("commands")) {
                if (shouldGiveReward(cmd)) {
                    String cleanCmd = getCleanCommand(cmd);
                    rewards.add(PendingReward.createCommandReward(dungeonId, cleanCmd));
                }
            }

            // Itens
            ConfigurationSection itemsSection = dungeonRewards.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection != null && shouldGiveReward(itemSection.getDouble("chance", 100))) {
                        ItemStack item = createRewardItem(itemSection);
                        if (item != null) {
                            rewards.add(PendingReward.createItemReward(dungeonId, item));
                        }
                    }
                }
            }
        }

        // Adicionar às recompensas pendentes
        addPendingRewards(player.getUniqueId(), rewards);

        // Notificar jogador
        if (!rewards.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(rewards.size()));
            plugin.getMessageManager().sendMessage(player, "rewards-pending", placeholders);
        }
    }

    /**
     * Processa drops de mob/boss (vão direto para pendentes)
     */
    public void processMobDrops(Player player, String dungeonId, ConfigurationSection dropsSection) {
        if (dropsSection == null) return;

        List<PendingReward> rewards = new ArrayList<>();

        for (String dropKey : dropsSection.getKeys(false)) {
            ConfigurationSection dropSection = dropsSection.getConfigurationSection(dropKey);
            if (dropSection == null) continue;

            double chance = dropSection.getDouble("chance", 100);
            if (!shouldGiveReward(chance)) continue;

            ItemStack item = createRewardItem(dropSection);
            if (item != null) {
                rewards.add(PendingReward.createItemReward(dungeonId, item));
            }
        }

        if (!rewards.isEmpty()) {
            addPendingRewards(player.getUniqueId(), rewards);
        }
    }

    /**
     * Cria item de recompensa a partir da config
     */
    private ItemStack createRewardItem(ConfigurationSection section) {
        String materialStr = section.getString("material");
        if (materialStr == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Quantidade (pode ser range "1-5")
        int amount = 1;
        String amountStr = section.getString("amount", "1");
        if (amountStr.contains("-")) {
            String[] parts = amountStr.split("-");
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            amount = min + (int) (Math.random() * (max - min + 1));
        } else {
            amount = Integer.parseInt(amountStr);
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (section.contains("name")) {
                meta.setDisplayName(ColorUtils.colorize(section.getString("name")));
            }

            if (section.contains("lore")) {
                List<String> lore = section.getStringList("lore").stream()
                        .map(ColorUtils::colorize)
                        .toList();
                meta.setLore(lore);
            }

            // Encantamentos
            ConfigurationSection enchantsSection = section.getConfigurationSection("enchants");
            if (enchantsSection != null) {
                for (String enchantKey : enchantsSection.getKeys(false)) {
                    try {
                        Enchantment enchant = Enchantment.getByName(enchantKey.toUpperCase());
                        if (enchant != null) {
                            int level = enchantsSection.getInt(enchantKey, 1);
                            meta.addEnchant(enchant, level, true);
                        }
                    } catch (Exception ignored) {}
                }
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Verifica chance de drop
     */
    private boolean shouldGiveReward(String commandWithChance) {
        if (!commandWithChance.contains(";")) return true;
        String[] parts = commandWithChance.split(";");
        if (parts.length < 2) return true;
        try {
            double chance = Double.parseDouble(parts[1]);
            return Math.random() * 100 < chance;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean shouldGiveReward(double chance) {
        return Math.random() * 100 < chance;
    }

    private String getCleanCommand(String commandWithChance) {
        if (!commandWithChance.contains(";")) return commandWithChance;
        return commandWithChance.split(";")[0];
    }

    /**
     * Adiciona recompensas pendentes
     */
    public void addPendingRewards(UUID playerId, List<PendingReward> rewards) {
        pendingRewards.computeIfAbsent(playerId, k -> new ArrayList<>()).addAll(rewards);
        savePendingRewards();
    }

    public void addPendingReward(UUID playerId, PendingReward reward) {
        pendingRewards.computeIfAbsent(playerId, k -> new ArrayList<>()).add(reward);
        savePendingRewards();
    }

    /**
     * Obtém recompensas pendentes de um jogador
     */
    public List<PendingReward> getPendingRewards(UUID playerId) {
        return pendingRewards.getOrDefault(playerId, new ArrayList<>());
    }

    /**
     * Coleta uma recompensa específica
     */
    public boolean claimReward(Player player, String rewardId) {
        List<PendingReward> rewards = pendingRewards.get(player.getUniqueId());
        if (rewards == null) return false;

        PendingReward reward = rewards.stream()
                .filter(r -> r.getId().equals(rewardId))
                .findFirst()
                .orElse(null);

        if (reward == null) return false;

        boolean claimed = processReward(player, reward);
        if (claimed) {
            rewards.remove(reward);
            if (rewards.isEmpty()) {
                pendingRewards.remove(player.getUniqueId());
            }
            savePendingRewards();
        }

        return claimed;
    }

    /**
     * Coleta todas as recompensas
     */
    public int claimAllRewards(Player player) {
        List<PendingReward> rewards = pendingRewards.get(player.getUniqueId());
        if (rewards == null || rewards.isEmpty()) return 0;

        int claimed = 0;
        List<PendingReward> toClaim = new ArrayList<>(rewards);

        for (PendingReward reward : toClaim) {
            if (processReward(player, reward)) {
                rewards.remove(reward);
                claimed++;
            }
        }

        if (rewards.isEmpty()) {
            pendingRewards.remove(player.getUniqueId());
        }
        savePendingRewards();

        return claimed;
    }

    /**
     * Processa uma recompensa individual
     */
    private boolean processReward(Player player, PendingReward reward) {
        switch (reward.getType()) {
            case COMMAND -> {
                String cmd = reward.getCommand().replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                return true;
            }
            case ITEM -> {
                ItemStack item = reward.getItem();
                if (player.getInventory().firstEmpty() == -1) {
                    // Inventário cheio
                    plugin.getMessageManager().sendMessage(player, "inventory-full");
                    return false;
                }
                player.getInventory().addItem(item);
                return true;
            }
            case EXPERIENCE -> {
                PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (data != null) {
                    // Aplica bônus de Wisdom
                    double wisdomBonus = plugin.getWeaponManager().getWisdomBonus(player);
                    int finalXp = (int) (reward.getExperience() * (1 + wisdomBonus / 100));
                    
                    int oldLevel = data.getLevel();
                    int levelsGained = data.addExperience(finalXp);
                    
                    // Notifica level up
                    if (levelsGained > 0) {
                        player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() + 
                                "&a&l✦ LEVEL UP! &eVocê agora é nível &a" + data.getLevel() + "&e!"));
                        player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() + 
                                "&7Você ganhou &6" + levelsGained + " ponto(s) de atributo&7!"));
                        plugin.getMessageManager().playSound(player, "level-up");
                    }
                }
                return true;
            }
            case POINTS -> {
                PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
                if (data != null) {
                    data.addPoints(reward.getPoints());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Salva recompensas pendentes em arquivo
     */
    private void savePendingRewards() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, List<PendingReward>> entry : pendingRewards.entrySet()) {
            String uuidStr = entry.getKey().toString();
            List<PendingReward> rewards = entry.getValue();

            for (int i = 0; i < rewards.size(); i++) {
                PendingReward reward = rewards.get(i);
                String path = uuidStr + "." + i;

                config.set(path + ".id", reward.getId());
                config.set(path + ".type", reward.getType().name());
                config.set(path + ".dungeon", reward.getDungeonId());
                config.set(path + ".timestamp", reward.getTimestamp());

                switch (reward.getType()) {
                    case COMMAND -> config.set(path + ".command", reward.getCommand());
                    case ITEM -> config.set(path + ".item", reward.getItem());
                    case EXPERIENCE -> config.set(path + ".experience", reward.getExperience());
                    case POINTS -> config.set(path + ".points", reward.getPoints());
                }
            }
        }

        try {
            config.save(rewardsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pending rewards: " + e.getMessage());
        }
    }

    /**
     * Carrega recompensas pendentes do arquivo
     */
    private void loadPendingRewards() {
        if (!rewardsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);

        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = config.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;

                List<PendingReward> rewards = new ArrayList<>();

                for (String indexStr : playerSection.getKeys(false)) {
                    ConfigurationSection rewardSection = playerSection.getConfigurationSection(indexStr);
                    if (rewardSection == null) continue;

                    String dungeonId = rewardSection.getString("dungeon", "unknown");
                    PendingReward.RewardType type = PendingReward.RewardType.valueOf(
                            rewardSection.getString("type", "POINTS"));

                    PendingReward reward = switch (type) {
                        case COMMAND -> PendingReward.createCommandReward(dungeonId, 
                                rewardSection.getString("command", ""));
                        case ITEM -> PendingReward.createItemReward(dungeonId, 
                                rewardSection.getItemStack("item"));
                        case EXPERIENCE -> PendingReward.createExperienceReward(dungeonId, 
                                rewardSection.getInt("experience", 0));
                        case POINTS -> PendingReward.createPointsReward(dungeonId, 
                                rewardSection.getInt("points", 0));
                    };

                    rewards.add(reward);
                }

                if (!rewards.isEmpty()) {
                    pendingRewards.put(playerId, rewards);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load rewards for " + uuidStr + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded pending rewards for " + pendingRewards.size() + " players!");
    }

    public int getPendingRewardCount(UUID playerId) {
        return pendingRewards.getOrDefault(playerId, Collections.emptyList()).size();
    }
}
