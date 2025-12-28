package github.dimazbtw.dungeons.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Armazena o equipamento que o jogador leva para dungeons
 * 4 slots de armadura + 2 slots de itens extras
 */
public class DungeonEquipment {

    private final UUID playerId;
    
    // Armadura (slots 0-3: helmet, chestplate, leggings, boots)
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    
    // Itens extras (2 slots livres)
    private ItemStack extraItem1;
    private ItemStack extraItem2;

    public DungeonEquipment(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    // ============ ARMADURA ============

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet != null ? helmet.clone() : null;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate != null ? chestplate.clone() : null;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings != null ? leggings.clone() : null;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots != null ? boots.clone() : null;
    }

    // ============ ITENS EXTRAS ============

    public ItemStack getExtraItem1() {
        return extraItem1;
    }

    public void setExtraItem1(ItemStack extraItem1) {
        this.extraItem1 = extraItem1 != null ? extraItem1.clone() : null;
    }

    public ItemStack getExtraItem2() {
        return extraItem2;
    }

    public void setExtraItem2(ItemStack extraItem2) {
        this.extraItem2 = extraItem2 != null ? extraItem2.clone() : null;
    }

    // ============ UTILITÁRIOS ============

    /**
     * Retorna array com toda armadura [helmet, chest, leggings, boots]
     */
    public ItemStack[] getArmorContents() {
        return new ItemStack[] { boots, leggings, chestplate, helmet };
    }

    /**
     * Define toda armadura de uma vez
     */
    public void setArmorContents(ItemStack[] armor) {
        if (armor == null || armor.length < 4) return;
        this.boots = armor[0] != null ? armor[0].clone() : null;
        this.leggings = armor[1] != null ? armor[1].clone() : null;
        this.chestplate = armor[2] != null ? armor[2].clone() : null;
        this.helmet = armor[3] != null ? armor[3].clone() : null;
    }

    /**
     * Retorna array com itens extras
     */
    public ItemStack[] getExtraItems() {
        return new ItemStack[] { extraItem1, extraItem2 };
    }

    /**
     * Verifica se tem algum equipamento definido
     */
    public boolean hasAnyEquipment() {
        return helmet != null || chestplate != null || leggings != null || boots != null
                || extraItem1 != null || extraItem2 != null;
    }

    /**
     * Limpa todo o equipamento
     */
    public void clear() {
        this.helmet = null;
        this.chestplate = null;
        this.leggings = null;
        this.boots = null;
        this.extraItem1 = null;
        this.extraItem2 = null;
    }

    /**
     * Cria uma cópia do equipamento
     */
    public DungeonEquipment clone() {
        DungeonEquipment copy = new DungeonEquipment(playerId);
        copy.helmet = helmet != null ? helmet.clone() : null;
        copy.chestplate = chestplate != null ? chestplate.clone() : null;
        copy.leggings = leggings != null ? leggings.clone() : null;
        copy.boots = boots != null ? boots.clone() : null;
        copy.extraItem1 = extraItem1 != null ? extraItem1.clone() : null;
        copy.extraItem2 = extraItem2 != null ? extraItem2.clone() : null;
        return copy;
    }
}
