package com.github.wcy6457.creatRecipeUI.manager;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import com.github.wcy6457.creatRecipeUI.ui.AddRecipeUI;
import com.github.wcy6457.creatRecipeUI.ui.LanguageUI;
import com.github.wcy6457.creatRecipeUI.ui.Menu;
import me.devnatan.inventoryframework.ViewFrame;

public class UIManager {
    private final CreatRecipeUI plugin;
    public final ViewFrame viewFrame;

    public UIManager(CreatRecipeUI plugin) {
        this.plugin = plugin;
        this.viewFrame = ViewFrame.create(this.plugin);
    }

    public void registerUI() {
        this.viewFrame.with(new Menu(plugin) , new AddRecipeUI(plugin) , new LanguageUI(plugin)).register();
    }
}
