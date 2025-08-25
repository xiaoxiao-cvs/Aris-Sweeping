package com.xiaoxiao.arissweeping;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.handler.EntityCleanupHandler;
import com.xiaoxiao.arissweeping.handler.LivestockDensityHandler;
import com.xiaoxiao.arissweeping.command.CleanupCommand;
import com.xiaoxiao.arissweeping.command.CleanupTabCompleter;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class ArisSweeping extends JavaPlugin {
    public static final String PLUGIN_ID = "arissweeping";
    private static ArisSweeping instance;
    private static Logger logger;
    
    private EntityCleanupHandler cleanupHandler;
    private LivestockDensityHandler livestockHandler;
    private ModConfig config;
    private PermissionManager permissionManager;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置
        config = new ModConfig(this);
        
        // 验证配置文件
        try {
            config.reload(); // 这会触发配置验证和错误通知
            logger.info("配置文件加载和验证完成");
        } catch (Exception e) {
            logger.severe("配置文件验证失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 初始化权限管理器
        permissionManager = new PermissionManager(this);
        
        // 初始化实体清理处理器
        try {
            cleanupHandler = new EntityCleanupHandler(this);
            cleanupHandler.init();
            logger.info("Entity cleanup handler initialized successfully!");
        } catch (Exception e) {
            logger.severe("Failed to initialize Entity Cleanup Handler: " + e.getMessage());
            e.printStackTrace();
            // 创建一个最小功能的处理器以避免完全失败
            cleanupHandler = null;
        }
        
        // 初始化畜牧业密度处理器
        try {
            livestockHandler = new LivestockDensityHandler(this, config);
            
            // 如果启用了畜牧业密度检测，启动检查
            if (config.isLivestockDensityCheckEnabled()) {
                livestockHandler.startLivestockMonitoring();
            }
            logger.info("Livestock density handler initialized successfully!");
        } catch (Exception e) {
            logger.severe("Failed to initialize Livestock Density Handler: " + e.getMessage());
            e.printStackTrace();
            livestockHandler = null;
        }
        
        // 注册命令
        try {
            CleanupCommand commandExecutor = new CleanupCommand(this);
            CleanupTabCompleter tabCompleter = new CleanupTabCompleter(this);
            getCommand("arissweeping").setExecutor(commandExecutor);
            getCommand("arissweeping").setTabCompleter(tabCompleter);
            logger.info("Commands registered successfully!");
        } catch (Exception e) {
            logger.severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 注册事件监听器
        try {
            if (cleanupHandler != null) {
                getServer().getPluginManager().registerEvents(cleanupHandler, this);
                logger.info("Event listeners registered successfully!");
            } else {
                logger.warning("CleanupHandler is null, events not registered. Plugin will run with limited functionality.");
            }
        } catch (Exception e) {
            logger.severe("Failed to register event listeners: " + e.getMessage());
            e.printStackTrace();
        }
        
        logger.info("Aris Sweeping plugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // 停止清理任务
        try {
            if (cleanupHandler != null) {
                cleanupHandler.shutdown();
                logger.info("Entity cleanup handler shutdown successfully!");
            }
        } catch (Exception e) {
            logger.severe("Error shutting down cleanup handler: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 停止畜牧业密度检查
        try {
            if (livestockHandler != null) {
                livestockHandler.stopDensityCheck();
                logger.info("Livestock density handler shutdown successfully!");
            }
        } catch (Exception e) {
            logger.severe("Error shutting down livestock handler: " + e.getMessage());
            e.printStackTrace();
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
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public LivestockDensityHandler getLivestockHandler() {
        return livestockHandler;
    }
    
    /**
     * 检查主清理是否正在运行
     */
    public boolean isMainCleanupRunning() {
        return cleanupHandler != null && cleanupHandler.getTaskStatusInfo().contains("清理进行中: 是");
    }
}