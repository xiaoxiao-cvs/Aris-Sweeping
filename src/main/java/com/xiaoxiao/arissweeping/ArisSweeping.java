package com.xiaoxiao.arissweeping;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.handler.EntityCleanupHandler;
import com.xiaoxiao.arissweeping.handler.LivestockDensityHandler;
import com.xiaoxiao.arissweeping.command.CleanupCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class ArisSweeping extends JavaPlugin {
    public static final String PLUGIN_ID = "arissweeping";
    private static ArisSweeping instance;
    private static Logger logger;
    
    private EntityCleanupHandler cleanupHandler;
    private LivestockDensityHandler livestockHandler;
    private ModConfig config;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置
        config = new ModConfig(this);
        
        // 初始化实体清理处理器
        cleanupHandler = new EntityCleanupHandler(this);
        cleanupHandler.init();
        
        // 初始化畜牧业密度处理器
        livestockHandler = new LivestockDensityHandler(this, config);
        
        // 如果启用了畜牧业密度检测，启动检查
        if (config.isLivestockDensityCheckEnabled()) {
            livestockHandler.startDensityCheck();
        }
        
        // 注册命令
        CleanupCommand commandExecutor = new CleanupCommand(this);
        getCommand("arissweeping").setExecutor(commandExecutor);
        getCommand("arissweeping").setTabCompleter(commandExecutor);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(cleanupHandler, this);
        
        logger.info("Aris Sweeping plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // 停止清理任务
        if (cleanupHandler != null) {
            cleanupHandler.shutdown();
        }
        
        // 停止畜牧业密度检查
        if (livestockHandler != null) {
            livestockHandler.stopDensityCheck();
        }
        
        logger.info("Aris Sweeping plugin disabled!");
    }
    
    public static ArisSweeping getInstance() {
        return instance;
    }
    
    public static Logger getPluginLogger() {
        return logger;
    }
    
    public ModConfig getModConfig() {
        return config;
    }
    
    public EntityCleanupHandler getCleanupHandler() {
        return cleanupHandler;
    }
    
    public LivestockDensityHandler getLivestockHandler() {
        return livestockHandler;
    }
}