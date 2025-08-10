package com.github.wcy6457.creatRecipeUI.ui;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import static com.github.wcy6457.creatRecipeUI.utlis.NamedUtil.named;

public final class AddRecipeUI extends View {

    private final CreatRecipeUI plugin;

    // 槽位布局
    private static final int[]  INGREDIENT_SLOTS   = {9,10,11,18,19,20,27,28,29};
    private static final int[]  ARROW_SLOT         = {21,22,23,24,25,5,15,33,41}; // 装饰箭头
    private static final int    RESULT_SLOT        = 26; // 结果物品
    private static final int    BACK_SLOT          = 45; // 返回按钮（左下角）
    private static final int    CONFIRM_SLOT       = 53; // 确认按钮（右下角）

    public AddRecipeUI(CreatRecipeUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.type(ViewType.CHEST);
        config.size(6);
        config.title(plugin.languageManager.get("gui.title_addRecipe"));
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        // 1) 背景填充（完全不可点击）
        ItemStack filler = named(new ItemStack(Material.RED_STAINED_GLASS_PANE), " ");
        for (int i = 0; i < 54; i++) {
            render.slot(i, filler).cancelOnClick();
        }

        // 2) 原料区（允许放/取物品：解取消）
        for (int slot : INGREDIENT_SLOTS) {
            render.slot(slot,new ItemStack(Material.AIR)).onClick(click -> click.setCancelled(false));

        }

        // 3) 装饰箭头（不可点击）
        ItemStack arrow = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        for (int slot : ARROW_SLOT){
            render.slot(slot, arrow).cancelOnClick();
        }

        // 4) 结果区（允许放/取物品：解取消）
        render.slot(RESULT_SLOT, new ItemStack(Material.AIR))
                .onClick(click -> click.setCancelled(false));

        // 5) 返回按钮（可点但不会被拿走：取消点击）
        ItemStack back = named(new ItemStack(Material.BARRIER), plugin.languageManager.get("gui.icon_back"));
        render.slot(BACK_SLOT, back).cancelOnClick().onClick(SlotContext::back);

        // 6) 确认按钮（可点但不会被拿走：取消点击）
        ItemStack confirm = named(new ItemStack(Material.LIME_STAINED_GLASS_PANE), plugin.languageManager.get("gui.confirm"));
        render.slot(CONFIRM_SLOT, confirm)
                .cancelOnClick()
                .onClick(click -> {
                    //TODO 具体实现
                    click.getPlayer().sendMessage("Confirm clicked (UI only)");
                });
    }
}
