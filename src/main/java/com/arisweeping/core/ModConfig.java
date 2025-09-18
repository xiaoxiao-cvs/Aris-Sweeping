package com.arisweeping.core;

import com.arisweeping.data.ConfigData;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 模组配置管理器
 * 
 * 负责管理模组的配置初始化和加载
 */
public class ModConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;
    
    /**
     * 初始化配置系统
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("ModConfig already initialized, skipping...");
            return;
        }
        
        try {
            LOGGER.info("Initializing mod configuration...");
            
            // 这里将在后续实现配置文件的读取和初始化
            // 目前只是标记为已初始化
            initialized = true;
            
            LOGGER.info("Mod configuration initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize mod configuration", e);
            throw new RuntimeException("Configuration initialization failed", e);
        }
    }
    
    /**
     * 检查配置是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 重新加载配置
     */
    public static void reload() {
        LOGGER.info("Reloading mod configuration...");
        initialized = false;
        initialize();
    }
}