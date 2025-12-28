package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.Dungeon;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.lib.inventories.PaginatedGUI;
import github.dimazbtw.lib.inventories.PaginatedGUIBuilder;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DungeonListMenu {

    private final Main plugin;
    private final Player player;
    private final FileConfiguration config;
    private PaginatedGUI paginatedGUI;

    public DungeonListMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.config = plugin.getMenuManager().getMenuConfig("dungeons");
        buildMenu();
    }

    private void buildMenu() {
        String title = ColorUtils.colorize(config.getString("menu.title", "&8&lDungeon Selection"));

        // Get shape from config
        List<String> shapeList = config.getStringList("menu.shape");
        StringBuilder shapeBuilder = new StringBuilder();
        for (String row : shapeList) {
            shapeBuilder.append(row);
        }
        String shape = shapeBuilder.toString();

        // Create dungeon buttons
        List<ItemButton> dungeonButtons = createDungeonButtons();

        // Create builder
        PaginatedGUIBuilder builder = new PaginatedGUIBuilder(title, shape);
        builder.setDefaultAllCancell(true);

        // Set border
        ConfigurationSection borderSection = config.getConfigurationSection("menu.border");
        if (borderSection != null) {
            String materialStr = borderSection.getString("material", "BLACK_STAINED_GLASS_PANE");
            Material material;
            try {
                material = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.BLACK_STAINED_GLASS_PANE;
            }
            String borderName = ColorUtils.colorize(borderSection.getString("name", "&7"));
            builder.setBorder(new ItemButton(material, borderName));
        }

        // Set navigation items
        ConfigurationSection navSection = config.getConfigurationSection("menu.navigation");
        if (navSection != null) {
            // Next page
            ConfigurationSection nextSection = navSection.getConfigurationSection("next_page");
            if (nextSection != null) {
                builder.setNextPageItem(createNavItem(nextSection));
            }

            // Previous page
            ConfigurationSection prevSection = navSection.getConfigurationSection("previous_page");
            if (prevSection != null) {
                builder.setPreviousPageItem(createNavItem(prevSection));
            }
        }

        // Set content
        builder.setContent(dungeonButtons);

        // Set hotbar items
        ConfigurationSection hotbarSection = config.getConfigurationSection("menu.hotbar");
        if (hotbarSection != null) {
            for (String key : hotbarSection.getKeys(false)) {
                ConfigurationSection itemSection = hotbarSection.getConfigurationSection(key);
                if (itemSection == null) continue;

                int slot = itemSection.getInt("slot", 0);
                if (slot > 8) slot = 8;

                ItemButton button = createHotbarButton(itemSection);
                builder.setHotbarButton((byte) slot, button);
            }
        }

        this.paginatedGUI = builder.build();
    }

    private List<ItemButton> createDungeonButtons() {
        List<ItemButton> buttons = new ArrayList<>();
        Collection<Dungeon> dungeons = plugin.getDungeonManager().getAllDungeons();

        ConfigurationSection buttonConfig = config.getConfigurationSection("menu.dungeon_button");
        ConfigurationSection statusColors = config.getConfigurationSection("menu.status_colors");

        for (Dungeon dungeon : dungeons) {
            DungeonSession session = plugin.getSessionManager().getSessionByDungeon(dungeon.getId());
            ItemButton button = createDungeonButton(dungeon, session, buttonConfig, statusColors);
            buttons.add(button);
        }

        return buttons;
    }

    private ItemButton createDungeonButton(Dungeon dungeon, DungeonSession session,
                                           ConfigurationSection buttonConfig, ConfigurationSection statusColors) {
        Material material = dungeon.getMaterial();
        if (material == null) {
            String materialStr = buttonConfig != null ? buttonConfig.getString("material", "DIAMOND_SWORD") : "DIAMOND_SWORD";
            try {
                material = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.DIAMOND_SWORD;
            }
        }

        String name = dungeon.getMaterialName();
        if (name == null && buttonConfig != null) {
            name = buttonConfig.getString("name", "&a{dungeon_name}");
        }
        if (name == null) {
            name = "&a" + dungeon.getId();
        }

        List<String> lore = dungeon.getMaterialLore();
        if ((lore == null || lore.isEmpty()) && buttonConfig != null) {
            lore = buttonConfig.getStringList("lore");
        }
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Process placeholders
        int currentPlayers = session != null ? session.getPlayerCount() : 0;
        String status = getStatusString(session, statusColors);
        String scheduleStr = dungeon.getScheduleString();

        String finalName = ColorUtils.colorize(name.replace("{dungeon_name}", dungeon.getDisplayName()));

        List<String> finalLore = new ArrayList<>();
        for (String line : lore) {
            String processed = line
                    .replace("{description}", dungeon.getDescriptionString())
                    .replace("{dungeon_name}", dungeon.getDisplayName())
                    .replace("{min_players}", String.valueOf(dungeon.getMinPlayers()))
                    .replace("{max_players}", String.valueOf(dungeon.getMaxPlayers()))
                    .replace("{current_players}", String.valueOf(currentPlayers))
                    .replace("{total_players}", String.valueOf(currentPlayers))
                    .replace("{total_rounds}", String.valueOf(dungeon.getTotalRounds()))
                    .replace("{status}", status)
                    .replace("{schedule}", scheduleStr);
            finalLore.add(ColorUtils.colorize(processed));
        }

        ItemButton button = new ItemButton(material, finalName, finalLore.toArray(new String[0]));

        if (dungeon.isMaterialGlow()) {
            button.glow(true);
        }

        // Set click action
        button.setDefaultAction(event -> {
            event.setCancelled(true);
            joinDungeon(dungeon);
        });

        return button;
    }

    private String getStatusString(DungeonSession session, ConfigurationSection statusColors) {
        if (statusColors == null) {
            if (session == null) return "&aAvailable";
            return session.isWaiting() ? "&eWaiting" : "&cIn Progress";
        }

        if (session == null) {
            return ColorUtils.colorize(statusColors.getString("available", "&aAvailable"));
        }

        if (session.isWaiting()) {
            return ColorUtils.colorize(statusColors.getString("waiting", "&eWaiting"));
        } else if (session.isStarting()) {
            return ColorUtils.colorize(statusColors.getString("starting", "&aStarting"));
        } else if (session.isInProgress() || session.isBossRound()) {
            return ColorUtils.colorize(statusColors.getString("in_progress", "&cIn Progress"));
        } else {
            return ColorUtils.colorize(statusColors.getString("available", "&aAvailable"));
        }
    }

    private ItemStack createNavItem(ConfigurationSection section) {
        String materialStr = section.getString("material", "ARROW");
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.ARROW;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(section.getString("name", "&7Navigation")));
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(ColorUtils.colorize(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemButton createHotbarButton(ConfigurationSection section) {
        String materialStr = section.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }

        String name = ColorUtils.colorize(section.getString("name", "&fItem"));
        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(ColorUtils.colorize(line));
        }
        boolean glow = section.getBoolean("glow", false);

        ItemButton button = new ItemButton(material, name, lore.toArray(new String[0]));

        if (glow) {
            button.glow(true);
        }

        String actionType = section.getString("action_type", "");
        button.setDefaultAction(event -> {
            event.setCancelled(true);
            handleHotbarAction(actionType);
        });

        return button;
    }

    private void handleHotbarAction(String actionType) {
        switch (actionType.toLowerCase()) {
            case "random" -> {
                Collection<Dungeon> dungeons = plugin.getDungeonManager().getAllDungeons();
                if (!dungeons.isEmpty()) {
                    List<Dungeon> dungeonList = new ArrayList<>(dungeons);
                    Dungeon randomDungeon = dungeonList.get((int) (Math.random() * dungeonList.size()));
                    joinDungeon(randomDungeon);
                }
            }
            case "back" -> {
                player.closeInventory();
                plugin.getMenuManager().openMainMenu(player);
            }
            case "refresh" -> {
                player.closeInventory();
                new DungeonListMenu(plugin, player).open();
            }
        }
    }

    private void joinDungeon(Dungeon dungeon) {
        player.closeInventory();

        DungeonSession session = plugin.getSessionManager().getSessionByDungeon(dungeon.getId());
        if (session == null) {
            session = plugin.getSessionManager().createSession(dungeon);
        }

        plugin.getSessionManager().joinSession(player, session);
    }

    public void open() {
        paginatedGUI.show(player);
    }
}
