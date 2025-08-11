package com.github.wcy6457.creatRecipeUI;

import com.github.wcy6457.creatRecipeUI.command.CommandHandler;
import com.github.wcy6457.creatRecipeUI.command.CruTabCompleter;
import com.github.wcy6457.creatRecipeUI.manager.RecipeManager;
import com.github.wcy6457.creatRecipeUI.manager.LanguageManager;
import com.github.wcy6457.creatRecipeUI.manager.UIManager;
import com.github.wcy6457.creatRecipeUI.utlis.Icon_HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreatRecipeUI extends JavaPlugin {

    public LanguageManager languageManager;
    public UIManager uiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 初始化语言系统
        this.languageManager = new LanguageManager(this);
        this.languageManager.load(getConfig().getString("language", "en_us"));

        Bukkit.getLogger().info("§7—————————————————————————————————————————");
        Bukkit.getLogger().info("§e" + this.languageManager.get("log.plugin_onEnable"));
        Bukkit.getLogger().info("§7—————————————————————————————————————————");

        // 初始化合成系统
        RecipeManager rm = new RecipeManager(this);
        rm.registerRecipe();

        // 初始化 UI 系统
        this.uiManager = new UIManager(this);
        this.uiManager.registerUI();

        // 初始化头颅缓存系统（异步）
        this.getLogger().info(this.languageManager.get("log.headIcon_onInit"));
        Icon_HeadUtil.init(this);

        // 注册命令 cru
        PluginCommand cruCommand = getCommand("cru");
        if (cruCommand != null) {
            cruCommand.setExecutor(new CommandHandler(this));
            cruCommand.setTabCompleter(new CruTabCompleter());
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("§7—————————————————————————————————————————");
        Bukkit.getLogger().info("§e" + this.languageManager.get("log.plugin_onDisable"));
        Bukkit.getLogger().info("§7—————————————————————————————————————————");

        Icon_HeadUtil.clearCache(); // 清理缓存，释放内存
    }
}