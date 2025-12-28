package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.managers.RankingManager.RankingEntry;
import github.dimazbtw.dungeons.managers.RankingManager.RankingType;
import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class RankingMenu {

    private final Main plugin;
    private final Player player;

    public RankingMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Abre o menu principal de ranking
     */
    public void open() {
        InventoryGUI gui = new InventoryGUI(ColorUtils.colorize("&6&lRanking"), 27);
        gui.setDefaultAllCancell(true);

        // Mobs Killed
        ItemButton mobsButton = new ItemButton(Material.ZOMBIE_HEAD,
                ColorUtils.colorize("&c&lMobs Eliminados"),
                ColorUtils.colorize("&7Ranking de jogadores com"),
                ColorUtils.colorize("&7mais mobs eliminados."),
                "",
                ColorUtils.colorize("&eClique para ver!")
        );
        mobsButton.setDefaultAction(e -> {
            e.setCancelled(true);
            openRanking(RankingType.MOBS_KILLED);
        });
        gui.setButton(10, mobsButton);

        // Bosses Killed
        ItemButton bossesButton = new ItemButton(Material.WITHER_SKELETON_SKULL,
                ColorUtils.colorize("&4&lBosses Derrotados"),
                ColorUtils.colorize("&7Ranking de jogadores com"),
                ColorUtils.colorize("&7mais bosses derrotados."),
                "",
                ColorUtils.colorize("&eClique para ver!")
        );
        bossesButton.setDefaultAction(e -> {
            e.setCancelled(true);
            openRanking(RankingType.BOSSES_KILLED);
        });
        gui.setButton(12, bossesButton);

        // Dungeons Completed
        ItemButton dungeonsButton = new ItemButton(Material.GOLDEN_SWORD,
                ColorUtils.colorize("&a&lDungeons Completadas"),
                ColorUtils.colorize("&7Ranking de jogadores com"),
                ColorUtils.colorize("&7mais dungeons completadas."),
                "",
                ColorUtils.colorize("&eClique para ver!")
        );
        dungeonsButton.setDefaultAction(e -> {
            e.setCancelled(true);
            openRanking(RankingType.DUNGEONS_COMPLETED);
        });
        gui.setButton(14, dungeonsButton);

        // Points
        ItemButton pointsButton = new ItemButton(Material.EMERALD,
                ColorUtils.colorize("&b&lPontos"),
                ColorUtils.colorize("&7Ranking de jogadores com"),
                ColorUtils.colorize("&7mais pontos acumulados."),
                "",
                ColorUtils.colorize("&eClique para ver!")
        );
        pointsButton.setDefaultAction(e -> {
            e.setCancelled(true);
            openRanking(RankingType.POINTS);
        });
        gui.setButton(16, pointsButton);

        // Voltar
        ItemButton backButton = new ItemButton(Material.ARROW, ColorUtils.colorize("&c&lVoltar"));
        backButton.setDefaultAction(e -> {
            e.setCancelled(true);
            plugin.getMenuManager().openMainMenu(player);
        });
        gui.setButton(22, backButton);

        gui.show(player);
    }

    /**
     * Abre o ranking específico
     */
    public void openRanking(RankingType type) {
        String title = switch (type) {
            case MOBS_KILLED -> "&c&lTop Mobs Eliminados";
            case BOSSES_KILLED -> "&4&lTop Bosses Derrotados";
            case DUNGEONS_COMPLETED -> "&a&lTop Dungeons";
            case POINTS -> "&b&lTop Pontos";
            case LEVEL -> "&e&lTop Níveis";
        };

        InventoryGUI gui = new InventoryGUI(ColorUtils.colorize(title), 54);
        gui.setDefaultAllCancell(true);

        List<RankingEntry> ranking = switch (type) {
            case MOBS_KILLED -> plugin.getRankingManager().getMobsKilledRanking(45);
            case BOSSES_KILLED -> plugin.getRankingManager().getBossesKilledRanking(45);
            case DUNGEONS_COMPLETED -> plugin.getRankingManager().getDungeonsCompletedRanking(45);
            case POINTS -> plugin.getRankingManager().getPointsRanking(45);
            case LEVEL -> plugin.getRankingManager().getLevelRanking(45);
        };

        int slot = 0;
        int position = 1;
        for (RankingEntry entry : ranking) {
            if (slot >= 45) break;

            ItemStack skull = createPlayerHead(entry.getPlayerName(), position, entry.getValue(type), type);
            ItemButton button = new ItemButton(skull);
            gui.setButton(slot, button);

            slot++;
            position++;
        }

        // Botão de voltar
        ItemButton backButton = new ItemButton(Material.ARROW, ColorUtils.colorize("&c&lVoltar"));
        backButton.setDefaultAction(e -> {
            e.setCancelled(true);
            open();
        });
        gui.setButton(49, backButton);

        // Mostrar posição do jogador
        int playerPosition = plugin.getRankingManager().getPlayerPosition(player.getUniqueId(), type);
        RankingEntry playerEntry = plugin.getRankingManager().getPlayerEntry(player.getUniqueId(), type);

        if (playerEntry != null) {
            ItemButton yourPosition = new ItemButton(Material.PLAYER_HEAD,
                    ColorUtils.colorize("&e&lSua Posição"),
                    ColorUtils.colorize("&7Posição: &f#" + playerPosition),
                    ColorUtils.colorize("&7Valor: &f" + playerEntry.getValue(type)),
                    "",
                    ColorUtils.colorize("&7Continue jogando para subir!")
            );
            gui.setButton(45, yourPosition);
        }

        gui.show(player);
    }

    private ItemStack createPlayerHead(String playerName, int position, int value, RankingType type) {
        String positionColor = switch (position) {
            case 1 -> "&6&l"; // Ouro
            case 2 -> "&7&l"; // Prata
            case 3 -> "&c&l"; // Bronze
            default -> "&f";
        };

        String typeLabel = switch (type) {
            case MOBS_KILLED -> "Mobs";
            case BOSSES_KILLED -> "Bosses";
            case DUNGEONS_COMPLETED -> "Dungeons";
            case POINTS -> "Pontos";
            case LEVEL -> "Nível";
        };

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName));
            meta.setDisplayName(ColorUtils.colorize(positionColor + "#" + position + " &f" + playerName));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" + typeLabel + ": &e" + value));

            if (position == 1) {
                lore.add("");
                lore.add(ColorUtils.colorize("&6⭐ &e&lCAMPEÃO! &6⭐"));
            } else if (position == 2) {
                lore.add("");
                lore.add(ColorUtils.colorize("&7⭐ &7&lVICE-CAMPEÃO! &7⭐"));
            } else if (position == 3) {
                lore.add("");
                lore.add(ColorUtils.colorize("&c⭐ &c&l3º LUGAR! &c⭐"));
            }

            meta.setLore(lore);
            skull.setItemMeta(meta);
        }

        return skull;
    }
}