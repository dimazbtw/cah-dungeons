package github.dimazbtw.dungeons.models;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DungeonBoss {

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

    // Abilities
    private Map<String, BossAbility> abilities;

    // Rewards (novo sistema)
    private List<DungeonMob.MobReward> configuredRewards;

    // Drops
    private Map<String, DungeonMob.MobDrop> drops;

    public DungeonBoss(String id) {
        this.id = id;
        this.abilities = new HashMap<>();
        this.configuredRewards = new ArrayList<>();
        this.drops = new HashMap<>();
        this.baby = false;
        this.range = 20.0;
        this.knockbackResistance = 0.5;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isBaby() {
        return baby;
    }

    public void setBaby(boolean baby) {
        this.baby = baby;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getKnockbackResistance() {
        return knockbackResistance;
    }

    public void setKnockbackResistance(double knockbackResistance) {
        this.knockbackResistance = knockbackResistance;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public ItemStack getMainHand() {
        return mainHand;
    }

    public void setMainHand(ItemStack mainHand) {
        this.mainHand = mainHand;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public void setOffHand(ItemStack offHand) {
        this.offHand = offHand;
    }

    public Map<String, BossAbility> getAbilities() {
        return abilities;
    }

    public void setAbilities(Map<String, BossAbility> abilities) {
        this.abilities = abilities;
    }

    public void addAbility(String key, BossAbility ability) {
        this.abilities.put(key, ability);
    }

    public List<DungeonMob.MobReward> getConfiguredRewards() {
        return configuredRewards;
    }

    public void setConfiguredRewards(List<DungeonMob.MobReward> configuredRewards) {
        this.configuredRewards = configuredRewards;
    }

    public void addConfiguredReward(String rewardId, double chance) {
        this.configuredRewards.add(new DungeonMob.MobReward(rewardId, chance));
    }

    public Map<String, DungeonMob.MobDrop> getDrops() {
        return drops;
    }

    public void setDrops(Map<String, DungeonMob.MobDrop> drops) {
        this.drops = drops;
    }

    public static class BossAbility {
        private boolean enabled;
        private int interval; // In seconds
        private Map<String, Object> parameters;

        public BossAbility() {
            this.parameters = new HashMap<>();
            this.enabled = false;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getInterval() {
            return interval;
        }

        public void setInterval(int interval) {
            this.interval = interval;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public Object getParameter(String key) {
            return parameters.get(key);
        }

        public int getIntParameter(String key, int defaultValue) {
            Object value = parameters.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defaultValue;
        }

        public double getDoubleParameter(String key, double defaultValue) {
            Object value = parameters.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return defaultValue;
        }
    }
}
