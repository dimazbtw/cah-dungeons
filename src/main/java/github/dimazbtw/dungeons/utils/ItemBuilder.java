package github.dimazbtw.dungeons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import github.dimazbtw.lib.utils.basics.ColorUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
        }
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(ColorUtils.colorize(line));
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchant, int level) {
        if (meta != null) {
            meta.addEnchant(enchant, level, true);
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemBuilder skullOwner(Player player) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            this.meta = skullMeta;
        }
        return this;
    }

    public ItemBuilder skullTexture(String texture) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            return this;
        }

        if (texture == null || texture.isEmpty()) {
            return this;
        }

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            String textureValue;

            // Check if it's already a base64 encoded texture
            if (texture.startsWith("eyJ")) {
                textureValue = texture;
            } else {
                // It's a texture hash, build the full JSON
                String textureUrl = "http://textures.minecraft.net/texture/" + texture;
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
                textureValue = Base64.getEncoder().encodeToString(json.getBytes());
            }

            profile.getProperties().put("textures", new Property("textures", textureValue));

            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);

            this.meta = skullMeta;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public ItemBuilder unbreakable() {
        if (meta != null) {
            meta.setUnbreakable(true);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
