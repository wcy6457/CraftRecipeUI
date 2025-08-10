package com.github.wcy6457.creatRecipeUI.utlis;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.util.UUID;


public class Icon_HeadUtil {
    public static ItemStack getHead(PLAYER_HEAD p, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        meta.setDisplayName(displayName);

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), null);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(p.getURL()); // skinURL 应该是 Mojang 的皮肤链接或 base64 数据
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Throwable t) {
            t.printStackTrace(); // 打印错误信息有助于调试
            return head;
        }

        head.setItemMeta(meta);
        return head;
    }
}


