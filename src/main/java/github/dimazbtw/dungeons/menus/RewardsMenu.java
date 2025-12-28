package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.PendingReward;
import github.dimazbtw.lib.inventories.InventoryGUI;
import github.dimazbtw.lib.inventories.ItemButton;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RewardsMenu {

    private final Main plugin;
    private final Player player;
    private final InventoryGUI gui;
    private final FileConfiguration config;
    private int currentPage;

    private final int[] rewardSlots;

    public RewardsMenu(Main plugin, Player player) {
        this(plugin, player, 0);
    }

    public RewardsMenu(Main plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.currentPage = page;
        this.config = plugin.getMenuManager().getMenuConfig("rewards");

        // Carregar configuração
        String title = config != null ? config.getString("menu.title", "&8&lRecompensas Pendentes") : "&8&lRecompensas Pendentes";
        int size = config != null ? config.getInt("menu.size", 54) : 54;

        // Slots de recompensa
        List<Integer> slotsList = config != null ? config.getIntegerList("menu.reward-slots") : null;
        if (slotsList != null && !slotsList.isEmpty()) {
            rewardSlots = slotsList.stream().mapToInt(Integer::intValue).toArray();
        } else {
            rewardSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        }

        this.gui = new InventoryGUI(ColorUtils.colorize(title), size);
        gui.setDefaultAllCancell(true);
        setupMenu();
    }

    private void setupMenu() {
        List<PendingReward> rewards = plugin.getRewardManager().getPendingRewards(player.getUniqueId());
        int totalPages = (int) Math.ceil(rewards.size() / (double) rewardSlots.length);
        if (totalPages == 0) totalPages = 1;

        // Recompensas
        int startIndex = currentPage * rewardSlots.length;
        for (int i = 0; i < rewardSlots.length; i++) {
            int rewardIndex = startIndex + i;
            if (rewardIndex >= rewards.size()) break;

            PendingReward reward = rewards.get(rewardIndex);
            ItemButton rewardButton = createRewardButton(reward);
            gui.setButton(rewardSlots[i], rewardButton);
        }

        // Navegação
        int prevSlot = config != null ? config.getInt("menu.buttons.prev-page.slot", 48) : 48;
        int infoSlot = config != null ? config.getInt("menu.buttons.info.slot", 49) : 49;
        int nextSlot = config != null ? config.getInt("menu.buttons.next-page.slot", 50) : 50;
        int claimAllSlot = config != null ? config.getInt("menu.buttons.claim-all.slot", 53) : 53;
        int backSlot = config != null ? config.getInt("menu.buttons.back.slot", 45) : 45;

        if (currentPage > 0) {
            ItemButton prevPage = new ItemButton(Material.ARROW, ColorUtils.colorize("&aPágina Anterior"));
            prevPage.setDefaultAction(e -> {
                e.setCancelled(true);
                new RewardsMenu(plugin, player, currentPage - 1).open();
            });
            gui.setButton(prevSlot, prevPage);
        }

        // Info
        final int finalTotalPages = totalPages;
        ItemButton info = new ItemButton(Material.BOOK, 
                ColorUtils.colorize("&e&lRecompensas"), 
                ColorUtils.colorize("&7Total: &f" + rewards.size()),
                ColorUtils.colorize("&7Página: &f" + (currentPage + 1) + "/" + finalTotalPages),
                "",
                ColorUtils.colorize("&aClique em uma recompensa"),
                ColorUtils.colorize("&apara coletar!")
        );
        gui.setButton(infoSlot, info);

        if (currentPage < totalPages - 1) {
            ItemButton nextPage = new ItemButton(Material.ARROW, ColorUtils.colorize("&aPróxima Página"));
            nextPage.setDefaultAction(e -> {
                e.setCancelled(true);
                new RewardsMenu(plugin, player, currentPage + 1).open();
            });
            gui.setButton(nextSlot, nextPage);
        }

        // Coletar todas
        if (!rewards.isEmpty()) {
            ItemButton claimAll = new ItemButton(Material.CHEST, 
                    ColorUtils.colorize("&a&lColetar Todas"),
                    ColorUtils.colorize("&7Clique para coletar todas"),
                    ColorUtils.colorize("&7as recompensas de uma vez!"),
                    "",
                    ColorUtils.colorize("&eRecompensas: &f" + rewards.size())
            );
            claimAll.setDefaultAction(e -> {
                e.setCancelled(true);
                int claimed = plugin.getRewardManager().claimAllRewards(player);
                if (claimed > 0) {
                    player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() + 
                            "&aVocê coletou &e" + claimed + " &arecompensas!"));
                    plugin.getMessageManager().playSound(player, "reward-claim");
                }
                player.closeInventory();
            });
            gui.setButton(claimAllSlot, claimAll);
        }

        // Voltar
        ItemButton back = new ItemButton(Material.BARRIER, ColorUtils.colorize("&cVoltar"));
        back.setDefaultAction(e -> {
            e.setCancelled(true);
            plugin.getMenuManager().openMainMenu(player);
        });
        gui.setButton(backSlot, back);
    }

    private ItemButton createRewardButton(PendingReward reward) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String date = sdf.format(new Date(reward.getTimestamp()));

        switch (reward.getType()) {
            case ITEM -> {
                ItemStack item = reward.getItem();
                material = item.getType();
                name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                        ? item.getItemMeta().getDisplayName() 
                        : "&f" + formatMaterialName(material);
                lore.add(ColorUtils.colorize("&7Tipo: &eItem"));
                lore.add(ColorUtils.colorize("&7Quantidade: &f" + item.getAmount()));
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    lore.add("");
                    lore.addAll(item.getItemMeta().getLore());
                }
            }
            case POINTS -> {
                material = Material.GOLD_INGOT;
                name = "&6&l" + reward.getPoints() + " Pontos";
                lore.add(ColorUtils.colorize("&7Tipo: &ePontos"));
                lore.add(ColorUtils.colorize("&7Quantidade: &6" + reward.getPoints()));
            }
            case EXPERIENCE -> {
                material = Material.EXPERIENCE_BOTTLE;
                name = "&a&l" + reward.getExperience() + " XP";
                lore.add(ColorUtils.colorize("&7Tipo: &eExperiência"));
                lore.add(ColorUtils.colorize("&7Quantidade: &a" + reward.getExperience()));
            }
            case COMMAND -> {
                // Usar informações de display se disponíveis
                material = reward.getDisplayMaterial() != null ? reward.getDisplayMaterial() : Material.COMMAND_BLOCK;
                name = reward.getDisplayName() != null ? reward.getDisplayName() : "&b&lRecompensa Especial";
                lore.add(ColorUtils.colorize("&7Tipo: &eRecompensa Especial"));
                if (reward.getDescription() != null && !reward.getDescription().isEmpty()) {
                    lore.add(ColorUtils.colorize(reward.getDescription()));
                }
            }
            default -> {
                material = Material.CHEST;
                name = "&fRecompensa";
            }
        }

        lore.add("");
        lore.add(ColorUtils.colorize("&7Dungeon: &f" + reward.getDungeonId()));
        lore.add(ColorUtils.colorize("&7Data: &f" + date));
        lore.add("");
        lore.add(ColorUtils.colorize("&aClique para coletar!"));

        ItemButton button = new ItemButton(material, ColorUtils.colorize(name), lore.toArray(new String[0]));
        
        button.setDefaultAction(e -> {
            e.setCancelled(true);
            if (plugin.getRewardManager().claimReward(player, reward.getId())) {
                player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() + 
                        "&aRecompensa coletada com sucesso!"));
                plugin.getMessageManager().playSound(player, "reward-claim");
                new RewardsMenu(plugin, player, currentPage).open();
            }
        });

        return button;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public void open() {
        gui.show(player);
    }
}
