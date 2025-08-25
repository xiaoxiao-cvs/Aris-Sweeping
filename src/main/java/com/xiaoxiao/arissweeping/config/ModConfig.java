package com.xiaoxiao.arissweeping.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    // 配置缓存，减少重复的配置文件访问
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_REFRESH_INTERVAL = 30000; // 30秒缓存刷新间隔
    
    public ModConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadDefaults();
        refreshCache();
    }
    
    /**
     * 刷新配置缓存
     */
    private void refreshCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_REFRESH_INTERVAL) {
            configCache.clear();
            lastCacheUpdate = currentTime;
            plugin.getLogger().fine("配置缓存已刷新");
        }
    }
    
    /**
     * 从缓存获取配置值，如果缓存中没有则从配置文件读取并缓存
     */
    @SuppressWarnings("unchecked")
    private <T> T getCachedValue(String path, T defaultValue, Class<T> type) {
        refreshCache();
        
        Object cachedValue = configCache.get(path);
        if (cachedValue != null && type.isInstance(cachedValue)) {
            return (T) cachedValue;
        }
        
        T value;
        try {
            if (type == Boolean.class) {
                value = (T) Boolean.valueOf(config.getBoolean(path, (Boolean) defaultValue));
            } else if (type == Integer.class) {
                value = (T) Integer.valueOf(config.getInt(path, (Integer) defaultValue));
            } else if (type == Double.class) {
                value = (T) Double.valueOf(config.getDouble(path, (Double) defaultValue));
            } else if (type == String.class) {
                value = (T) config.getString(path, (String) defaultValue);
            } else {
                value = defaultValue;
            }
            
            configCache.put(path, value);
            return value;
        } catch (Exception e) {
            plugin.getLogger().warning("获取配置 " + path + " 时发生错误，使用默认值: " + e.getMessage());
            return defaultValue;
        }
    }
    
    private void loadDefaults() {
        try {
            // 基础设置
            config.addDefault("general.cleanupInterval", 300);
            
            // 实体清理设置
            config.addDefault("entity_cleanup.cleanupItems", true);
            config.addDefault("entity_cleanup.cleanupExperienceOrbs", true);
            config.addDefault("entity_cleanup.cleanupArrows", true);
            config.addDefault("entity_cleanup.cleanupFallingBlocks", true);
            config.addDefault("entity_cleanup.cleanupHostileMobs", false);
            config.addDefault("entity_cleanup.cleanupPassiveMobs", false);
            
            // 清理阈值设置
            config.addDefault("thresholds.maxItemsPerChunk", 50);
            config.addDefault("thresholds.maxEntitiesPerChunk", 100);
            config.addDefault("thresholds.itemAgeThreshold", 60);
            
            // 实体最小年龄设置
            config.addDefault("entity_age.itemMinAge", 100);
            config.addDefault("entity_age.experienceOrbMinAge", 100);
            config.addDefault("entity_age.arrowMinAge", 100);
            
            // 性能设置
            config.addDefault("performance.asyncCleanup", true);
            config.addDefault("performance.maxChunksPerTick", 5);
            config.addDefault("performance.batchSize", 100);
            config.addDefault("performance.batchDelay", 10);
            
            // 消息设置
            config.addDefault("messages.broadcastCleanup", true);
            config.addDefault("messages.showCleanupStats", true);
            
            // 畜牧业管理设置
            config.addDefault("livestock.enableDensityCheck", true);
            config.addDefault("livestock.maxAnimalsPerChunk", 20);
            config.addDefault("livestock.warningTime", 5);
            config.addDefault("livestock.enableWarning", true);
            
            // 区域解析功能设置
            config.addDefault("livestock.regions.enabled", true);
            config.addDefault("livestock.regions.playerTitle", "老师");
            
            // 默认区域配置示例
            config.addDefault("livestock.regions.areas.example.name", "示例学院自管区");
            config.addDefault("livestock.regions.areas.example.world", "world");
            config.addDefault("livestock.regions.areas.example.x1", -1000);
            config.addDefault("livestock.regions.areas.example.z1", -1000);
            config.addDefault("livestock.regions.areas.example.x2", 1000);
            config.addDefault("livestock.regions.areas.example.z2", 1000);
            
            // 中文生物名称映射
            config.addDefault("livestock.animalNames.COW", "牛");
            config.addDefault("livestock.animalNames.PIG", "猪");
            config.addDefault("livestock.animalNames.SHEEP", "羊");
            config.addDefault("livestock.animalNames.CHICKEN", "鸡");
            config.addDefault("livestock.animalNames.HORSE", "马");
            config.addDefault("livestock.animalNames.DONKEY", "驴");
            config.addDefault("livestock.animalNames.MULE", "骡子");
            config.addDefault("livestock.animalNames.LLAMA", "羊驼");
            config.addDefault("livestock.animalNames.RABBIT", "兔子");
            config.addDefault("livestock.animalNames.WOLF", "狼");
            config.addDefault("livestock.animalNames.CAT", "猫");
            config.addDefault("livestock.animalNames.PARROT", "鹦鹉");
            config.addDefault("livestock.animalNames.BEE", "蜜蜂");
            config.addDefault("livestock.animalNames.FOX", "狐狸");
            config.addDefault("livestock.animalNames.PANDA", "熊猫");
            config.addDefault("livestock.animalNames.POLAR_BEAR", "北极熊");
            config.addDefault("livestock.animalNames.TURTLE", "海龟");
            config.addDefault("livestock.animalNames.OCELOT", "豹猫");
            config.addDefault("livestock.animalNames.MOOSHROOM", "哞菇");
            config.addDefault("livestock.animalNames.GOAT", "山羊");
            config.addDefault("livestock.animalNames.AXOLOTL", "美西螈");
            config.addDefault("livestock.animalNames.GLOW_SQUID", "发光鱿鱼");
            config.addDefault("livestock.animalNames.SQUID", "鱿鱼");
            config.addDefault("livestock.animalNames.BAT", "蝙蝠");
            config.addDefault("livestock.animalNames.VILLAGER", "村民");
            config.addDefault("livestock.animalNames.WANDERING_TRADER", "流浪商人");
            config.addDefault("livestock.animalNames.IRON_GOLEM", "铁傀儡");
            config.addDefault("livestock.animalNames.SNOW_GOLEM", "雪傀儡");
            config.addDefault("livestock.animalNames.ALLAY", "悦灵");
            config.addDefault("livestock.animalNames.FROG", "青蛙");
            config.addDefault("livestock.animalNames.TADPOLE", "蝌蚪");
            config.addDefault("livestock.animalNames.CAMEL", "骆驼");
            config.addDefault("livestock.animalNames.SNIFFER", "嗅探兽");
            config.addDefault("livestock.animalNames.ARMADILLO", "犰狳");
            
            // 实体密度设置
            config.addDefault("entity_density.threshold", 1500);
            
            // TPS监控设置
            config.addDefault("tps_monitor.enabled", true);
            config.addDefault("tps_monitor.lowTpsThreshold", 17.0);
            config.addDefault("tps_monitor.emergencyCleanup", true);
            
            // 聚集清理设置
            config.addDefault("cluster_cleanup.enabled", true);
            config.addDefault("cluster_cleanup.detectionDistance", 3.0);
            config.addDefault("cluster_cleanup.minClusterSize", 5);
            config.addDefault("cluster_cleanup.preserveRatio", 0.3);
            config.addDefault("cluster_cleanup.onlyCountSameType", true);
            
            // 智能清理设置
            config.addDefault("smart_cleanup.enabled", true);
            config.addDefault("smart_cleanup.interval", 60);
            config.addDefault("smart_cleanup.protectNamedEntities", true);
            config.addDefault("smart_cleanup.protectLeashedEntities", true);
            config.addDefault("smart_cleanup.protectRiddenEntities", true);
            config.addDefault("smart_cleanup.spawnReasonFilter", false);
            
            // 畜牧业监控设置
            config.addDefault("livestock.checkInterval", 300);
            
            // 全局设置
            config.addDefault("global.enabled", false);
            config.addDefault("global.debug", false);
            
            config.options().copyDefaults(true);
            plugin.saveConfig();
            
            plugin.getLogger().info("配置默认值加载成功");
        } catch (Exception e) {
            plugin.getLogger().severe("加载配置默认值时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void reload() {
        try {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            
            // 验证配置文件
            validateConfigurationAndNotify();
            
            plugin.getLogger().info("配置文件重新加载成功");
        } catch (Exception e) {
            plugin.getLogger().severe("重新加载配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            // 向管理员公屏通知配置错误
            notifyConfigError("配置文件重新加载失败: " + e.getMessage());
        }
    }
    
    // 基础设置
    public int getCleanupInterval() {
        int interval = getCachedValue("general.cleanupInterval", 300, Integer.class);
        return Math.max(interval, 10); // 确保最小值为10秒
    }
    
    public void setCleanupInterval(int interval) {
        try {
            config.set("general.cleanupInterval", interval);
            plugin.saveConfig();
            configCache.put("general.cleanupInterval", interval); // 更新缓存
        } catch (Exception e) {
            plugin.getLogger().severe("保存清理间隔配置时发生错误: " + e.getMessage());
        }
    }
    
    // 实体清理设置的setter方法
    public void setCleanupItems(boolean enabled) {
        try {
            config.set("entity_cleanup.cleanupItems", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存物品清理配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setCleanupExperienceOrbs(boolean enabled) {
        try {
            config.set("entity_cleanup.cleanupExperienceOrbs", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存经验球清理配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setCleanupArrows(boolean enabled) {
        try {
            config.set("entity_cleanup.cleanupArrows", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存箭矢清理配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setCleanupFallingBlocks(boolean enabled) {
        try {
            config.set("entity_cleanup.cleanupFallingBlocks", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存掉落方块清理配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setCleanupHostileMobs(boolean enabled) {
        try {
            config.set("entity_cleanup.cleanupHostileMobs", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存敌对生物清理配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setCleanupPassiveMobs(boolean enabled) {
        try {
            config.set("entity_cleanup.cleanupPassiveMobs", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存被动生物清理配置时发生错误: " + e.getMessage());
        }
    }
    
    // 实体清理设置
    public boolean isCleanupItems() {
        return getCachedValue("entity_cleanup.cleanupItems", true, Boolean.class);
    }
    
    public boolean isCleanupExperienceOrbs() {
        return getCachedValue("entity_cleanup.cleanupExperienceOrbs", true, Boolean.class);
    }
    
    public boolean isCleanupArrows() {
        return getCachedValue("entity_cleanup.cleanupArrows", true, Boolean.class);
    }
    
    public boolean isCleanupFallingBlocks() {
        return getCachedValue("entity_cleanup.cleanupFallingBlocks", true, Boolean.class);
    }
    
    public boolean isCleanupHostileMobs() {
        return getCachedValue("entity_cleanup.cleanupHostileMobs", false, Boolean.class);
    }
    
    public boolean isCleanupPassiveMobs() {
        try {
            return config.getBoolean("entity_cleanup.cleanupPassiveMobs", false);
        } catch (Exception e) {
            plugin.getLogger().warning("获取被动生物清理配置时发生错误，使用默认值: " + e.getMessage());
            return false;
        }
    }
    
    // 清理阈值设置
    public int getMaxItemsPerChunk() {
        try {
            int value = config.getInt("thresholds.maxItemsPerChunk", 50);
            return Math.max(value, 1); // 确保最小值为1
        } catch (Exception e) {
            plugin.getLogger().warning("获取每区块最大物品数配置时发生错误，使用默认值: " + e.getMessage());
            return 50;
        }
    }
    
    public int getMaxEntitiesPerChunk() {
        try {
            int value = config.getInt("thresholds.maxEntitiesPerChunk", 100);
            return Math.max(value, 1); // 确保最小值为1
        } catch (Exception e) {
            plugin.getLogger().warning("获取每区块最大实体数配置时发生错误，使用默认值: " + e.getMessage());
            return 100;
        }
    }
    
    public int getItemAgeThreshold() {
        int value = getCachedValue("thresholds.itemAgeThreshold", 60, Integer.class);
        return Math.max(value, 10); // 确保最小值为10秒
    }
    
    // 清理阈值设置的setter方法
    public void setMaxItemsPerChunk(int value) {
        try {
            config.set("thresholds.maxItemsPerChunk", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存每区块最大物品数配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setMaxEntitiesPerChunk(int value) {
        try {
            config.set("thresholds.maxEntitiesPerChunk", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存每区块最大实体数配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setItemAgeThreshold(int value) {
        try {
            config.set("thresholds.itemAgeThreshold", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存物品年龄阈值配置时发生错误: " + e.getMessage());
        }
    }
    
    // 性能设置
    public boolean isAsyncCleanup() {
        try {
            return config.getBoolean("performance.asyncCleanup", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取异步清理配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public int getMaxChunksPerTick() {
        try {
            int value = config.getInt("performance.maxChunksPerTick", 5);
            return Math.max(value, 1); // 确保最小值为1
        } catch (Exception e) {
            plugin.getLogger().warning("获取每tick最大区块数配置时发生错误，使用默认值: " + e.getMessage());
            return 5;
        }
    }
    
    public int getBatchSize() {
        try {
            int value = config.getInt("performance.batchSize", 100);
            return Math.max(value, 1); // 确保最小值为1
        } catch (Exception e) {
            plugin.getLogger().warning("获取批处理大小配置时发生错误，使用默认值: " + e.getMessage());
            return 100;
        }
    }
    
    public int getBatchDelay() {
        try {
            int value = config.getInt("performance.batchDelay", 10);
            return Math.max(value, 0); // 确保最小值为0
        } catch (Exception e) {
            plugin.getLogger().warning("获取批处理延迟配置时发生错误，使用默认值: " + e.getMessage());
            return 10;
        }
    }
    
    // 性能设置的setter方法
    public void setAsyncCleanup(boolean enabled) {
        try {
            config.set("performance.asyncCleanup", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存异步清理配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setMaxChunksPerTick(int value) {
        try {
            config.set("performance.maxChunksPerTick", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存每tick最大区块数配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setBatchSize(int value) {
        try {
            config.set("performance.batchSize", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存批处理大小配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setBatchDelay(int value) {
        try {
            config.set("performance.batchDelay", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存批处理延迟配置时发生错误: " + e.getMessage());
        }
    }
    
    // 畜牧业管理配置
    public boolean isLivestockDensityCheckEnabled() {
        try {
            return config.getBoolean("livestock.enableDensityCheck", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取畜牧业密度检查配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public int getMaxAnimalsPerChunk() {
        try {
            int value = config.getInt("livestock.maxAnimalsPerChunk", 20);
            return Math.max(value, 1); // 确保最小值为1
        } catch (Exception e) {
            plugin.getLogger().warning("获取每区块最大动物数配置时发生错误，使用默认值: " + e.getMessage());
            return 20;
        }
    }
    
    public int getWarningTime() {
        try {
            int value = config.getInt("livestock.warningTime", 5);
            return Math.max(value, 1); // 确保最小值为1分钟
        } catch (Exception e) {
            plugin.getLogger().warning("获取预警时间配置时发生错误，使用默认值: " + e.getMessage());
            return 5;
        }
    }
    
    public boolean isWarningEnabled() {
        try {
            return config.getBoolean("livestock.enableWarning", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取预警启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public int getWarningCooldown() {
        try {
            int value = config.getInt("livestock.warningCooldown", 60);
            return Math.max(value, 10); // 确保最小值为10秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取警告冷却时间配置时发生错误，使用默认值: " + e.getMessage());
            return 60;
        }
    }
    
    // 区域解析功能配置
    public boolean isRegionParsingEnabled() {
        try {
            return config.getBoolean("livestock.regions.enabled", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域解析功能配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public String getPlayerTitle() {
        try {
            return config.getString("livestock.regions.playerTitle", "老师");
        } catch (Exception e) {
            plugin.getLogger().warning("获取玩家称呼配置时发生错误，使用默认值: " + e.getMessage());
            return "老师";
        }
    }
    
    public String getChineseAnimalName(String entityType) {
        try {
            return config.getString("livestock.animalNames." + entityType, entityType);
        } catch (Exception e) {
            plugin.getLogger().warning("获取中文动物名称时发生错误，使用英文名: " + e.getMessage());
            return entityType;
        }
    }
    
    public java.util.Set<String> getRegionKeys() {
        try {
            org.bukkit.configuration.ConfigurationSection regionsSection = config.getConfigurationSection("livestock.regions.areas");
            if (regionsSection != null) {
                return regionsSection.getKeys(false);
            }
            return new java.util.HashSet<>();
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域配置键时发生错误: " + e.getMessage());
            return new java.util.HashSet<>();
        }
    }
    
    public String getRegionName(String regionKey) {
        try {
            return config.getString("livestock.regions.areas." + regionKey + ".name", regionKey);
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域名称时发生错误，使用键名: " + e.getMessage());
            return regionKey;
        }
    }
    
    public String getRegionWorld(String regionKey) {
        try {
            return config.getString("livestock.regions.areas." + regionKey + ".world", "world");
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域世界时发生错误，使用默认值: " + e.getMessage());
            return "world";
        }
    }
    
    public int getRegionX1(String regionKey) {
        try {
            return config.getInt("livestock.regions.areas." + regionKey + ".x1", 0);
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域X1坐标时发生错误，使用默认值: " + e.getMessage());
            return 0;
        }
    }
    
    public int getRegionZ1(String regionKey) {
        try {
            return config.getInt("livestock.regions.areas." + regionKey + ".z1", 0);
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域Z1坐标时发生错误，使用默认值: " + e.getMessage());
            return 0;
        }
    }
    
    public int getRegionX2(String regionKey) {
        try {
            return config.getInt("livestock.regions.areas." + regionKey + ".x2", 0);
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域X2坐标时发生错误，使用默认值: " + e.getMessage());
            return 0;
        }
    }
    
    public int getRegionZ2(String regionKey) {
        try {
            return config.getInt("livestock.regions.areas." + regionKey + ".z2", 0);
        } catch (Exception e) {
            plugin.getLogger().warning("获取区域Z2坐标时发生错误，使用默认值: " + e.getMessage());
            return 0;
        }
    }
    
    // 畜牧业管理配置的setter方法
    public void setLivestockDensityCheckEnabled(boolean enabled) {
        try {
            config.set("livestock.enableDensityCheck", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存畜牧业密度检查配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setMaxAnimalsPerChunk(int value) {
        try {
            config.set("livestock.maxAnimalsPerChunk", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存每区块最大动物数配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setWarningTime(int value) {
        try {
            config.set("livestock.warningTime", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存预警时间配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setWarningEnabled(boolean enabled) {
        try {
            config.set("livestock.enableWarning", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存预警启用配置时发生错误: " + e.getMessage());
        }
    }
    
    // 全局设置
    public boolean isPluginEnabled() {
        try {
            return config.getBoolean("global.enabled", false);
        } catch (Exception e) {
            plugin.getLogger().warning("获取插件启用状态配置时发生错误，使用默认值: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isDebugMode() {
        try {
            return config.getBoolean("global.debug", false);
        } catch (Exception e) {
            plugin.getLogger().warning("获取调试模式配置时发生错误，使用默认值: " + e.getMessage());
            return false;
        }
    }
    
    // 全局设置的setter方法
    public void setPluginEnabled(boolean enabled) {
        try {
            config.set("global.enabled", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存插件启用状态配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setDebugMode(boolean enabled) {
        try {
            config.set("global.debug", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存调试模式配置时发生错误: " + e.getMessage());
        }
    }
    
    // 消息设置
    public boolean isBroadcastCleanup() {
        try {
            return config.getBoolean("messages.broadcastCleanup", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取清理消息广播配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public boolean isShowCleanupStats() {
        try {
            return config.getBoolean("messages.showCleanupStats", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取清理统计显示配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    // 消息设置的setter方法
    public void setBroadcastCleanup(boolean enabled) {
        try {
            config.set("messages.broadcastCleanup", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存清理消息广播配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setShowCleanupStats(boolean enabled) {
        try {
            config.set("messages.showCleanupStats", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存清理统计显示配置时发生错误: " + e.getMessage());
        }
    }
    
    // TPS监控配置
    public boolean isTpsMonitorEnabled() {
        try {
            return config.getBoolean("tps_monitor.enabled", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取TPS监控启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public double getLowTpsThreshold() {
        try {
            double value = config.getDouble("tps_monitor.lowTpsThreshold", 17.0);
            return Math.max(value, 5.0); // 确保最小值为5.0
        } catch (Exception e) {
            plugin.getLogger().warning("获取低TPS阈值配置时发生错误，使用默认值: " + e.getMessage());
            return 17.0;
        }
    }
    
    public boolean isEmergencyCleanupEnabled() {
        try {
            return config.getBoolean("tps_monitor.emergencyCleanup", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取紧急清理启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    // TPS监控配置的setter方法
    public void setTpsMonitorEnabled(boolean enabled) {
        try {
            config.set("tps_monitor.enabled", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存TPS监控启用配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setLowTpsThreshold(double value) {
        try {
            config.set("tps_monitor.lowTpsThreshold", value);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存低TPS阈值配置时发生错误: " + e.getMessage());
        }
    }
    
    public void setEmergencyCleanupEnabled(boolean enabled) {
        try {
            config.set("tps_monitor.emergencyCleanup", enabled);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存紧急清理启用配置时发生错误: " + e.getMessage());
        }
    }
    
    // 聚集清理配置
    public boolean isClusterCleanupEnabled() {
        try {
            return config.getBoolean("cluster_cleanup.enabled", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取聚集清理启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public double getClusterDetectionDistance() {
        try {
            double value = config.getDouble("cluster_cleanup.detectionDistance", 3.0);
            return Math.max(value, 1.0); // 确保最小值为1.0
        } catch (Exception e) {
            plugin.getLogger().warning("获取聚集检测距离配置时发生错误，使用默认值: " + e.getMessage());
            return 3.0;
        }
    }
    
    public int getMinClusterSize() {
        try {
            int value = config.getInt("cluster_cleanup.minClusterSize", 5);
            return Math.max(value, 2); // 确保最小值为2
        } catch (Exception e) {
            plugin.getLogger().warning("获取最小聚集大小配置时发生错误，使用默认值: " + e.getMessage());
            return 5;
        }
    }
    
    public double getClusterPreserveRatio() {
        try {
            double value = config.getDouble("cluster_cleanup.preserveRatio", 0.3);
            return Math.max(0.0, Math.min(value, 1.0)); // 确保值在0.0-1.0之间
        } catch (Exception e) {
            plugin.getLogger().warning("获取聚集保留比例配置时发生错误，使用默认值: " + e.getMessage());
            return 0.3;
        }
    }
    
    public boolean isOnlyCountSameType() {
        try {
            return config.getBoolean("cluster_cleanup.onlyCountSameType", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取同类型聚集配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    // 智能清理配置
    public boolean isSmartCleanupEnabled() {
        try {
            return config.getBoolean("smart_cleanup.enabled", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取智能清理启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public int getSmartCleanupInterval() {
        try {
            int value = config.getInt("smart_cleanup.interval", 60);
            return Math.max(value, 10); // 确保最小值为10秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取智能清理间隔配置时发生错误，使用默认值: " + e.getMessage());
            return 60;
        }
    }
    
    public int getLivestockCheckInterval() {
        try {
            int interval = config.getInt("livestock.checkInterval", 300);
            return Math.max(interval, 30); // 确保最小值为30秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取畜牧业检查间隔配置时发生错误，使用默认值: " + e.getMessage());
            return 300;
        }
    }
    
    public boolean isProtectNamedEntities() {
        try {
            return config.getBoolean("smart_cleanup.protectNamedEntities", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取保护命名实体配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public boolean isProtectLeashedEntities() {
        try {
            return config.getBoolean("smart_cleanup.protectLeashedEntities", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取保护拴绳实体配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public boolean isProtectRiddenEntities() {
        try {
            return config.getBoolean("smart_cleanup.protectRiddenEntities", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取保护骑乘实体配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public boolean isSpawnReasonFilterEnabled() {
        try {
            return config.getBoolean("smart_cleanup.spawnReasonFilter", false);
        } catch (Exception e) {
            plugin.getLogger().warning("获取生成原因过滤配置时发生错误，使用默认值: " + e.getMessage());
            return false;
        }
    }
    
    // 实体密度配置
    public int getEntityDensityThreshold() {
        int threshold = getCachedValue("entity_density.threshold", 500, Integer.class);
        return Math.max(threshold, 50); // 确保最小值为50
    }
    
    // 实体密度配置的setter方法
    public void setEntityDensityThreshold(int threshold) {
        try {
            config.set("entity_density.threshold", threshold);
            plugin.saveConfig();
            configCache.put("entity_density.threshold", threshold); // 更新缓存
        } catch (Exception e) {
            plugin.getLogger().severe("保存实体密度阈值配置时发生错误: " + e.getMessage());
        }
    }
    
    // 基础配置读取方法
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    public String getString(String path, String defaultValue) {
        return getCachedValue(path, defaultValue, String.class);
    }
    
    // 畜牧业区域配置
    public boolean isLivestockRegionsEnabled() {
        return getCachedValue("livestock.regions.enabled", false, Boolean.class);
    }
    
    public String getLivestockPlayerTitle() {
        try {
            return config.getString("livestock.regions.playerTitle", "老师");
        } catch (Exception e) {
            plugin.getLogger().warning("获取畜牧业玩家称呼配置时发生错误，使用默认值: " + e.getMessage());
            return "老师";
        }
    }
    
    /**
     * 获取畜牧业区域配置
     */
    public Map<String, Map<String, Object>> getLivestockRegionAreas() {
        try {
            Map<String, Map<String, Object>> areas = new HashMap<>();
            
            if (config.isConfigurationSection("livestock.regions.areas")) {
                var areasSection = config.getConfigurationSection("livestock.regions.areas");
                if (areasSection != null) {
                    for (String areaKey : areasSection.getKeys(false)) {
                        if (areasSection.isConfigurationSection(areaKey)) {
                            var areaSection = areasSection.getConfigurationSection(areaKey);
                            if (areaSection != null) {
                                Map<String, Object> areaData = new HashMap<>();
                                areaData.put("name", areaSection.getString("name", areaKey));
                                areaData.put("world", areaSection.getString("world", "world"));
                                areaData.put("x1", areaSection.getInt("x1", 0));
                                areaData.put("z1", areaSection.getInt("z1", 0));
                                areaData.put("x2", areaSection.getInt("x2", 0));
                                areaData.put("z2", areaSection.getInt("z2", 0));
                                areas.put(areaKey, areaData);
                            }
                        }
                    }
                }
            }
            
            return areas;
        } catch (Exception e) {
            plugin.getLogger().warning("获取畜牧业区域配置时发生错误: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 根据坐标查找所属区域
     */
    public String findRegionByCoordinates(String worldName, int x, int z) {
        if (!isLivestockRegionsEnabled()) {
            return null;
        }
        
        Map<String, Map<String, Object>> areas = getLivestockRegionAreas();
        
        for (Map.Entry<String, Map<String, Object>> entry : areas.entrySet()) {
            Map<String, Object> areaData = entry.getValue();
            String areaWorld = (String) areaData.get("world");
            
            // 检查世界是否匹配
            if (!worldName.equals(areaWorld)) {
                continue;
            }
            
            int x1 = (Integer) areaData.get("x1");
            int z1 = (Integer) areaData.get("z1");
            int x2 = (Integer) areaData.get("x2");
            int z2 = (Integer) areaData.get("z2");
            
            // 确保坐标顺序正确
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            
            // 检查坐标是否在区域内
            if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                return (String) areaData.get("name");
            }
        }
        
        return null;
    }
    
    // 实体最小年龄配置
    public int getItemMinAge() {
        try {
            int value = config.getInt("entity_age.itemMinAge", 100);
            return Math.max(value, 0); // 确保最小值为0
        } catch (Exception e) {
            plugin.getLogger().warning("获取物品最小年龄配置时发生错误，使用默认值: " + e.getMessage());
            return 100;
        }
    }
    
    public void setItemMinAge(int age) {
        try {
            config.set("entity_age.itemMinAge", age);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存物品最小年龄配置时发生错误: " + e.getMessage());
        }
    }
    
    public int getExperienceOrbMinAge() {
        try {
            int value = config.getInt("entity_age.experienceOrbMinAge", 100);
            return Math.max(value, 0); // 确保最小值为0
        } catch (Exception e) {
            plugin.getLogger().warning("获取经验球最小年龄配置时发生错误，使用默认值: " + e.getMessage());
            return 100;
        }
    }
    
    public void setExperienceOrbMinAge(int age) {
        try {
            config.set("entity_age.experienceOrbMinAge", age);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存经验球最小年龄配置时发生错误: " + e.getMessage());
        }
    }
    
    public int getArrowMinAge() {
        try {
            int value = config.getInt("entity_age.arrowMinAge", 100);
            return Math.max(value, 0); // 确保最小值为0
        } catch (Exception e) {
            plugin.getLogger().warning("获取箭矢最小年龄配置时发生错误，使用默认值: " + e.getMessage());
            return 100;
        }
    }
    
    public void setArrowMinAge(int age) {
        try {
            config.set("entity_age.arrowMinAge", age);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存箭矢最小年龄配置时发生错误: " + e.getMessage());
        }
    }
    
    // 新增TPS监控配置项
    public int getTpsCheckInterval() {
        try {
            int value = config.getInt("tps_monitoring.checkInterval", 20);
            return Math.max(value, 1); // 确保最小值为1
        } catch (Exception e) {
            plugin.getLogger().warning("获取TPS检查间隔配置时发生错误，使用默认值: " + e.getMessage());
            return 20;
        }
    }
    
    public int getConsecutiveLowTpsThreshold() {
        int value = getCachedValue("tps_monitoring.consecutiveLowTpsThreshold", 3, Integer.class);
        return Math.max(value, 1); // 确保最小值为1
    }
    
    public double getLowTpsCleanupIntensity() {
        try {
            double value = config.getDouble("tps_monitoring.lowTpsCleanupIntensity", 1.5);
            return Math.max(value, 1.0); // 确保最小值为1.0
        } catch (Exception e) {
            plugin.getLogger().warning("获取低TPS清理强度配置时发生错误，使用默认值: " + e.getMessage());
            return 1.5;
        }
    }
    
    public boolean isPrioritizeClusterCleanup() {
        try {
            return config.getBoolean("tps_monitoring.prioritizeClusterCleanup", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取优先聚集清理配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    // 定时检查配置项
    public boolean isScheduledChecksEnabled() {
        try {
            return config.getBoolean("scheduled_checks.enabled", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取定时检查启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public int getEntityCountCheckInterval() {
        try {
            int value = config.getInt("scheduled_checks.entityCountCheckInterval", 300);
            return Math.max(value, 60); // 确保最小值为60秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取实体数量检查间隔配置时发生错误，使用默认值: " + e.getMessage());
            return 300;
        }
    }
    
    public int getLivestockDensityCheckInterval() {
        try {
            int value = config.getInt("scheduled_checks.livestockDensityCheckInterval", 600);
            return Math.max(value, 60); // 确保最小值为60秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取畜牧密度检查间隔配置时发生错误，使用默认值: " + e.getMessage());
            return 600;
        }
    }
    
    public int getPerformanceCheckInterval() {
        try {
            int value = config.getInt("scheduled_checks.performanceCheckInterval", 120);
            return Math.max(value, 30); // 确保最小值为30秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取性能检查间隔配置时发生错误，使用默认值: " + e.getMessage());
            return 120;
        }
    }
    
    public boolean isLogChecks() {
        try {
            return config.getBoolean("scheduled_checks.logChecks", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取记录检查配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public int getResultRetentionTime() {
        try {
            int value = config.getInt("scheduled_checks.resultRetentionTime", 3600);
            return Math.max(value, 300); // 确保最小值为300秒
        } catch (Exception e) {
            plugin.getLogger().warning("获取结果保留时间配置时发生错误，使用默认值: " + e.getMessage());
            return 3600;
        }
    }
    
    public boolean isEnableEarlyWarning() {
        try {
            return config.getBoolean("scheduled_checks.enableEarlyWarning", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取早期警告启用配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    public double getEarlyWarningThreshold() {
        try {
            double value = config.getDouble("scheduled_checks.earlyWarningThreshold", 0.8);
            return Math.max(0.1, Math.min(value, 1.0)); // 确保值在0.1-1.0之间
        } catch (Exception e) {
            plugin.getLogger().warning("获取早期警告阈值配置时发生错误，使用默认值: " + e.getMessage());
            return 0.8;
        }
    }

    // 配置文件访问方法
    public FileConfiguration getConfig() {
        try {
            return config;
        } catch (Exception e) {
            plugin.getLogger().severe("获取配置文件时发生错误: " + e.getMessage());
            return null;
        }
    }
    
    public void saveConfig() {
        try {
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("保存配置文件时发生错误: " + e.getMessage());
            // 向管理员公屏通知配置保存错误
            notifyConfigError("配置文件保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        reload();
    }
    
    /**
     * 验证配置文件并通知错误
     */
    private void validateConfigurationAndNotify() {
        try {
            StringBuilder errors = new StringBuilder();
            boolean hasErrors = false;
            
            // 验证清理间隔
            int cleanupInterval = config.getInt("general.cleanupInterval", 300);
            if (cleanupInterval <= 0) {
                errors.append("清理间隔必须大于0秒 (当前: ").append(cleanupInterval).append("); ");
                hasErrors = true;
            } else if (cleanupInterval < 10) {
                plugin.getLogger().warning("清理间隔过短 (" + cleanupInterval + "秒)，可能影响服务器性能");
            }
            
            // 验证实体密度阈值
            int densityThreshold = config.getInt("entity_density.threshold", 500);
            if (densityThreshold <= 0) {
                errors.append("实体密度阈值必须大于0 (当前: ").append(densityThreshold).append("); ");
                hasErrors = true;
            }
            
            // 验证畜牧业配置
            int maxAnimals = config.getInt("livestock.maxAnimalsPerChunk", 20);
            if (maxAnimals <= 0) {
                errors.append("每区块最大动物数必须大于0 (当前: ").append(maxAnimals).append("); ");
                hasErrors = true;
            }
            
            // 验证TPS阈值
            double tpsThreshold = config.getDouble("tps_monitor.lowTpsThreshold", 17.0);
            if (tpsThreshold <= 0 || tpsThreshold > 20) {
                errors.append("TPS阈值必须在0-20之间 (当前: ").append(tpsThreshold).append("); ");
                hasErrors = true;
            }
            
            // 验证性能配置
            int batchSize = config.getInt("performance.batchSize", 100);
            if (batchSize <= 0) {
                errors.append("批处理大小必须大于0 (当前: ").append(batchSize).append("); ");
                hasErrors = true;
            }
            
            int batchDelay = config.getInt("performance.batchDelay", 10);
            if (batchDelay < 0) {
                errors.append("批处理延迟不能为负数 (当前: ").append(batchDelay).append("); ");
                hasErrors = true;
            }
            
            // 如果有错误，通知管理员
            if (hasErrors) {
                String errorMessage = "配置文件存在错误: " + errors.toString();
                plugin.getLogger().severe(errorMessage);
                notifyConfigError(errorMessage);
            } else {
                plugin.getLogger().info("配置文件验证通过");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("配置验证过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            notifyConfigError("配置验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 向管理员公屏通知配置错误
     */
    private void notifyConfigError(String errorMessage) {
        try {
            // 导入必要的类
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    String message = org.bukkit.ChatColor.RED + "[配置错误] " + 
                                   org.bukkit.ChatColor.WHITE + errorMessage;
                    
                    // 向所有在线的OP发送消息
                    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (player.isOp()) {
                            player.sendMessage(message);
                        }
                    }
                    
                    // 同时在控制台输出
                    plugin.getLogger().severe("[配置错误通知] " + errorMessage);
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("发送配置错误通知时发生异常: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("安排配置错误通知任务时发生异常: " + e.getMessage());
        }
    }
    
    // 添加缺失的方法
    
    /**
     * 检查是否启用自动清理
     */
    public boolean isAutoCleanupEnabled() {
        try {
            return config.getBoolean("smart_cleanup.enabled", true);
        } catch (Exception e) {
            plugin.getLogger().warning("获取自动清理配置时发生错误，使用默认值: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 获取警告时间（分钟）
     */
    public int getWarningTimeMinutes() {
        try {
            return config.getInt("livestock.warningTime", 5);
        } catch (Exception e) {
            plugin.getLogger().warning("获取警告时间配置时发生错误，使用默认值: " + e.getMessage());
            return 5;
        }
    }
    
    /**
     * 获取插件实例
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    // 畜牧业警告消息模板配置
    

    

    
    /**
     * 获取畜牧业警告消息模板
     */
    public String getLivestockWarningMessage(String messageKey) {
        String path = "messages.livestock." + messageKey;
        switch (messageKey) {
            case "title":
                return getString(path, "&c&l[邦邦卡邦！] &f畜牧业密度警告");
            case "separator":
                return getString(path, "&7===========================================");
            case "location":
                return getString(path, "&e位置: &f{region} &7({world} {x}, {z})");
            case "violation":
                return getString(path, "&c超标情况: &f当前 {current} 只，限制 {limit} 只 &c(超出 {excess} 只)");
            case "violation_header":
                return getString(path, "&6▶ [超标情况]");
            case "details":
                return getString(path, "&6详细信息:");
            case "total_animals":
                return getString(path, "&7- 总生物数量: &f{total} 只");
            case "animal_types":
                return getString(path, "&7- 动物类型: &f{types}");
            case "cleanup_notice":
                return getString(path, "&c&l清理通知:");
            case "cleanup_time":
                return getString(path, "&f系统将在 &c{time} 分钟&f 后自动清理超出的动物");
            case "action_reminder":
                return getString(path, "&e请 {title} 及时处理，避免自动清理造成损失");
            case "cleanup_list":
                return getString(path, "&6将被清理的动物:");
            case "cleanup_item":
                return getString(path, "&7- {type}: &f{count} 只");
            case "countdown":
                return getString(path, "&c&l倒计时: &f{time} 秒后开始清理");
            default:
                plugin.getLogger().warning("未知的畜牧业警告消息键: " + messageKey);
                return "&c[消息模板缺失: " + messageKey + "]"; 
        }
    }
}