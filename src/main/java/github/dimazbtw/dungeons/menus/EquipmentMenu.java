package github.dimazbtw.dungeons.menus;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonEquipment;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EquipmentMenu implements InventoryHolder {

    private final Main plugin;
    private final Player player;
    private final Inventory inventory;
    private final FileConfiguration config;

    // Slots configuráveis
    private final int helmetSlot;
    private final int chestplateSlot;
    private final int leggingsSlot;
    private final int bootsSlot;
    private final int extra1Slot;
    private final int extra2Slot;
    private final int clearSlot;
    private final int backSlot;
    private final int infoSlot;

    private final Set<Integer> equipmentSlots;

    public EquipmentMenu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.config = plugin.getMenuManager().getMenuConfig("equipment");

        // Carregar slots da config ou usar padrões
        ConfigurationSection slotsSection = config != null ? config.getConfigurationSection("menu.slots") : null;
        
        this.helmetSlot = slotsSection != null ? slotsSection.getInt("helmet", 10) : 10;
        this.chestplateSlot = slotsSection != null ? slotsSection.getInt("chestplate", 19) : 19;
        this.leggingsSlot = slotsSection != null ? slotsSection.getInt("leggings", 28) : 28;
        this.bootsSlot = slotsSection != null ? slotsSection.getInt("boots", 37) : 37;
        this.extra1Slot = slotsSection != null ? slotsSection.getInt("extra1", 15) : 15;
        this.extra2Slot = slotsSection != null ? slotsSection.getInt("extra2", 24) : 24;

        // Carregar slots de botões
        ConfigurationSection buttonsSection = config != null ? config.getConfigurationSection("menu.buttons") : null;
        this.clearSlot = buttonsSection != null ? buttonsSection.getInt("clear.slot", 40) : 40;
        this.backSlot = buttonsSection != null ? buttonsSection.getInt("back.slot", 36) : 36;
        this.infoSlot = buttonsSection != null ? buttonsSection.getInt("info.slot", 4) : 4;

        this.equipmentSlots = new HashSet<>(Arrays.asList(
                helmetSlot, chestplateSlot, leggingsSlot, bootsSlot, extra1Slot, extra2Slot
        ));

        String title = config != null ? config.getString("menu.title", "&8&lEquipamento de Dungeon") : "&8&lEquipamento de Dungeon";
        int size = config != null ? config.getInt("menu.size", 45) : 45;

        this.inventory = Bukkit.createInventory(this, size, ColorUtils.colorize(title));
        setupMenu();
    }

    private void setupMenu() {
        // Sem borda - apenas limpar inventário
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // Carregar equipamento salvo
        loadSavedEquipment();

        // Marcadores visuais para slots vazios
        setupSlotPlaceholders();

        // Botão Info
        inventory.setItem(infoSlot, createInfoButton());

        // Botão Limpar
        inventory.setItem(clearSlot, createClearButton());

        // Botão Voltar
        inventory.setItem(backSlot, createBackButton());
    }

    private void setupSlotPlaceholders() {
        DungeonEquipment equipment = plugin.getEquipmentManager().getEquipment(player.getUniqueId());

        // Só mostrar placeholder se slot estiver vazio
        if (equipment.getHelmet() == null) {
            inventory.setItem(helmetSlot, createPlaceholder("&e&lCapacete", "&7Clique em um capacete no", "&7seu inventário para equipar"));
        }
        if (equipment.getChestplate() == null) {
            inventory.setItem(chestplateSlot, createPlaceholder("&e&lPeitoral", "&7Clique em um peitoral no", "&7seu inventário para equipar"));
        }
        if (equipment.getLeggings() == null) {
            inventory.setItem(leggingsSlot, createPlaceholder("&e&lCalças", "&7Clique em calças no", "&7seu inventário para equipar"));
        }
        if (equipment.getBoots() == null) {
            inventory.setItem(bootsSlot, createPlaceholder("&e&lBotas", "&7Clique em botas no", "&7seu inventário para equipar"));
        }
        if (equipment.getExtraItem1() == null) {
            inventory.setItem(extra1Slot, createPlaceholder("&6&lItem Extra 1", "&7Clique em qualquer item no", "&7seu inventário para equipar"));
        }
        if (equipment.getExtraItem2() == null) {
            inventory.setItem(extra2Slot, createPlaceholder("&6&lItem Extra 2", "&7Clique em qualquer item no", "&7seu inventário para equipar"));
        }
    }

    private ItemStack createPlaceholder(String name, String... lore) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtils.colorize(line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void loadSavedEquipment() {
        DungeonEquipment equipment = plugin.getEquipmentManager().getEquipment(player.getUniqueId());

        if (equipment.getHelmet() != null) {
            inventory.setItem(helmetSlot, equipment.getHelmet().clone());
        }
        if (equipment.getChestplate() != null) {
            inventory.setItem(chestplateSlot, equipment.getChestplate().clone());
        }
        if (equipment.getLeggings() != null) {
            inventory.setItem(leggingsSlot, equipment.getLeggings().clone());
        }
        if (equipment.getBoots() != null) {
            inventory.setItem(bootsSlot, equipment.getBoots().clone());
        }
        if (equipment.getExtraItem1() != null) {
            inventory.setItem(extra1Slot, equipment.getExtraItem1().clone());
        }
        if (equipment.getExtraItem2() != null) {
            inventory.setItem(extra2Slot, equipment.getExtraItem2().clone());
        }
    }

    private ItemStack createInfoButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&e&lEquipamento de Dungeon"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ColorUtils.colorize("&7Configure o equipamento que você"));
            lore.add(ColorUtils.colorize("&7irá usar nas dungeons."));
            lore.add("");
            lore.add(ColorUtils.colorize("&a&lCOMO USAR:"));
            lore.add(ColorUtils.colorize("&7Clique em um item no seu inventário"));
            lore.add(ColorUtils.colorize("&7para equipá-lo automaticamente!"));
            lore.add("");
            lore.add(ColorUtils.colorize("&c&lATENÇÃO:"));
            lore.add(ColorUtils.colorize("&cSe você morrer na dungeon,"));
            lore.add(ColorUtils.colorize("&cperde todo este equipamento!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createClearButton() {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&c&lLimpar Tudo"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Clique para remover todo"));
            lore.add(ColorUtils.colorize("&7o equipamento configurado."));
            lore.add("");
            lore.add(ColorUtils.colorize("&eOs itens voltam ao seu inventário!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&cVoltar"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();

        // Clique no inventário do jogador - equipar item
        if (slot >= inventory.getSize()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                equipItem(clickedItem, event.getSlot());
            }
            return;
        }

        // Clique nos slots de equipamento - remover item
        if (equipmentSlots.contains(slot)) {
            ItemStack currentItem = inventory.getItem(slot);
            if (currentItem != null && currentItem.getType() != Material.AIR && !isPlaceholder(currentItem)) {
                removeEquipmentFromSlot(slot, currentItem);
            }
            return;
        }

        // Botões
        if (slot == clearSlot) {
            clearAllEquipment();
        } else if (slot == backSlot) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
                plugin.getMenuManager().openMainMenu(player), 1L);
        }
    }

    private boolean isPlaceholder(ItemStack item) {
        return item.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    }

    private void equipItem(ItemStack item, int playerSlot) {
        DungeonEquipment equipment = plugin.getEquipmentManager().getEquipment(player.getUniqueId());
        String typeName = item.getType().name();
        int targetSlot = -1;

        // Determinar slot baseado no tipo de item
        if (isHelmet(typeName) && equipment.getHelmet() == null) {
            targetSlot = helmetSlot;
            equipment.setHelmet(item);
        } else if (isChestplate(typeName) && equipment.getChestplate() == null) {
            targetSlot = chestplateSlot;
            equipment.setChestplate(item);
        } else if (typeName.endsWith("_LEGGINGS") && equipment.getLeggings() == null) {
            targetSlot = leggingsSlot;
            equipment.setLeggings(item);
        } else if (typeName.endsWith("_BOOTS") && equipment.getBoots() == null) {
            targetSlot = bootsSlot;
            equipment.setBoots(item);
        } else {
            // Item extra - vai para o primeiro slot vazio
            if (equipment.getExtraItem1() == null) {
                targetSlot = extra1Slot;
                equipment.setExtraItem1(item);
            } else if (equipment.getExtraItem2() == null) {
                targetSlot = extra2Slot;
                equipment.setExtraItem2(item);
            }
        }

        if (targetSlot == -1) {
            player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                    "&cNão há slot disponível para este item!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Remover do inventário do jogador e colocar no menu
        player.getInventory().setItem(playerSlot, null);
        inventory.setItem(targetSlot, item.clone());

        // Salvar automaticamente
        plugin.getEquipmentManager().saveEquipment(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1.2f);
    }

    private void removeEquipmentFromSlot(int slot, ItemStack item) {
        DungeonEquipment equipment = plugin.getEquipmentManager().getEquipment(player.getUniqueId());

        // Limpar do modelo
        if (slot == helmetSlot) {
            equipment.setHelmet(null);
        } else if (slot == chestplateSlot) {
            equipment.setChestplate(null);
        } else if (slot == leggingsSlot) {
            equipment.setLeggings(null);
        } else if (slot == bootsSlot) {
            equipment.setBoots(null);
        } else if (slot == extra1Slot) {
            equipment.setExtraItem1(null);
        } else if (slot == extra2Slot) {
            equipment.setExtraItem2(null);
        }

        // Devolver ao jogador
        giveItemToPlayer(item);

        // Colocar placeholder no lugar
        inventory.setItem(slot, getPlaceholderForSlot(slot));

        // Salvar automaticamente
        plugin.getEquipmentManager().saveEquipment(player.getUniqueId());

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 0.8f);
    }

    private ItemStack getPlaceholderForSlot(int slot) {
        if (slot == helmetSlot) {
            return createPlaceholder("&e&lCapacete", "&7Clique em um capacete no", "&7seu inventário para equipar");
        } else if (slot == chestplateSlot) {
            return createPlaceholder("&e&lPeitoral", "&7Clique em um peitoral no", "&7seu inventário para equipar");
        } else if (slot == leggingsSlot) {
            return createPlaceholder("&e&lCalças", "&7Clique em calças no", "&7seu inventário para equipar");
        } else if (slot == bootsSlot) {
            return createPlaceholder("&e&lBotas", "&7Clique em botas no", "&7seu inventário para equipar");
        } else if (slot == extra1Slot) {
            return createPlaceholder("&6&lItem Extra 1", "&7Clique em qualquer item no", "&7seu inventário para equipar");
        } else if (slot == extra2Slot) {
            return createPlaceholder("&6&lItem Extra 2", "&7Clique em qualquer item no", "&7seu inventário para equipar");
        }
        return null;
    }

    private void clearAllEquipment() {
        DungeonEquipment equipment = plugin.getEquipmentManager().getEquipment(player.getUniqueId());

        // Devolver todos os itens
        if (equipment.getHelmet() != null) {
            giveItemToPlayer(equipment.getHelmet());
        }
        if (equipment.getChestplate() != null) {
            giveItemToPlayer(equipment.getChestplate());
        }
        if (equipment.getLeggings() != null) {
            giveItemToPlayer(equipment.getLeggings());
        }
        if (equipment.getBoots() != null) {
            giveItemToPlayer(equipment.getBoots());
        }
        if (equipment.getExtraItem1() != null) {
            giveItemToPlayer(equipment.getExtraItem1());
        }
        if (equipment.getExtraItem2() != null) {
            giveItemToPlayer(equipment.getExtraItem2());
        }

        // Limpar equipamento
        equipment.clear();
        plugin.getEquipmentManager().saveEquipment(player.getUniqueId());

        // Atualizar menu
        setupMenu();

        player.sendMessage(ColorUtils.colorize(plugin.getMessageManager().getPrefix() +
                "&cEquipamento de dungeon limpo!"));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
    }

    private boolean isHelmet(String typeName) {
        return typeName.endsWith("_HELMET") || typeName.equals("PLAYER_HEAD")
                || typeName.equals("ZOMBIE_HEAD") || typeName.equals("SKELETON_SKULL")
                || typeName.equals("CREEPER_HEAD") || typeName.equals("DRAGON_HEAD")
                || typeName.equals("TURTLE_HELMET") || typeName.equals("CARVED_PUMPKIN");
    }

    private boolean isChestplate(String typeName) {
        return typeName.endsWith("_CHESTPLATE") || typeName.equals("ELYTRA");
    }

    private void giveItemToPlayer(ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item.clone());
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void handleClose(InventoryCloseEvent event) {
        // Salvar ao fechar (garantia extra)
        plugin.getEquipmentManager().saveEquipment(player.getUniqueId());
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
