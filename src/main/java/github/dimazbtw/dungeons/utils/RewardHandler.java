package github.dimazbtw.dungeons.utils;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonMob;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RewardHandler {

    private static final Random random = new Random();

    /**
     * Process rewards from a killed mob
     * Format: "command;chance" (e.g., "give {player} diamond 1;50")
     */
    public static void processRewards(Main plugin, Player player, List<String> rewards) {
        for (String reward : rewards) {
            String[] parts = reward.split(";");
            if (parts.length != 2) continue;

            String command = parts[0].trim();
            double chance;

            try {
                chance = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }

            // Check if reward should be given
            if (random.nextDouble() * 100 <= chance) {
                executeReward(plugin, player, command);
            }
        }
    }

    private static void executeReward(Main plugin, Player player, String command) {
        // Replace placeholders
        command = command.replace("{player}", player.getName());

        // Execute command as console
        String finalCommand = command;
        Bukkit.getScheduler().runTask(plugin, () ->
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)
        );
    }

    /**
     * Process drops from a killed mob
     */
    public static void processDrops(Main plugin, Player player, Map<String, DungeonMob.MobDrop> drops) {
        for (DungeonMob.MobDrop drop : drops.values()) {
            // Check drop chance
            if (random.nextDouble() * 100 > drop.getChance()) {
                continue;
            }

            // Create drop item
            ItemStack item = createDropItem(drop);
            if (item == null || item.getType() == Material.AIR) continue;

            // Give to player or drop at location
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                for (ItemStack overflowItem : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                }
            }
        }
    }

    private static ItemStack createDropItem(DungeonMob.MobDrop drop) {
        if (drop.getMaterial() == null) return null;

        int amount = drop.getRandomAmount();
        ItemStack item = new ItemStack(drop.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (drop.getName() != null) {
                meta.setDisplayName(ColorUtils.colorize(drop.getName()));
            }

            if (drop.getLore() != null && !drop.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : drop.getLore()) {
                    coloredLore.add(ColorUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }

            // Apply enchants
            for (Map.Entry<String, Integer> enchant : drop.getEnchants().entrySet()) {
                try {
                    Enchantment enchantment = Enchantment.getByName(enchant.getKey().toUpperCase());
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, enchant.getValue(), true);
                    }
                } catch (Exception ignored) {}
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Give experience to player with wisdom bonus
     * @return XP final dado ao jogador
     */
    public static int giveExperience(Main plugin, Player player, int baseExp) {
        double wisdomBonus = plugin.getWeaponManager().getWisdomBonus(player);
        int finalExp = (int) (baseExp * (1 + wisdomBonus / 100));

        player.giveExp(finalExp);

        // Also add to player data
        var data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data != null) {
            data.addExperience(finalExp);
        }
        
        return finalExp;
    }
}
