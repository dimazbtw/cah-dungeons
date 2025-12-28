package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.menus.DungeonListMenu;
import github.dimazbtw.dungeons.menus.EnchantsMenu;
import github.dimazbtw.dungeons.menus.EquipmentMenu;
import github.dimazbtw.dungeons.menus.MainMenu;
import github.dimazbtw.dungeons.menus.PerksMenu;
import github.dimazbtw.dungeons.menus.RankingMenu;
import github.dimazbtw.dungeons.menus.RewardsMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MenuManager {

    private final Main plugin;
    private final Map<String, FileConfiguration> menuConfigs;

    public MenuManager(Main plugin) {
        this.plugin = plugin;
        this.menuConfigs = new HashMap<>();
        loadMenus();
    }

    private void loadMenus() {
        menuConfigs.clear();
        File menuFolder = new File(plugin.getDataFolder(), "menus");

        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }

        File[] files = menuFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String menuName = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                menuConfigs.put(menuName, config);
                plugin.getLogger().info("Loaded menu: " + menuName);
            }
        }
    }

    public void reload() {
        loadMenus();
    }

    public FileConfiguration getMenuConfig(String menuName) {
        return menuConfigs.get(menuName);
    }

    public void openMainMenu(Player player) {
        new MainMenu(plugin, player).open();
    }

    public void openDungeonListMenu(Player player) {
        new DungeonListMenu(plugin, player).open();
    }

    public void openEnchantsMenu(Player player) {
        new EnchantsMenu(plugin, player).open();
    }

    public void openRewardsMenu(Player player) {
        new RewardsMenu(plugin, player).open();
    }

    public void openPerksMenu(Player player) {
        new PerksMenu(plugin, player).open();
    }

    public void openEquipmentMenu(Player player) {
        new EquipmentMenu(plugin, player).open();
    }

    public void openRankingMenu(Player player) {
        new RankingMenu(plugin, player).open();
    }
}
