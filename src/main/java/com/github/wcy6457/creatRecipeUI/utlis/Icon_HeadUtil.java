package com.github.wcy6457.creatRecipeUI.utlis;

import com.github.wcy6457.creatRecipeUI.CreatRecipeUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.*;

public final class Icon_HeadUtil {

    // 缓存：每个枚举对应一个“已写入OwnerProfile、无标题”的基础头颅
    private static final Map<PLAYER_HEAD, ItemStack> CACHE = new ConcurrentHashMap<>();
    // 防抖：防止重复并发构建同一个头
    private static final Set<PLAYER_HEAD> LOADING = ConcurrentHashMap.newKeySet();
    // 插件句柄（仅用于调度）
    private static volatile CreatRecipeUI plugin;

    private Icon_HeadUtil() {}

    // 在 onEnable 调用，启动异步预热
    public static void init(CreatRecipeUI plugin) {

        Icon_HeadUtil.plugin = Objects.requireNonNull(plugin, "plugin");
        preloadAsync();
    }

    // 预热所有枚举值的基础头（异步->主线程构建）
    public static void preloadAsync() {
        if (plugin == null) return;
        for (PLAYER_HEAD p : PLAYER_HEAD.values()) {
            ensureLoadedAsync(p);
        }
    }

    // 原入口：不改签名与调用方式
    public static ItemStack getHead(PLAYER_HEAD p, String displayName) {
        // 命中缓存：克隆 + 设置显示名
        ItemStack base = CACHE.get(p);
        if (base != null) {
            return withDisplayName(base.clone(), displayName);
        }

        // 缓存未就绪：触发后台加载（若未触发过），返回一个“临时空白玩家头”（不阻塞主线程）
        ensureLoadedAsync(p);

        ItemStack fallback = new ItemStack(Material.PLAYER_HEAD);
        return withDisplayName(fallback, displayName);
    }

    // 仅设置显示名的小工具
    private static ItemStack withDisplayName(ItemStack item, String displayName) {
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null && displayName != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }

    // 异步触发加载，主线程安全构建基础头
    private static void ensureLoadedAsync(PLAYER_HEAD p) {
        if (plugin == null) return;
        if (CACHE.containsKey(p)) return;
        if (!LOADING.add(p)) return; // 已在加载中

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 在主线程安全构建真正的基础头
                ItemStack built = Bukkit.getScheduler()
                        .callSyncMethod(plugin, () -> buildBaseHeadSync(p))
                        .get(10, TimeUnit.SECONDS);
                if (built != null) {
                    CACHE.put(p, built);
                }
            } catch (TimeoutException te) {
                plugin.getLogger().warning("[Icon_HeadUtil] 构建头颅超时: " + p.name());
            } catch (Exception e) {
                plugin.getLogger().warning("[Icon_HeadUtil] 构建头颅失败: " + p.name() + " -> " + e.getMessage());
            } finally {
                LOADING.remove(p);
            }
        });
    }

    // 必须在主线程调用：Bukkit API 不保证线程安全
    private static ItemStack buildBaseHeadSync(PLAYER_HEAD p) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), null);
            PlayerTextures textures = profile.getTextures();
            URL skinUrl = p.getURL();
            if (skinUrl != null) {
                textures.setSkin(skinUrl);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            }
        } catch (Throwable t) {
            // 避免阻断；记录日志，返回无皮肤的基础头
            if (plugin != null) {
                plugin.getLogger().warning("[Icon_HeadUtil] 写入皮肤失败: " + p.name() + " -> " + t.getMessage());
            }
        }

        head.setItemMeta(meta);
        return head;
    }

    // 可选：关闭/重载时清理
    public static void clearCache() {
        CACHE.clear();
        LOADING.clear();
    }
}