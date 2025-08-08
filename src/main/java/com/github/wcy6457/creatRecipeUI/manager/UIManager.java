package com.github.wcy6457.creatRecipeUI.manager;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import com.github.wcy6457.creatRecipeUI.ui.Menu;
import me.devnatan.inventoryframework.ViewFrame;

public class UIManager {
    private final CreatRecipeUI plugin;
    public ViewFrame menu;

    public UIManager(CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    public void registerUI() {
        this.menu = ViewFrame.create(this.plugin)
                .with(new Menu())
                .register();

    }
}
