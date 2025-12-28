package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class MainMenu {

    private final Main plugin;
    private final Player player;
    private final InventoryGUI gui;

    public MainMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration config = plugin.getMenuManager().getMenuConfig("main");
        String title = ColorUtils.colorize(config.getString("menu.title", "&8&lDungeon Hub"));
        int size = config.getInt("menu.size", 45);

        this.gui = new InventoryGUI(title, size);
        gui.setDefaultAllCancell(true);
        setupItems(config);
    }

    private void setupItems(FileConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("menu.items");
        if (itemsSection == null) return;

        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        boolean inDungeon = plugin.getSessionManager().isInDungeon(player);

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            // Handle decoration items with multiple slots
            if (itemSection.contains("slots")) {
                List<Integer> slots = itemSection.getIntegerList("slots");
                ItemButton decorItem = createItemButton(itemSection, data, inDungeon, "");
                for (int slot : slots) {
                    gui.setButton(slot, decorItem);
                }
                continue;
            }

            int slot = itemSection.getInt("slot", 0);
            String actionType = itemSection.getString("action_type", "");
            ItemButton item = createItemButton(itemSection, data, inDungeon, actionType);

            gui.setButton(slot, item);
        }
    }

    private ItemButton createItemButton(ConfigurationSection section, PlayerData data, boolean inDungeon, String actionType) {
        String materialStr = section.getString("material", "STONE");
        Material material = parseMaterial(materialStr);

        String name = ColorUtils.colorize(section.getString("name", "&fItem"));
        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(ColorUtils.colorize(processPlaceholders(line, data, inDungeon)));
        }
        boolean glow = section.getBoolean("glow", false);

        ItemButton button;

        // Check for player head
        if (materialStr.equalsIgnoreCase("{player}")) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
                meta.setDisplayName(name);
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            button = new ItemButton(skull);
        } else {
            // Check for texture head
            String texture = section.getString("texture");
            if (texture != null && !texture.isEmpty()) {
                ItemStack skull = ItemButton.getSkull("http://textures.minecraft.net/texture/" + texture);
                ItemMeta meta = skull.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                    skull.setItemMeta(meta);
                }
                button = new ItemButton(skull);
            } else {
                button = new ItemButton(material, name, lore.toArray(new String[0]));
            }
        }

        if (glow) {
            button.glow(true);
        }

        // Set action
        if (actionType != null && !actionType.isEmpty()) {
            button.setDefaultAction(event -> {
                event.setCancelled(true);
                handleAction(actionType);
            });
        }

        return button;
    }

    private Material parseMaterial(String materialStr) {
        if (materialStr.equalsIgnoreCase("{player}")) {
            return Material.PLAYER_HEAD;
        }

        try {
            return Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    private String processPlaceholders(String line, PlayerData data, boolean inDungeon) {
        String result = line;

        // Session info
        result = result.replace("{in_dungeon_status}", inDungeon ? "&aIn Dungeon" : "&7Not in Dungeon");
        result = result.replace("{active_sessions}", String.valueOf(plugin.getSessionManager().getActiveSessionCount()));
        result = result.replace("{total_players_in_dungeons}", String.valueOf(plugin.getSessionManager().getTotalPlayersInDungeons()));

        // Player stats
        if (data != null) {
            result = result.replace("{level}", String.valueOf(data.getLevel()));
            result = result.replace("{level_bar}", data.getLevelProgressBar());
            result = result.replace("{vitality}", String.valueOf(data.getVitality()));
            result = result.replace("{strength}", String.valueOf(data.getStrength()));
            result = result.replace("{agility}", String.valueOf(data.getAgility()));
            result = result.replace("{points}", String.valueOf(data.getPoints()));
            result = result.replace("{dungeons_completed}", String.valueOf(data.getDungeonsCompleted()));
            result = result.replace("{mobs_killed}", String.valueOf(data.getMobsKilled()));

            // Pending rewards
            int pendingRewards = plugin.getRewardManager().getPendingRewards(player.getUniqueId()).size();
            result = result.replace("{pending_rewards}", String.valueOf(pendingRewards));

            // Perk points (pontos disponíveis para gastar em atributos)
            int perkPoints = data.getAvailableAttributePoints();
            result = result.replace("{perk_points}", String.valueOf(perkPoints));
        } else {
            result = result.replace("{pending_rewards}", "0");
            result = result.replace("{perk_points}", "0");
        }

        return result;
    }

    private void handleAction(String actionType) {
        switch (actionType.toLowerCase()) {
            case "play" -> {
                player.closeInventory();
                plugin.getMenuManager().openDungeonListMenu(player);
            }
            case "leaderboard", "ranking", "top" -> {
                player.closeInventory();
                plugin.getMenuManager().openRankingMenu(player);
            }
            case "gear", "enchants", "upgrades" -> {
                player.closeInventory();
                plugin.getMenuManager().openEnchantsMenu(player);
            }
            case "perks", "stats", "attributes" -> {
                player.closeInventory();
                plugin.getMenuManager().openPerksMenu(player);
            }
            case "equipment", "armor", "equip" -> {
                player.closeInventory();
                plugin.getMenuManager().openEquipmentMenu(player);
            }
            case "profile" -> {
                showProfile();
            }
            case "quests" -> {
                player.sendMessage(ColorUtils.colorize("&eMissões em breve!"));
            }
            case "rewards" -> {
                player.closeInventory();
                plugin.getMenuManager().openRewardsMenu(player);
            }
            case "guild" -> {
                player.sendMessage(ColorUtils.colorize("&eGuilda em breve!"));
            }
        }
    }

    private void showProfile() {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(ColorUtils.colorize("&cError loading profile data."));
            return;
        }

        player.sendMessage(ColorUtils.colorize("&e&l--- Your Profile ---"));
        player.sendMessage(ColorUtils.colorize("&fLevel: &e" + data.getLevel()));
        player.sendMessage(ColorUtils.colorize("&fDungeons Completed: &a" + data.getDungeonsCompleted()));
        player.sendMessage(ColorUtils.colorize("&fMobs Killed: &a" + data.getMobsKilled()));
        player.sendMessage(ColorUtils.colorize("&fBosses Killed: &a" + data.getBossesKilled()));
        player.sendMessage(ColorUtils.colorize("&fDeaths: &c" + data.getDeaths()));
        player.sendMessage(ColorUtils.colorize("&fPlaytime: &b" + data.getFormattedPlayTime()));
        player.sendMessage(ColorUtils.colorize("&fPoints: &6" + data.getPoints()));
    }

    public void open() {
        gui.show(player);
    }
}
