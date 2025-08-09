package com.github.wcy6457.creatRecipeUI.utlis;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Head {
    public static ItemStack getHead () {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        //todo
        if (meta != null) {
            head.setItemMeta(meta);
        }
        return head;
    }
}
