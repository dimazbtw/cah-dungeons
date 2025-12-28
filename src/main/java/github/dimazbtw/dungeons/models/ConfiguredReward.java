package github.dimazbtw.dungeons.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Representa uma recompensa configurada no rewards.yml
 */
public class ConfiguredReward {

    private final String id;
    private final RewardType type;
    private final String displayName;
    private final String description;
    private final int amount;
    private final Material material;
    private final String command;
    private final ItemStack customItem;

    public enum RewardType {
        ITEM,
        POINTS,
        EXPERIENCE,
        COMMAND
    }

    public ConfiguredReward(String id, RewardType type, String displayName, String description,
                            int amount, Material material, String command, ItemStack customItem) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.amount = amount;
        this.material = material;
        this.command = command;
        this.customItem = customItem;
    }

    public String getId() {
        return id;
    }

    public RewardType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getAmount() {
        return amount;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCommand() {
        return command;
    }

    public ItemStack getCustomItem() {
        return customItem;
    }

    /**
     * Cria o ItemStack para esta recompensa
     */
    public ItemStack createItemStack() {
        if (customItem != null) {
            ItemStack item = customItem.clone();
            item.setAmount(amount);
            return item;
        }
        
        if (material != null) {
            return new ItemStack(material, amount);
        }
        
        return new ItemStack(Material.CHEST);
    }

    /**
     * Builder para criar ConfiguredReward
     */
    public static class Builder {
        private String id;
        private RewardType type = RewardType.ITEM;
        private String displayName = "Reward";
        private String description = "";
        private int amount = 1;
        private Material material = Material.CHEST;
        private String command = "";
        private ItemStack customItem = null;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(RewardType type) {
            this.type = type;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder customItem(ItemStack customItem) {
            this.customItem = customItem;
            return this;
        }

        public ConfiguredReward build() {
            return new ConfiguredReward(id, type, displayName, description, amount, material, command, customItem);
        }
    }
}
