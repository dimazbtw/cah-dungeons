package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;

public class MessageManager {

    private final Main plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        prefix = ColorUtils.colorize(messages.getString("prefix", "&8[&aDungeons&8] &7"));
    }

    public void reload() {
        loadMessages();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMessage(String key) {
        String message = messages.getString("messages." + key);
        if (message == null) {
            message = messages.getString(key, key);
        }
        return ColorUtils.colorize(message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String getMessageWithPrefix(String key) {
        return prefix + getMessage(key);
    }

    public String getMessageWithPrefix(String key, Map<String, String> placeholders) {
        return prefix + getMessage(key, placeholders);
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessageWithPrefix(key));
    }

    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessageWithPrefix(key, placeholders));
    }

    // ActionBar
    public String getActionBar(String key) {
        return ColorUtils.colorize(messages.getString("actionbar." + key, ""));
    }

    public String getActionBar(String key, Map<String, String> placeholders) {
        String message = getActionBar(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    // BossBar
    public String getBossBarFormat() {
        return ColorUtils.colorize(messages.getString("bossbar.format", "&c&l{boss_name}"));
    }

    public BarColor getBossBarColor() {
        try {
            return BarColor.valueOf(messages.getString("bossbar.color", "RED").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.RED;
        }
    }

    public BarStyle getBossBarStyle() {
        try {
            return BarStyle.valueOf(messages.getString("bossbar.style", "SEGMENTED_10").toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SEGMENTED_10;
        }
    }

    // Titles
    public String getTitleMain(String key) {
        return ColorUtils.colorize(messages.getString("titles." + key + ".title", ""));
    }

    public String getTitleSub(String key) {
        return ColorUtils.colorize(messages.getString("titles." + key + ".subtitle", ""));
    }

    public int getTitleFadeIn(String key) {
        return messages.getInt("titles." + key + ".fade-in", 10);
    }

    public int getTitleStay(String key) {
        return messages.getInt("titles." + key + ".stay", 40);
    }

    public int getTitleFadeOut(String key) {
        return messages.getInt("titles." + key + ".fade-out", 10);
    }

    public void sendTitle(Player player, String key) {
        sendTitle(player, key, null);
    }

    public void sendTitle(Player player, String key, Map<String, String> placeholders) {
        String title = getTitleMain(key);
        String subtitle = getTitleSub(key);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                title = title.replace("{" + entry.getKey() + "}", entry.getValue());
                subtitle = subtitle.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        player.sendTitle(title, subtitle, getTitleFadeIn(key), getTitleStay(key), getTitleFadeOut(key));
    }

    // Sounds
    public Sound getSound(String key) {
        try {
            return Sound.valueOf(messages.getString("sounds." + key, "BLOCK_NOTE_BLOCK_PLING").toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }

    public void playSound(Player player, String key) {
        Sound sound = getSound(key);
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public void playSound(Player player, String key, float volume, float pitch) {
        Sound sound = getSound(key);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // Progress Bar
    public String createProgressBar(double current, double max) {
        int amount = messages.getInt("progress-bar.amount", 10);
        String symbol = messages.getString("progress-bar.symbol", "❤");
        String colorFilled = messages.getString("progress-bar.color-filled", "&c");
        String colorEmpty = messages.getString("progress-bar.color-empty", "&7");

        double percentage = current / max;
        int filled = (int) Math.round(percentage * amount);
        int empty = amount - filled;

        StringBuilder bar = new StringBuilder();
        bar.append(ColorUtils.colorize(colorFilled));
        for (int i = 0; i < filled; i++) {
            bar.append(symbol);
        }
        bar.append(ColorUtils.colorize(colorEmpty));
        for (int i = 0; i < empty; i++) {
            bar.append(symbol);
        }

        return bar.toString();
    }
}
