package github.dimazbtw.dungeons.models;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DungeonMob {

    private final String id;
    private EntityType entityType;
    private String displayName;
    private boolean baby;
    private double range;

    // Attributes
    private double speed;
    private double damage;
    private double health;
    private double knockbackResistance;

    // Equipment
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;

    // Rewards (novo sistema - lista de MobReward)
    private List<MobReward> configuredRewards;

    // Drops
    private Map<String, MobDrop> drops;

    public DungeonMob(String id) {
        this.id = id;
        this.configuredRewards = new ArrayList<>();
        this.drops = new HashMap<>();
        this.baby = false;
        this.range = 10.0;
        this.knockbackResistance = 0;
    }

    // Getters and Setters
    public String getId() { return id; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isBaby() { return baby; }
    public void setBaby(boolean baby) { this.baby = baby; }

    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }

    public double getKnockbackResistance() { return knockbackResistance; }
    public void setKnockbackResistance(double knockbackResistance) { this.knockbackResistance = knockbackResistance; }

    public ItemStack getHelmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }

    public ItemStack getChestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }

    public ItemStack getLeggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }

    public ItemStack getBoots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }

    public ItemStack getMainHand() { return mainHand; }
    public void setMainHand(ItemStack mainHand) { this.mainHand = mainHand; }

    public ItemStack getOffHand() { return offHand; }
    public void setOffHand(ItemStack offHand) { this.offHand = offHand; }

    public List<MobReward> getConfiguredRewards() { return configuredRewards; }
    public void setConfiguredRewards(List<MobReward> configuredRewards) { this.configuredRewards = configuredRewards; }
    public void addConfiguredReward(String rewardId, double chance) {
        this.configuredRewards.add(new MobReward(rewardId, chance));
    }

    public Map<String, MobDrop> getDrops() { return drops; }
    public void setDrops(Map<String, MobDrop> drops) { this.drops = drops; }
    public void addDrop(String key, MobDrop drop) { this.drops.put(key, drop); }

    /**
     * Representa uma recompensa configurada no mob
     */
    public static class MobReward {
        private final String rewardId;
        private final double chance;

        public MobReward(String rewardId, double chance) {
            this.rewardId = rewardId;
            this.chance = chance;
        }

        public String getRewardId() { return rewardId; }
        public double getChance() { return chance; }
    }

    public static class MobDrop {
        private Material material;
        private String name;
        private List<String> lore;
        private double chance;
        private int minAmount;
        private int maxAmount;
        private Map<String, Integer> enchants;

        public MobDrop() {
            this.lore = new ArrayList<>();
            this.enchants = new HashMap<>();
            this.minAmount = 1;
            this.maxAmount = 1;
            this.chance = 100;
        }

        public Material getMaterial() { return material; }
        public void setMaterial(Material material) { this.material = material; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }

        public double getChance() { return chance; }
        public void setChance(double chance) { this.chance = chance; }

        public int getMinAmount() { return minAmount; }
        public int getMaxAmount() { return maxAmount; }

        public void setAmount(String amount) {
            if (amount.contains("-")) {
                String[] parts = amount.split("-");
                this.minAmount = Integer.parseInt(parts[0]);
                this.maxAmount = Integer.parseInt(parts[1]);
            } else {
                this.minAmount = Integer.parseInt(amount);
                this.maxAmount = this.minAmount;
            }
        }

        public void setAmount(int amount) {
            this.minAmount = amount;
            this.maxAmount = amount;
        }

        public int getRandomAmount() {
            if (minAmount == maxAmount) return minAmount;
            return minAmount + (int) (Math.random() * (maxAmount - minAmount + 1));
        }

        public Map<String, Integer> getEnchants() { return enchants; }
        public void setEnchants(Map<String, Integer> enchants) { this.enchants = enchants; }
    }
}
