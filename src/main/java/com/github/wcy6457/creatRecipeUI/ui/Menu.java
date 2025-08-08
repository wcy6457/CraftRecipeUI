package com.github.wcy6457.creatRecipeUI.ui;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import me.devnatan.inventoryframework.*;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public final class Menu extends View {

    private final CreatRecipeUI plugin;

    public Menu(CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.title(plugin.languageManager.get("gui.title_menu"));
        config.size(3);
        config.type(ViewType.CHEST);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {

        render.slot(1, new ItemStack(Material.BOOK)); // 中心位放一本书作为“占位符”
    }

    @Override
    public void onClose(@NotNull CloseContext close) {
        // 可根据需要做保存/清理
        // 如需阻止关闭：close.setCancelled(true);
    }
}
