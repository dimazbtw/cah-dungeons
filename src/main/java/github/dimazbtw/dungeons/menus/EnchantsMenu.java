package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.managers.WeaponManager.EnchantConfig;
import github.dimazbtw.dungeons.managers.WeaponManager.EnchantLevel;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantsMenu {

    private final Main plugin;
    private final Player player;
    private final InventoryGUI gui;
    private final FileConfiguration config;

    // Configurações
    private final int[] enchantSlots;
    private final Map<String, Material> enchantMaterials;
    private final Map<String, String> enchantColors;

    public EnchantsMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.config = plugin.getMenuManager().getMenuConfig("enchants");

        // Carregar configurações
        String title = config != null ? config.getString("menu.title", "&8&lMelhorias da Arma") : "&8&lMelhorias da Arma";
        int size = config != null ? config.getInt("menu.size", 45) : 45;

        // Slots dos encantamentos
        List<Integer> slotsList = config != null ? config.getIntegerList("menu.enchant-slots") : null;
        if (slotsList != null && !slotsList.isEmpty()) {
            enchantSlots = slotsList.stream().mapToInt(Integer::intValue).toArray();
        } else {
            enchantSlots = new int[]{10, 12, 14, 16, 28, 30, 32, 34};
        }

        // Materiais dos encantamentos
        enchantMaterials = new HashMap<>();
        ConfigurationSection matSection = config != null ? config.getConfigurationSection("menu.enchant-materials") : null;
        if (matSection != null) {
            for (String key : matSection.getKeys(false)) {
                try {
                    enchantMaterials.put(key, Material.valueOf(matSection.getString(key, "ENCHANTED_BOOK")));
                } catch (IllegalArgumentException e) {
                    enchantMaterials.put(key, Material.ENCHANTED_BOOK);
                }
            }
        }

        // Cores dos encantamentos
        enchantColors = new HashMap<>();
        ConfigurationSection colorSection = config != null ? config.getConfigurationSection("menu.enchant-colors") : null;
        if (colorSection != null) {
            for (String key : colorSection.getKeys(false)) {
                enchantColors.put(key, colorSection.getString(key, "&f"));
            }
        }

        this.gui = new InventoryGUI(ColorUtils.colorize(title), size);
        gui.setDefaultAllCancell(true);
        setupMenu();
    }

    private void setupMenu() {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return;

        // Encantamentos
        List<EnchantConfig> enchants = new ArrayList<>(plugin.getWeaponManager().getAllEnchants());
        for (int i = 0; i < enchants.size() && i < enchantSlots.length; i++) {
            EnchantConfig enchant = enchants.get(i);
            Material material = enchantMaterials.getOrDefault(enchant.getId(), Material.ENCHANTED_BOOK);
            ItemButton enchantButton = createEnchantButton(enchant, data, material);
            gui.setButton(enchantSlots[i], enchantButton);
        }

        // Info do jogador
        int playerInfoSlot = config != null ? config.getInt("menu.buttons.player-info.slot", 22) : 22;
        ItemButton playerInfo = new ItemButton(Material.PLAYER_HEAD,
                ColorUtils.colorize("&e&l" + player.getName()),
                ColorUtils.colorize("&7Seus pontos: &6" + data.getPoints()),
                ColorUtils.colorize("&7Nível: &a" + data.getLevel()),
                "",
                ColorUtils.colorize("&7Clique nos encantamentos"),
                ColorUtils.colorize("&7para fazer upgrade!")
        );
        gui.setButton(playerInfoSlot, playerInfo);

        // Arma preview
        int weaponSlot = config != null ? config.getInt("menu.buttons.weapon-preview.slot", 4) : 4;
        ItemButton weaponPreview = new ItemButton(Material.DIAMOND_SWORD,
                ColorUtils.colorize("&c&lSua Arma"),
                createWeaponLore(data)
        );
        weaponPreview.glow(true);
        gui.setButton(weaponSlot, weaponPreview);

        // Voltar
        int backSlot = config != null ? config.getInt("menu.buttons.back.slot", 40) : 40;
        ItemButton back = new ItemButton(Material.BARRIER, ColorUtils.colorize("&cVoltar"));
        back.setDefaultAction(e -> {
            e.setCancelled(true);
            plugin.getMenuManager().openMainMenu(player);
        });
        gui.setButton(backSlot, back);
    }

    private ItemButton createEnchantButton(EnchantConfig enchant, PlayerData data, Material material) {
        int currentLevel = data.getEnchantLevel(enchant.getId());
        int maxLevel = enchant.getMaxLevel();
        boolean isMaxed = currentLevel >= maxLevel;

        EnchantLevel nextLevelConfig = enchant.getLevel(currentLevel + 1);
        int upgradeCost = nextLevelConfig != null ? nextLevelConfig.getPointsCost() : 0;
        boolean canAfford = data.getPoints() >= upgradeCost;

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + enchant.getDescription()));
        lore.add("");
        lore.add(ColorUtils.colorize("&7Nível atual: " + getLevelColor(currentLevel, maxLevel) + currentLevel + "/" + maxLevel));
        lore.add("");

        // Mostrar níveis
        for (int i = 1; i <= maxLevel; i++) {
            EnchantLevel levelConfig = enchant.getLevel(i);
            if (levelConfig == null) continue;

            String status;
            if (i <= currentLevel) {
                status = "&a✔";
            } else if (i == currentLevel + 1) {
                status = "&e➤";
            } else {
                status = "&c✘";
            }

            String levelInfo = formatLevelInfo(enchant.getId(), levelConfig);
            lore.add(ColorUtils.colorize(status + " &7Nível " + i + ": " + levelInfo));
        }

        lore.add("");

        if (isMaxed) {
            lore.add(ColorUtils.colorize("&a&lNÍVEL MÁXIMO!"));
        } else {
            lore.add(ColorUtils.colorize("&7Custo upgrade: " + (canAfford ? "&a" : "&c") + upgradeCost + " pontos"));
            lore.add("");
            if (canAfford) {
                lore.add(ColorUtils.colorize("&aClique para fazer upgrade!"));
            } else {
                lore.add(ColorUtils.colorize("&cPontos insuficientes!"));
            }
        }

        String color = enchantColors.getOrDefault(enchant.getId(), "&f");
        ItemButton button = new ItemButton(material,
                ColorUtils.colorize(color + "&l" + enchant.getName()),
                lore.toArray(new String[0]));

        if (currentLevel > 0) {
            button.glow(true);
        }

        button.setDefaultAction(e -> {
            e.setCancelled(true);

            if (isMaxed) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&cEste encantamento já está no nível máximo!"));
                return;
            }

            if (!canAfford) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&cVocê não tem pontos suficientes!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (plugin.getWeaponManager().upgradeEnchant(player, enchant.getId())) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&aEncantamento &e" + enchant.getName() + " &amelhorado para nível &e" + (currentLevel + 1) + "&a!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                new EnchantsMenu(plugin, player).open();
            }
        });

        return button;
    }

    private String formatLevelInfo(String enchantId, EnchantLevel level) {
        return switch (enchantId) {
            case "smite" -> "&f+" + level.getValue() + "% dano";
            case "wisdom" -> "&f+" + level.getValue() + "% XP";
            case "lifesteal" -> "&f" + (int) level.getChance() + "% chance, +" + (level.getValue() * 2) + " vida";
            case "crit" -> "&f" + (int) level.getChance() + "% chance dano dobrado";
            case "thor" -> "&f" + (int) level.getChance() + "% chance, " + level.getRadius() + " blocos";
            case "shockwave" -> "&f" + (int) level.getChance() + "% chance, " + level.getRadius() + " blocos";
            default -> "&f" + level.getValue();
        };
    }

    private String getLevelColor(int current, int max) {
        if (current >= max) return "&a";
        if (current > max / 2) return "&e";
        if (current > 0) return "&6";
        return "&c";
    }

    private String[] createWeaponLore(PlayerData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        for (EnchantConfig enchant : plugin.getWeaponManager().getAllEnchants()) {
            int level = data.getEnchantLevel(enchant.getId());
            String color = level > 0 ? "&a" : "&7";
            lore.add(ColorUtils.colorize(color + enchant.getName() + ": &f" + level + "/" + enchant.getMaxLevel()));
        }

        lore.add("");
        lore.add(ColorUtils.colorize("&7Use seus &6pontos &7para"));
        lore.add(ColorUtils.colorize("&7melhorar sua arma!"));

        return lore.toArray(new String[0]);
    }

    public void open() {
        gui.show(player);
    }
}
