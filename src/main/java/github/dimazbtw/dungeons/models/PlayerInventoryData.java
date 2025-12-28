package github.dimazbtw.dungeons.models;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

public class PlayerInventoryData {

    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack offHand;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final Collection<PotionEffect> potionEffects;
    private final GameMode gameMode;

    public PlayerInventoryData(Player player) {
        this.contents = player.getInventory().getContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();
        this.offHand = player.getInventory().getItemInOffHand().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.potionEffects = player.getActivePotionEffects();
        this.gameMode = player.getGameMode();
    }

    public void restore(Player player) {
        // Clear current inventory
        player.getInventory().clear();

        // Restore inventory
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armorContents);
        player.getInventory().setItemInOffHand(offHand);

        // Restore stats
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.setGameMode(gameMode);

        // Restore potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public double getHealth() {
        return health;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public int getLevel() {
        return level;
    }

    public float getExp() {
        return exp;
    }

    public Collection<PotionEffect> getPotionEffects() {
        return potionEffects;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}
