package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.PlayerData;
import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PerksMenu {

    private final Main plugin;
    private final Player player;
    private final InventoryGUI gui;
    private final FileConfiguration config;

    // Slots configuráveis
    private final int vitalitySlot;
    private final int strengthSlot;
    private final int agilitySlot;
    private final int playerInfoSlot;
    private final int xpInfoSlot;
    private final int backSlot;
    private final int[] progressBarSlots;

    public PerksMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.config = plugin.getMenuManager().getMenuConfig("perks");

        // Carregar configurações
        String title = config != null ? config.getString("menu.title", "&8&lAtributos") : "&8&lAtributos";
        int size = config != null ? config.getInt("menu.size", 45) : 45;

        // Carregar slots
        ConfigurationSection slotsSection = config != null ? config.getConfigurationSection("menu.slots") : null;
        this.vitalitySlot = slotsSection != null ? slotsSection.getInt("vitality", 11) : 11;
        this.strengthSlot = slotsSection != null ? slotsSection.getInt("strength", 13) : 13;
        this.agilitySlot = slotsSection != null ? slotsSection.getInt("agility", 15) : 15;
        this.playerInfoSlot = slotsSection != null ? slotsSection.getInt("player-info", 4) : 4;
        this.xpInfoSlot = slotsSection != null ? slotsSection.getInt("xp-info", 22) : 22;

        ConfigurationSection buttonsSection = config != null ? config.getConfigurationSection("menu.buttons") : null;
        this.backSlot = buttonsSection != null ? buttonsSection.getInt("back.slot", 40) : 40;

        // Barra de progresso
        List<Integer> barSlots = config != null ? config.getIntegerList("menu.progress-bar.slots") : null;
        if (barSlots != null && !barSlots.isEmpty()) {
            progressBarSlots = barSlots.stream().mapToInt(Integer::intValue).toArray();
        } else {
            progressBarSlots = new int[]{28, 29, 30, 31, 32, 33, 34};
        }

        this.gui = new InventoryGUI(ColorUtils.colorize(title), size);
        gui.setDefaultAllCancell(true);
        setupMenu();
    }

    private void setupMenu() {
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        if (data == null) return;

        // Info do jogador
        gui.setButton(playerInfoSlot, createPlayerInfoButton(data));

        // Vitalidade
        gui.setButton(vitalitySlot, createVitalityButton(data));

        // Força
        gui.setButton(strengthSlot, createStrengthButton(data));

        // Agilidade
        gui.setButton(agilitySlot, createAgilityButton(data));

        // Info de XP
        gui.setButton(xpInfoSlot, createXpInfoButton(data));

        // Barra de progresso visual
        createProgressBar(data);

        // Voltar
        ItemButton back = new ItemButton(Material.BARRIER, ColorUtils.colorize("&cVoltar"));
        back.setDefaultAction(e -> {
            e.setCancelled(true);
            plugin.getMenuManager().openMainMenu(player);
        });
        gui.setButton(backSlot, back);
    }

    private ItemButton createPlayerInfoButton(PlayerData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtils.colorize("&7Nível: &a" + data.getLevel()));
        lore.add(ColorUtils.colorize("&7XP: &e" + data.getExperience() + "&7/&e" + data.getExpRequiredForNextLevel()));
        lore.add("");
        lore.add(ColorUtils.colorize("&6&lPontos Disponíveis: &f" + data.getPerkPoints()));
        lore.add("");
        lore.add(ColorUtils.colorize("&7A cada nível você ganha"));
        lore.add(ColorUtils.colorize("&7&l1 ponto &7de atributo!"));

        return new ItemButton(Material.PLAYER_HEAD,
                ColorUtils.colorize("&e&l" + player.getName()),
                lore.toArray(new String[0]));
    }

    private ItemButton createVitalityButton(PlayerData data) {
        ConfigurationSection attrSection = config != null ? 
                config.getConfigurationSection("menu.attributes.vitality") : null;

        Material material = Material.GOLDEN_APPLE;
        String name = "&c&l❤ Vitalidade";
        
        if (attrSection != null) {
            try {
                material = Material.valueOf(attrSection.getString("material", "GOLDEN_APPLE"));
            } catch (IllegalArgumentException ignored) {}
            name = attrSection.getString("name", name);
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtils.colorize("&7Aumenta sua vida máxima."));
        lore.add(ColorUtils.colorize("&7Cada ponto: &c+2 ❤"));
        lore.add("");
        lore.add(ColorUtils.colorize("&7Pontos investidos: &f" + data.getVitality()));
        lore.add(ColorUtils.colorize("&7Vida máxima: &c" + (int) data.getMaxHealth() + " ❤"));
        lore.add("");

        if (data.getPerkPoints() > 0) {
            lore.add(ColorUtils.colorize("&aClique para investir!"));
            lore.add(ColorUtils.colorize("&7Custo: &e1 ponto"));
        } else {
            lore.add(ColorUtils.colorize("&cSem pontos disponíveis!"));
        }

        ItemButton button = new ItemButton(material,
                ColorUtils.colorize(name),
                lore.toArray(new String[0]));

        button.setDefaultAction(e -> {
            e.setCancelled(true);
            if (data.upgradeVitality()) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&aVitalidade aumentada! Vida máxima: &c" + (int) data.getMaxHealth() + " ❤"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                applyStats(player, data);
                new PerksMenu(plugin, player).open();
            } else {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&cVocê não tem pontos de atributo disponíveis!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        });

        return button;
    }

    private ItemButton createStrengthButton(PlayerData data) {
        ConfigurationSection attrSection = config != null ? 
                config.getConfigurationSection("menu.attributes.strength") : null;

        Material material = Material.IRON_SWORD;
        String name = "&6&l⚔ Força";
        
        if (attrSection != null) {
            try {
                material = Material.valueOf(attrSection.getString("material", "IRON_SWORD"));
            } catch (IllegalArgumentException ignored) {}
            name = attrSection.getString("name", name);
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtils.colorize("&7Aumenta seu dano de ataque."));
        lore.add(ColorUtils.colorize("&7Cada ponto: &6+0.5 ⚔"));
        lore.add("");
        lore.add(ColorUtils.colorize("&7Pontos investidos: &f" + data.getStrength()));
        lore.add(ColorUtils.colorize("&7Dano bônus: &6+" + String.format("%.1f", data.getDamageBonus()) + " ⚔"));
        lore.add("");

        if (data.getPerkPoints() > 0) {
            lore.add(ColorUtils.colorize("&aClique para investir!"));
            lore.add(ColorUtils.colorize("&7Custo: &e1 ponto"));
        } else {
            lore.add(ColorUtils.colorize("&cSem pontos disponíveis!"));
        }

        ItemButton button = new ItemButton(material,
                ColorUtils.colorize(name),
                lore.toArray(new String[0]));

        button.setDefaultAction(e -> {
            e.setCancelled(true);
            if (data.upgradeStrength()) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&aForça aumentada! Dano bônus: &6+" + String.format("%.1f", data.getDamageBonus()) + " ⚔"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                applyStats(player, data);
                new PerksMenu(plugin, player).open();
            } else {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&cVocê não tem pontos de atributo disponíveis!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        });

        return button;
    }

    private ItemButton createAgilityButton(PlayerData data) {
        boolean isMaxed = data.getAgility() >= PlayerData.MAX_AGILITY;

        ConfigurationSection attrSection = config != null ? 
                config.getConfigurationSection("menu.attributes.agility") : null;

        Material material = Material.FEATHER;
        String name = "&b&l✦ Agilidade";
        
        if (attrSection != null) {
            try {
                material = Material.valueOf(attrSection.getString("material", "FEATHER"));
            } catch (IllegalArgumentException ignored) {}
            name = attrSection.getString("name", name);
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtils.colorize("&7Aumenta sua velocidade."));
        lore.add(ColorUtils.colorize("&7Cada ponto: &b+0.5 velocidade"));
        lore.add("");
        lore.add(ColorUtils.colorize("&7Pontos investidos: &f" + (int)(data.getAgility() / PlayerData.AGILITY_PER_POINT)));
        lore.add(ColorUtils.colorize("&7Agilidade: &b" + String.format("%.1f", data.getAgility()) + "/" + String.format("%.1f", PlayerData.MAX_AGILITY)));

        int filled = (int) ((data.getAgility() / PlayerData.MAX_AGILITY) * 6);
        int empty = 6 - filled;
        lore.add(ColorUtils.colorize("&b" + "█".repeat(filled) + "&8" + "█".repeat(empty)));
        lore.add("");

        if (isMaxed) {
            lore.add(ColorUtils.colorize("&a&lNÍVEL MÁXIMO!"));
        } else if (data.getPerkPoints() > 0) {
            lore.add(ColorUtils.colorize("&aClique para investir!"));
            lore.add(ColorUtils.colorize("&7Custo: &e1 ponto"));
        } else {
            lore.add(ColorUtils.colorize("&cSem pontos disponíveis!"));
        }

        ItemButton button = new ItemButton(material,
                ColorUtils.colorize(name),
                lore.toArray(new String[0]));

        if (isMaxed) {
            button.glow(true);
        }

        button.setDefaultAction(e -> {
            e.setCancelled(true);

            if (isMaxed) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&cAgilidade já está no nível máximo!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (data.upgradeAgility()) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&aAgilidade aumentada! Velocidade: &b" + String.format("%.1f", data.getAgility())));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                applyStats(player, data);
                new PerksMenu(plugin, player).open();
            } else {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                        "&cVocê não tem pontos de atributo disponíveis!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        });

        return button;
    }

    private ItemButton createXpInfoButton(PlayerData data) {
        double progress = data.getLevelProgress();
        int percentage = (int) (progress * 100);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ColorUtils.colorize("&7Progresso: &a" + percentage + "%"));
        lore.add(ColorUtils.colorize(data.getLevelProgressBar()));
        lore.add("");
        lore.add(ColorUtils.colorize("&7XP atual: &e" + data.getExperience()));
        lore.add(ColorUtils.colorize("&7XP necessário: &e" + data.getExpRequiredForNextLevel()));
        lore.add("");
        lore.add(ColorUtils.colorize("&7Complete dungeons para"));
        lore.add(ColorUtils.colorize("&7ganhar experiência!"));

        return new ItemButton(Material.EXPERIENCE_BOTTLE,
                ColorUtils.colorize("&a&lNível " + data.getLevel()),
                lore.toArray(new String[0]));
    }

    private void createProgressBar(PlayerData data) {
        double progress = data.getLevelProgress();
        int filled = (int) (progress * progressBarSlots.length);

        String filledMat = config != null ? config.getString("menu.progress-bar.filled-material", "LIME_STAINED_GLASS_PANE") : "LIME_STAINED_GLASS_PANE";
        String emptyMat = config != null ? config.getString("menu.progress-bar.empty-material", "GRAY_STAINED_GLASS_PANE") : "GRAY_STAINED_GLASS_PANE";
        String filledName = config != null ? config.getString("menu.progress-bar.filled-name", "&a&lXP") : "&a&lXP";
        String emptyName = config != null ? config.getString("menu.progress-bar.empty-name", "&7&lXP") : "&7&lXP";

        for (int i = 0; i < progressBarSlots.length; i++) {
            int slot = progressBarSlots[i];
            Material material = i < filled ? Material.valueOf(filledMat) : Material.valueOf(emptyMat);
            String name = i < filled ? filledName : emptyName;
            ItemButton xpBar = new ItemButton(material, ColorUtils.colorize(name));
            gui.setButton(slot, xpBar);
        }
    }

    public static void applyStats(Player player, PlayerData data) {
        var healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(data.getMaxHealth());
        }

        var speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.1 + data.getSpeedModifier());
        }

        var attackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(1.0 + data.getDamageBonus());
        }
    }

    public static void resetStats(Player player) {
        var healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(20.0);
        }

        var speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.1);
        }

        var attackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(1.0);
        }
    }

    public void open() {
        gui.show(player);
    }
}
