package com.xzavier0722.mc.plugin.slimeglue;

import com.xzavier0722.mc.plugin.slimefuncomplib.ICompatibleSlimefun;
import com.xzavier0722.mc.plugin.slimeglue.listener.BlockListener;
import com.xzavier0722.mc.plugin.slimeglue.listener.PluginListener;
import com.xzavier0722.mc.plugin.slimeglue.listener.SlimefunCompListener;
import com.xzavier0722.mc.plugin.slimeglue.listener.SlimefunListener;
import com.xzavier0722.mc.plugin.slimeglue.manager.CompatibilityModuleManager;
import com.xzavier0722.mc.plugin.slimeglue.module.*;
import com.xzavier0722.mc.plugin.slimeglue.slimefun.GlueProtectionModule;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SlimeGlue extends JavaPlugin implements SlimefunAddon {

    private static SlimeGlue instance;
    private static GlueLogger logger;
    private static CompatibilityModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;
        logger = new GlueLogger(getLogger());
        logger.i("====SlimeGlue Start====");
        moduleManager = new CompatibilityModuleManager();

        logger.i("- Loading modules...");
        registerModules();
        moduleManager().load();

        logger.i("- Registering listeners...");
        getServer().getPluginManager().registerEvents(new PluginListener(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginManager().registerEvents(new SlimefunListener(), this);
        if (isCompSlimefun()) {
            logger.i("- Compatible Slimefun detected, enabling advanced features...");
            loadCompOnlyFeatures();
        }

        logger.i("- Registering protection module...");
        if (!registerSfProtectionModule()) {
            logger.w("- Failed to register protection module, schedule the retry task after the server started.");
            AtomicInteger counter = new AtomicInteger();
            Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
                if (registerSfProtectionModule()) {
                    logger.i("Protection module is registered!");
                    task.cancel();
                    return;
                }
                if (counter.getAndIncrement() >= 10) {
                    logger.e("Failed to register the slimefun protection module, some function may not work properly");
                    task.cancel();
                }
            }, 50, 20 * 50, TimeUnit.MILLISECONDS);
        }

        logger.i("- SlimeGlue Started!");
        logger.i("=======================");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/Xzavier0722/SlimeGlue/issues";
    }

    public static SlimeGlue instance() {
        return instance;
    }

    public static GlueLogger logger() {
        return logger;
    }

    public static CompatibilityModuleManager moduleManager() {
        return moduleManager;
    }

    private void registerModules() {
        moduleManager().register(new KingdomsXModule());
        moduleManager().register(new MagicModule());
        moduleManager().register(new QuickShopModule());
        moduleManager().register(new QuickShopHikariModule());
        moduleManager().register(new LocketteProModule());
    }

    private boolean registerSfProtectionModule() {
        ProtectionManager pm = Slimefun.getProtectionManager();
        if (pm == null) {
            return false;
        }
        pm.registerModule(getServer().getPluginManager(), "SlimeGlue", (p) -> new GlueProtectionModule());
        return true;
    }

    private boolean isCompSlimefun() {
        try {
            return ICompatibleSlimefun.class.isInstance(Slimefun.instance());
        } catch (Throwable _ignore) {
            return false;
        }
    }

    private void loadCompOnlyFeatures() {
        getServer().getPluginManager().registerEvents(new SlimefunCompListener(), this);
    }

}
