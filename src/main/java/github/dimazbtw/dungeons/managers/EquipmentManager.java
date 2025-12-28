package github.dimazbtw.dungeons.managers;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.DungeonEquipment;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EquipmentManager {

    private final Main plugin;
    private final Map<UUID, DungeonEquipment> equipmentCache;
    private final File dataFolder;

    public EquipmentManager(Main plugin) {
        this.plugin = plugin;
        this.equipmentCache = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "equipment");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Obtém o equipamento de um jogador (carrega se necessário)
     */
    public DungeonEquipment getEquipment(UUID playerId) {
        return equipmentCache.computeIfAbsent(playerId, this::loadEquipment);
    }

    /**
     * Carrega equipamento do arquivo
     */
    private DungeonEquipment loadEquipment(UUID playerId) {
        File file = new File(dataFolder, playerId.toString() + ".yml");
        DungeonEquipment equipment = new DungeonEquipment(playerId);

        if (!file.exists()) {
            return equipment;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Carregar armadura
        if (config.contains("armor.helmet")) {
            equipment.setHelmet(config.getItemStack("armor.helmet"));
        }
        if (config.contains("armor.chestplate")) {
            equipment.setChestplate(config.getItemStack("armor.chestplate"));
        }
        if (config.contains("armor.leggings")) {
            equipment.setLeggings(config.getItemStack("armor.leggings"));
        }
        if (config.contains("armor.boots")) {
            equipment.setBoots(config.getItemStack("armor.boots"));
        }

        // Carregar itens extras
        if (config.contains("extras.item1")) {
            equipment.setExtraItem1(config.getItemStack("extras.item1"));
        }
        if (config.contains("extras.item2")) {
            equipment.setExtraItem2(config.getItemStack("extras.item2"));
        }

        return equipment;
    }

    /**
     * Salva equipamento no arquivo
     */
    public void saveEquipment(UUID playerId) {
        DungeonEquipment equipment = equipmentCache.get(playerId);
        if (equipment == null) return;

        File file = new File(dataFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // Salvar armadura
        config.set("armor.helmet", equipment.getHelmet());
        config.set("armor.chestplate", equipment.getChestplate());
        config.set("armor.leggings", equipment.getLeggings());
        config.set("armor.boots", equipment.getBoots());

        // Salvar itens extras
        config.set("extras.item1", equipment.getExtraItem1());
        config.set("extras.item2", equipment.getExtraItem2());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save equipment for " + playerId);
            e.printStackTrace();
        }
    }

    /**
     * Aplica o equipamento salvo ao jogador (para início de dungeon)
     */
    public void applyEquipment(Player player) {
        DungeonEquipment equipment = getEquipment(player.getUniqueId());

        // Aplicar armadura
        if (equipment.getHelmet() != null) {
            player.getInventory().setHelmet(equipment.getHelmet().clone());
        }
        if (equipment.getChestplate() != null) {
            player.getInventory().setChestplate(equipment.getChestplate().clone());
        }
        if (equipment.getLeggings() != null) {
            player.getInventory().setLeggings(equipment.getLeggings().clone());
        }
        if (equipment.getBoots() != null) {
            player.getInventory().setBoots(equipment.getBoots().clone());
        }

        // Aplicar itens extras (slots 1 e 2, pois 0 é a espada)
        if (equipment.getExtraItem1() != null) {
            player.getInventory().setItem(1, equipment.getExtraItem1().clone());
        }
        if (equipment.getExtraItem2() != null) {
            player.getInventory().setItem(2, equipment.getExtraItem2().clone());
        }
    }

    /**
     * Sincroniza o equipamento salvo com o inventário atual do jogador
     * Usado quando o jogador sai da dungeon SEM morrer
     * Isso atualiza as quantidades de itens consumíveis (maçãs, poções, etc)
     */
    public void syncEquipmentFromInventory(Player player) {
        DungeonEquipment equipment = getEquipment(player.getUniqueId());

        // Sincronizar armadura (verificar se ainda existe e está intacta)
        ItemStack currentHelmet = player.getInventory().getHelmet();
        ItemStack currentChest = player.getInventory().getChestplate();
        ItemStack currentLegs = player.getInventory().getLeggings();
        ItemStack currentBoots = player.getInventory().getBoots();

        // Se o jogador tinha armadura salva, atualizar com o estado atual
        if (equipment.getHelmet() != null) {
            equipment.setHelmet(currentHelmet); // null se foi destruída/removida
        }
        if (equipment.getChestplate() != null) {
            equipment.setChestplate(currentChest);
        }
        if (equipment.getLeggings() != null) {
            equipment.setLeggings(currentLegs);
        }
        if (equipment.getBoots() != null) {
            equipment.setBoots(currentBoots);
        }

        // Sincronizar itens extras (slots 1 e 2)
        // Verificar se os itens nos slots ainda são os mesmos tipos (ou foram consumidos)
        ItemStack slot1 = player.getInventory().getItem(1);
        ItemStack slot2 = player.getInventory().getItem(2);

        if (equipment.getExtraItem1() != null) {
            // Verificar se o item foi consumido ou ainda existe
            if (slot1 != null && isSameItemType(equipment.getExtraItem1(), slot1)) {
                // Atualizar com quantidade atual
                equipment.setExtraItem1(slot1);
            } else {
                // Item foi totalmente consumido ou substituído
                equipment.setExtraItem1(null);
            }
        }

        if (equipment.getExtraItem2() != null) {
            if (slot2 != null && isSameItemType(equipment.getExtraItem2(), slot2)) {
                equipment.setExtraItem2(slot2);
            } else {
                equipment.setExtraItem2(null);
            }
        }

        // Salvar as alterações
        saveEquipment(player.getUniqueId());
    }

    /**
     * Verifica se dois itens são do mesmo tipo (ignora quantidade)
     */
    private boolean isSameItemType(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        if (item1.getType() != item2.getType()) return false;

        // Verificar meta (nome, lore, encantamentos)
        if (item1.hasItemMeta() && item2.hasItemMeta()) {
            return item1.getItemMeta().equals(item2.getItemMeta());
        }

        return !item1.hasItemMeta() && !item2.hasItemMeta();
    }

    /**
     * Limpa o equipamento salvo (quando jogador morre na dungeon)
     */
    public void clearEquipment(UUID playerId) {
        DungeonEquipment equipment = equipmentCache.get(playerId);
        if (equipment != null) {
            equipment.clear();
            saveEquipment(playerId);
        }
    }

    /**
     * Atualiza um slot específico de armadura
     */
    public void setArmorSlot(UUID playerId, int slot, ItemStack item) {
        DungeonEquipment equipment = getEquipment(playerId);
        
        switch (slot) {
            case 0 -> equipment.setHelmet(item);
            case 1 -> equipment.setChestplate(item);
            case 2 -> equipment.setLeggings(item);
            case 3 -> equipment.setBoots(item);
        }
    }

    /**
     * Atualiza um slot de item extra
     */
    public void setExtraSlot(UUID playerId, int slot, ItemStack item) {
        DungeonEquipment equipment = getEquipment(playerId);
        
        if (slot == 0) {
            equipment.setExtraItem1(item);
        } else if (slot == 1) {
            equipment.setExtraItem2(item);
        }
    }

    /**
     * Carrega equipamento quando jogador entra
     */
    public void loadPlayer(UUID playerId) {
        equipmentCache.put(playerId, loadEquipment(playerId));
    }

    /**
     * Salva e remove do cache quando jogador sai
     */
    public void unloadPlayer(UUID playerId) {
        saveEquipment(playerId);
        equipmentCache.remove(playerId);
    }

    /**
     * Salva todos os equipamentos
     */
    public void saveAll() {
        for (UUID playerId : equipmentCache.keySet()) {
            saveEquipment(playerId);
        }
    }
}
