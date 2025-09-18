package com.arisweeping.core;

import com.arisweeping.data.ConfigData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置文件管理器
 * 负责配置的保存、加载和验证
 */
public class ConfigManager {
    
    private static final String CONFIG_DIR = "aris-sweeping";
    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    
    private static Path configPath;
    
    /**
     * 初始化配置管理器
     */
    public static void initialize() {
        try {
            // 获取配置目录路径 - 使用当前工作目录下的config文件夹
            Path configDir = Paths.get("config", CONFIG_DIR);
            
            // 确保配置目录存在
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                ArisLogger.info("创建配置目录: " + configDir.toString());
            }
            
            configPath = configDir.resolve(CONFIG_FILE);
            ArisLogger.info("配置文件路径: " + configPath.toString());
            
        } catch (IOException e) {
            ArisLogger.error("初始化配置管理器失败", e);
            throw new RuntimeException("配置管理器初始化失败", e);
        }
    }
    
    /**
     * 保存配置到文件
     */
    public static boolean saveConfig(ConfigData configData) {
        if (configPath == null) {
            ArisLogger.error("配置管理器未初始化");
            return false;
        }
        
        if (configData == null) {
            ArisLogger.error("配置数据为null，无法保存");
            return false;
        }
        
        try {
            // 验证配置数据
            if (!configData.validate()) {
                ArisLogger.warn("配置数据验证失败，使用默认配置保存");
                configData.resetToDefaults();
            }
            
            // 创建临时文件
            Path tempPath = Paths.get(configPath.toString() + ".tmp");
            
            // 将配置数据转换为JSON并写入临时文件
            String jsonContent = GSON.toJson(configData);
            try (FileWriter writer = new FileWriter(tempPath.toFile(), StandardCharsets.UTF_8)) {
                writer.write(jsonContent);
                writer.flush();
            }
            
            // 原子性替换原文件
            Files.move(tempPath, configPath);
            
            ArisLogger.info("配置已保存到: " + configPath.toString());
            return true;
            
        } catch (IOException e) {
            ArisLogger.error("保存配置失败", e);
            return false;
        }
    }
    
    /**
     * 从文件加载配置
     */
    public static ConfigData loadConfig() {
        if (configPath == null) {
            ArisLogger.error("配置管理器未初始化");
            return createDefaultConfig();
        }
        
        // 如果配置文件不存在，创建默认配置
        if (!Files.exists(configPath)) {
            ArisLogger.info("配置文件不存在，创建默认配置");
            ConfigData defaultConfig = createDefaultConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }
        
        try {
            // 读取配置文件
            String jsonContent;
            try (FileReader reader = new FileReader(configPath.toFile(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, length);
                }
                jsonContent = sb.toString();
            }
            
            // 解析JSON到配置对象
            ConfigData configData = GSON.fromJson(jsonContent, ConfigData.class);
            
            if (configData == null) {
                ArisLogger.warn("配置文件解析结果为null，使用默认配置");
                return createDefaultConfig();
            }
            
            // 验证配置数据
            if (!configData.validate()) {
                ArisLogger.warn("配置数据验证失败，重置为默认配置");
                configData.resetToDefaults();
                saveConfig(configData); // 保存修复后的配置
            }
            
            ArisLogger.info("成功加载配置文件");
            return configData;
            
        } catch (JsonParseException e) {
            ArisLogger.error("配置文件JSON解析失败，使用默认配置", e);
            return createDefaultConfig();
        } catch (IOException e) {
            ArisLogger.error("加载配置文件失败，使用默认配置", e);
            return createDefaultConfig();
        }
    }
    
    /**
     * 创建默认配置
     */
    private static ConfigData createDefaultConfig() {
        ConfigData config = new ConfigData();
        config.resetToDefaults();
        return config;
    }
    
    /**
     * 备份当前配置文件
     */
    public static boolean backupConfig() {
        if (configPath == null || !Files.exists(configPath)) {
            return false;
        }
        
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path backupPath = Paths.get(configPath.toString() + ".backup." + timestamp);
            Files.copy(configPath, backupPath);
            
            ArisLogger.info("配置文件已备份到: " + backupPath.toString());
            return true;
            
        } catch (IOException e) {
            ArisLogger.error("备份配置文件失败", e);
            return false;
        }
    }
    
    /**
     * 重置配置为默认值
     */
    public static ConfigData resetConfig() {
        try {
            // 备份现有配置
            backupConfig();
            
            // 创建默认配置
            ConfigData defaultConfig = createDefaultConfig();
            
            // 保存默认配置
            saveConfig(defaultConfig);
            
            ArisLogger.info("配置已重置为默认值");
            return defaultConfig;
            
        } catch (Exception e) {
            ArisLogger.error("重置配置失败", e);
            return createDefaultConfig();
        }
    }
    
    /**
     * 获取配置文件路径
     */
    public static String getConfigPath() {
        return configPath != null ? configPath.toString() : "未初始化";
    }
    
    /**
     * 检查配置文件是否存在
     */
    public static boolean configExists() {
        return configPath != null && Files.exists(configPath);
    }
}