package com.github.wcy6457.creatRecipeUI.ui;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import me.devnatan.inventoryframework.*;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import static com.github.wcy6457.creatRecipeUI.utlis.Named.named;

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
        config.cancelOnClick();
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {

        render.slot(0 , named(new ItemStack(Material.CRAFTING_TABLE) , plugin.languageManager.get("gui.icon_addRecipe")))
                .onClick(click -> {
                    click.openForPlayer(AddRecipeUI.class);
        });
    }

    @Override
    public void onClick(@NotNull SlotClickContext click) {

    }

    @Override
    public void onClose(@NotNull CloseContext close) {

    }
}
