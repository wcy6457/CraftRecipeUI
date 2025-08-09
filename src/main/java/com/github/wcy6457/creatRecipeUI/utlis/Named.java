package com.github.wcy6457.creatRecipeUI.utlis;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Named {
    /**
     * 复制传入ItemStack的ItemMeta后通过setDisplayName修改后重新设置传入的ItemStack
     * @param item 需要修改的ItemStack
     * @param name 需要修改成的名字
     * @return 修改后的ItemStack
     */
    public static ItemStack named (ItemStack item, String name){
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
