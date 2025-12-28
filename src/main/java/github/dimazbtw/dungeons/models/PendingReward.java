package github.dimazbtw.dungeons.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PendingReward {

    private final String id;
    private final RewardType type;
    private final String dungeonId;
    private final long timestamp;
    
    // Para comandos
    private String command;
    
    // Para itens
    private ItemStack item;
    
    // Para experiência
    private int experience;
    
    // Para pontos
    private int points;

    // Para display no menu (especialmente comandos)
    private String displayName;
    private Material displayMaterial;
    private String description;

    public PendingReward(String dungeonId, RewardType type) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.dungeonId = dungeonId;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    // Construtor alternativo com tipo e item
    public PendingReward(RewardType type, String dungeonId, ItemStack item) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.dungeonId = dungeonId;
        this.timestamp = System.currentTimeMillis();
        this.item = item != null ? item.clone() : null;
    }

    // Construtor alternativo com tipo e pontos
    public PendingReward(RewardType type, String dungeonId, int amount) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.dungeonId = dungeonId;
        this.timestamp = System.currentTimeMillis();
        if (type == RewardType.POINTS) {
            this.points = amount;
        } else if (type == RewardType.EXPERIENCE) {
            this.experience = amount;
        }
    }

    // Construtor alternativo com tipo e comando
    public PendingReward(RewardType type, String dungeonId, String command) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.dungeonId = dungeonId;
        this.timestamp = System.currentTimeMillis();
        this.command = command;
    }

    public static PendingReward createCommandReward(String dungeonId, String command) {
        PendingReward reward = new PendingReward(dungeonId, RewardType.COMMAND);
        reward.command = command;
        return reward;
    }

    public static PendingReward createItemReward(String dungeonId, ItemStack item) {
        PendingReward reward = new PendingReward(dungeonId, RewardType.ITEM);
        reward.item = item.clone();
        return reward;
    }

    public static PendingReward createExperienceReward(String dungeonId, int experience) {
        PendingReward reward = new PendingReward(dungeonId, RewardType.EXPERIENCE);
        reward.experience = experience;
        return reward;
    }

    public static PendingReward createPointsReward(String dungeonId, int points) {
        PendingReward reward = new PendingReward(dungeonId, RewardType.POINTS);
        reward.points = points;
        return reward;
    }

    // Getters
    public String getId() { return id; }
    public RewardType getType() { return type; }
    public String getDungeonId() { return dungeonId; }
    public long getTimestamp() { return timestamp; }
    public String getCommand() { return command; }
    public ItemStack getItem() { return item; }
    public int getExperience() { return experience; }
    public int getPoints() { return points; }

    // Display getters/setters
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public Material getDisplayMaterial() { return displayMaterial; }
    public void setDisplayMaterial(Material displayMaterial) { this.displayMaterial = displayMaterial; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public void setExperience(int experience) { this.experience = experience; }
    public void setPoints(int points) { this.points = points; }

    public enum RewardType {
        COMMAND,
        ITEM,
        EXPERIENCE,
        POINTS
    }
}
