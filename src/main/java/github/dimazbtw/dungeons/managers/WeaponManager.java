package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class WeaponManager {

    private final Main plugin;
    private final NamespacedKey weaponKey;
    private final Map<String, EnchantConfig> enchants;

    private Material weaponMaterial;
    private String weaponName;
    private List<String> weaponLore;

    public WeaponManager(Main plugin) {
        this.plugin = plugin;
        this.weaponKey = new NamespacedKey(plugin, "dungeon_weapon");
        this.enchants = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        enchants.clear();
        FileConfiguration config = plugin.getWeaponConfig();

        ConfigurationSection weaponSection = config.getConfigurationSection("weapon");
        if (weaponSection != null) {
            String materialStr = weaponSection.getString("material", "DIAMOND_SWORD");
            try {
                weaponMaterial = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                weaponMaterial = Material.DIAMOND_SWORD;
            }

            weaponName = ColorUtils.colorize(weaponSection.getString("name", "&c&lDungeon Weapon"));
            weaponLore = weaponSection.getStringList("lore").stream()
                    .map(ColorUtils::colorize)
                    .toList();
        }

        ConfigurationSection enchantsSection = config.getConfigurationSection("enchants");
        if (enchantsSection != null) {
            for (String enchantId : enchantsSection.getKeys(false)) {
                ConfigurationSection enchantSection = enchantsSection.getConfigurationSection(enchantId);
                if (enchantSection != null) {
                    EnchantConfig enchant = new EnchantConfig(enchantId);
                    enchant.setEnabled(enchantSection.getBoolean("enabled", true));
                    enchant.setName(enchantSection.getString("name", enchantId));
                    enchant.setDescription(enchantSection.getString("description", ""));

                    ConfigurationSection levelsSection = enchantSection.getConfigurationSection("levels");
                    if (levelsSection != null) {
                        for (String levelKey : levelsSection.getKeys(false)) {
                            try {
                                int level = Integer.parseInt(levelKey);
                                ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                                if (levelSection != null) {
                                    EnchantLevel enchantLevel = new EnchantLevel(level);
                                    enchantLevel.setPointsCost(levelSection.getInt("price.points", 1));
                                    enchantLevel.setValue(levelSection.getInt("value", 0));
                                    enchantLevel.setChance(levelSection.getDouble("chance", 0));
                                    enchantLevel.setRadius(levelSection.getDouble("radius", 0));
                                    enchantLevel.setKnockback(levelSection.getDouble("knockback", 0));
                                    enchant.addLevel(level, enchantLevel);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    enchants.put(enchantId, enchant);
                }
            }
        }
        plugin.getLogger().info("Loaded " + enchants.size() + " weapon enchants!");
    }

    public void reload() {
        loadConfig();
    }

    public void giveWeapon(Player player) {
        ItemStack weapon = createWeapon(player);
        player.getInventory().setItem(0, weapon);
    }

    public ItemStack createWeapon(Player player) {
        ItemStack weapon = new ItemStack(weaponMaterial);
        ItemMeta meta = weapon.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(weaponName);

            PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
            List<String> lore = new ArrayList<>();

            for (String loreLine : weaponLore) {
                String processed = loreLine;
                for (EnchantConfig enchant : enchants.values()) {
                    int level = data != null ? data.getEnchantLevel(enchant.getId()) : 0;
                    String levelStr = level > 0 ? String.valueOf(level) : "0";
                    processed = processed.replace("{" + enchant.getId() + "_level}", levelStr);
                }
                lore.add(processed);
            }

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.BYTE, (byte) 1);
            weapon.setItemMeta(meta);
        }

        return weapon;
    }

    public boolean isDungeonWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(weaponKey, PersistentDataType.BYTE);
    }

    public EnchantConfig getEnchant(String id) {
        return enchants.get(id);
    }

    public Collection<EnchantConfig> getAllEnchants() {
        return enchants.values();
    }

    public boolean upgradeEnchant(Player player, String enchantId) {
        EnchantConfig enchant = enchants.get(enchantId);
        if (enchant == null || !enchant.isEnabled()) return false;

        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return false;

        int currentLevel = data.getEnchantLevel(enchantId);
        int nextLevel = currentLevel + 1;

        EnchantLevel nextLevelConfig = enchant.getLevel(nextLevel);
        if (nextLevelConfig == null) return false;

        if (data.getPoints() < nextLevelConfig.getPointsCost()) return false;

        data.removePoints(nextLevelConfig.getPointsCost());
        data.setEnchantLevel(enchantId, nextLevel);

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (isDungeonWeapon(inHand)) {
            player.getInventory().setItemInMainHand(createWeapon(player));
        }

        return true;
    }

    public double getSmiteDamageBonus(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return 0;

        int level = data.getEnchantLevel("smite");
        if (level == 0) return 0;

        EnchantConfig enchant = enchants.get("smite");
        if (enchant == null) return 0;

        EnchantLevel levelConfig = enchant.getLevel(level);
        return levelConfig != null ? levelConfig.getValue() : 0;
    }

    public double getWisdomBonus(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return 0;

        int level = data.getEnchantLevel("wisdom");
        if (level == 0) return 0;

        EnchantConfig enchant = enchants.get("wisdom");
        if (enchant == null) return 0;

        EnchantLevel levelConfig = enchant.getLevel(level);
        return levelConfig != null ? levelConfig.getValue() : 0;
    }

    public boolean shouldTriggerLifesteal(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return false;

        int level = data.getEnchantLevel("lifesteal");
        if (level == 0) return false;

        EnchantConfig enchant = enchants.get("lifesteal");
        if (enchant == null) return false;

        EnchantLevel levelConfig = enchant.getLevel(level);
        if (levelConfig == null) return false;

        return Math.random() * 100 < levelConfig.getChance();
    }

    public double getLifestealAmount(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return 0;

        int level = data.getEnchantLevel("lifesteal");
        if (level == 0) return 0;

        EnchantConfig enchant = enchants.get("lifesteal");
        if (enchant == null) return 0;

        EnchantLevel levelConfig = enchant.getLevel(level);
        return levelConfig != null ? levelConfig.getValue() * 2 : 0;
    }

    public boolean shouldTriggerCrit(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return false;

        int level = data.getEnchantLevel("crit");
        if (level == 0) return false;

        EnchantConfig enchant = enchants.get("crit");
        if (enchant == null) return false;

        EnchantLevel levelConfig = enchant.getLevel(level);
        if (levelConfig == null) return false;

        return Math.random() * 100 < levelConfig.getChance();
    }

    public boolean shouldTriggerThor(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return false;

        int level = data.getEnchantLevel("thor");
        if (level == 0) return false;

        EnchantConfig enchant = enchants.get("thor");
        if (enchant == null) return false;

        EnchantLevel levelConfig = enchant.getLevel(level);
        if (levelConfig == null) return false;

        return Math.random() * 100 < levelConfig.getChance();
    }

    public double getThorRadius(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return 0;

        int level = data.getEnchantLevel("thor");
        if (level == 0) return 0;

        EnchantConfig enchant = enchants.get("thor");
        if (enchant == null) return 0;

        EnchantLevel levelConfig = enchant.getLevel(level);
        return levelConfig != null ? levelConfig.getRadius() : 0;
    }

    public boolean shouldTriggerShockwave(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return false;

        int level = data.getEnchantLevel("shockwave");
        if (level == 0) return false;

        EnchantConfig enchant = enchants.get("shockwave");
        if (enchant == null) return false;

        EnchantLevel levelConfig = enchant.getLevel(level);
        if (levelConfig == null) return false;

        return Math.random() * 100 < levelConfig.getChance();
    }

    public double getShockwaveRadius(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return 0;

        int level = data.getEnchantLevel("shockwave");
        if (level == 0) return 0;

        EnchantConfig enchant = enchants.get("shockwave");
        if (enchant == null) return 0;

        EnchantLevel levelConfig = enchant.getLevel(level);
        return levelConfig != null ? levelConfig.getRadius() : 0;
    }

    public double getShockwaveKnockback(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return 0;

        int level = data.getEnchantLevel("shockwave");
        if (level == 0) return 0;

        EnchantConfig enchant = enchants.get("shockwave");
        if (enchant == null) return 0;

        EnchantLevel levelConfig = enchant.getLevel(level);
        return levelConfig != null ? levelConfig.getKnockback() : 0;
    }

    public static class EnchantConfig {
        private final String id;
        private boolean enabled;
        private String name;
        private String description;
        private final Map<Integer, EnchantLevel> levels = new HashMap<>();

        public EnchantConfig(String id) { this.id = id; }
        public String getId() { return id; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<Integer, EnchantLevel> getLevels() { return levels; }
        public void addLevel(int level, EnchantLevel config) { levels.put(level, config); }
        public EnchantLevel getLevel(int level) { return levels.get(level); }
        public int getMaxLevel() { return levels.keySet().stream().max(Integer::compare).orElse(0); }
    }

    public static class EnchantLevel {
        private final int level;
        private int pointsCost;
        private int value;
        private double chance;
        private double radius;
        private double knockback;

        public EnchantLevel(int level) { this.level = level; }
        public int getLevel() { return level; }
        public int getPointsCost() { return pointsCost; }
        public void setPointsCost(int pointsCost) { this.pointsCost = pointsCost; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public double getChance() { return chance; }
        public void setChance(double chance) { this.chance = chance; }
        public double getRadius() { return radius; }
        public void setRadius(double radius) { this.radius = radius; }
        public double getKnockback() { return knockback; }
        public void setKnockback(double knockback) { this.knockback = knockback; }
    }
}
