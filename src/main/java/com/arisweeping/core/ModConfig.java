package com.arisweeping.core;

import com.arisweeping.data.ConfigData;

/**
 * 模组配置管理器
 * 
 * 负责管理模组的配置初始化和加载
 */
public class ModConfig {
    private static boolean initialized = false;
    private static volatile ConfigData configData = null;
    
    /**
     * 初始化配置系统
     */
    public static void initialize() {
        if (initialized) {
            ArisLogger.warn("ModConfig 已经初始化，跳过...");
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            ArisLogger.logStartupPhase("CONFIG", "正在初始化模组配置系统...");
            
            // 初始化配置管理器
            ConfigManager.initialize();
            
            // 加载配置数据
            loadConfig();
            
            initialized = true;
            
            long duration = System.currentTimeMillis() - startTime;
            ArisLogger.logStartupSuccess("配置系统", duration);
        } catch (Exception e) {
            ArisLogger.logStartupFailure("配置系统", e);
            throw new RuntimeException("配置系统初始化失败", e);
        }
    }
    
    /**
     * 加载配置数据
     */
    private static void loadConfig() {
        try {
            configData = ConfigManager.loadConfig();
            ArisLogger.logConfigStatus("配置加载", true, "配置文件路径: " + ConfigManager.getConfigPath());
        } catch (Exception e) {
            ArisLogger.logConfigStatus("配置加载", false, e.getMessage());
            configData = new ConfigData(); // 使用默认配置
        }
    }
    
    /**
     * 获取配置数据实例
     */
    public static ConfigData getConfig() {
        if (configData == null) {
            ArisLogger.warn("在初始化前获取配置，返回默认配置");
            return new ConfigData();
        }
        return configData;
    }
    
    /**
     * 更新配置数据
     */
    public static void updateConfig(ConfigData newConfig) {
        if (newConfig == null) {
            ArisLogger.error("尝试使用 null 数据更新配置");
            return;
        }
        
        configData = newConfig;
        
        // 保存配置到文件
        if (ConfigManager.saveConfig(configData)) {
            ArisLogger.info("配置已更新并保存");
        } else {
            ArisLogger.warn("配置已更新但保存失败");
        }
    }
    
    /**
     * 保存当前配置到文件
     */
    public static boolean saveConfig() {
        if (configData == null) {
            ArisLogger.warn("配置数据为null，无法保存");
            return false;
        }
        
        return ConfigManager.saveConfig(configData);
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
        ArisLogger.info("重新加载模组配置...");
        loadConfig();
    }
    
    /**
     * 重置配置为默认值
     */
    public static ConfigData resetToDefaults() {
        ArisLogger.info("重置配置为默认值...");
        configData = ConfigManager.resetConfig();
        return configData;
    }
}