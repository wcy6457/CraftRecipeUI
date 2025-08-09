package com.github.wcy6457.creatRecipeUI.ui;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.jetbrains.annotations.NotNull;

public class AddRecipeUI extends View {
    private final CreatRecipeUI plugin;

    public AddRecipeUI (CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.title(plugin.languageManager.get("gui.title_addRecipeUI"));
        config.size(3);
        config.type(ViewType.CHEST);
        config.cancelOnClick();
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {

    }

    @Override
    public void onClick(@NotNull SlotClickContext click) {

    }

    @Override
    public void onClose(@NotNull CloseContext close) {

    }
}
