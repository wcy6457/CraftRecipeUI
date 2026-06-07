package com.github.wcy6457.recipeSmith;

import com.github.wcy6457.recipeSmith.command.RecipesCommand;
import com.github.wcy6457.recipeSmith.gui.RecipeGuiManager;
import com.github.wcy6457.recipeSmith.i18n.LanguageService;
import com.github.wcy6457.recipeSmith.service.RecipeService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class RecipeSmith extends JavaPlugin {
    private RecipeService recipeService;

    @Override
    public void onEnable() {
        LanguageService languageService = new LanguageService(this);
        languageService.load();
        recipeService = new RecipeService(this);
        RecipeGuiManager guiManager = new RecipeGuiManager(this, recipeService, languageService);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                "recipes",
                languageService.plain("command.description"),
                List.of("recipesmith"),
                new RecipesCommand(recipeService, guiManager, languageService, this)
        ));
        recipeService.reloadFromDisk();
    }

    @Override
    public void onDisable() {
        if (recipeService != null) {
            recipeService.shutdown();
        }
    }
}
