package github.dimazbtw.dungeons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

public class EquipmentParser {

    /**
     * Parse equipment string to ItemStack
     * Formats:
     * - MATERIAL (e.g., "DIAMOND_SWORD")
     * - MATERIAL:#HEXCOLOR (e.g., "LEATHER_CHESTPLATE:#FF0000")
     * - TEXTURE_HASH (64 char hex for skull texture)
     */
    public static ItemStack parseEquipment(String equipmentString) {
        if (equipmentString == null || equipmentString.isEmpty()) {
            return null;
        }

        equipmentString = equipmentString.trim();

        // Check if it's a skull texture (64 char hex hash)
        if (isTextureHash(equipmentString)) {
            return createSkullWithTexture(equipmentString);
        }

        // Check for color format (MATERIAL:#COLOR)
        if (equipmentString.contains(":#")) {
            return parseColoredArmor(equipmentString);
        }

        // Regular material
        try {
            Material material = Material.valueOf(equipmentString.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isTextureHash(String str) {
        // Texture hashes are 64 character hex strings
        if (str.length() != 64) return false;
        return str.matches("[a-fA-F0-9]+");
    }

    private static ItemStack createSkullWithTexture(String textureHash) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta == null) return skull;

        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            String textureUrl = "http://textures.minecraft.net/texture/" + textureHash;
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
            String encodedTexture = Base64.getEncoder().encodeToString(json.getBytes());

            profile.getProperties().put("textures", new Property("textures", encodedTexture));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

            skull.setItemMeta(meta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return skull;
    }

    private static ItemStack parseColoredArmor(String equipmentString) {
        String[] parts = equipmentString.split(":#");
        if (parts.length != 2) return null;

        String materialStr = parts[0].trim();
        String colorStr = parts[1].trim();

        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Check if it's leather armor
        if (!materialStr.toUpperCase().startsWith("LEATHER_")) {
            return new ItemStack(material);
        }

        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();

        if (meta != null) {
            Color color = parseColor(colorStr);
            if (color != null) {
                meta.setColor(color);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private static Color parseColor(String colorStr) {
        // Remove # if present
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }

        try {
            // Parse hex color
            int rgb = Integer.parseInt(colorStr, 16);
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            return Color.fromRGB(red, green, blue);
        } catch (NumberFormatException e) {
            // Try named colors
            return switch (colorStr.toUpperCase()) {
                case "RED" -> Color.RED;
                case "GREEN" -> Color.GREEN;
                case "BLUE" -> Color.BLUE;
                case "WHITE" -> Color.WHITE;
                case "BLACK" -> Color.BLACK;
                case "YELLOW" -> Color.YELLOW;
                case "ORANGE" -> Color.ORANGE;
                case "PURPLE" -> Color.PURPLE;
                case "GRAY", "GREY" -> Color.GRAY;
                case "AQUA", "CYAN" -> Color.AQUA;
                case "LIME" -> Color.LIME;
                case "MAROON" -> Color.MAROON;
                case "NAVY" -> Color.NAVY;
                case "OLIVE" -> Color.OLIVE;
                case "SILVER" -> Color.SILVER;
                case "TEAL" -> Color.TEAL;
                case "FUCHSIA" -> Color.FUCHSIA;
                default -> null;
            };
        }
    }
}
