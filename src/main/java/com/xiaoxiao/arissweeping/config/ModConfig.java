package com.xiaoxiao.arissweeping.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ModConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    public ModConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadDefaults();
    }
    
    private void loadDefaults() {
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
        
        // 性能设置
        config.addDefault("performance.asyncCleanup", true);
        config.addDefault("performance.maxChunksPerTick", 5);
        
        // 消息设置
        config.addDefault("messages.broadcastCleanup", true);
        config.addDefault("messages.showCleanupStats", true);
        
        // 畜牧业管理设置
        config.addDefault("livestock.enableDensityCheck", true);
        config.addDefault("livestock.maxAnimalsPerChunk", 20);
        config.addDefault("livestock.warningTime", 5);
        config.addDefault("livestock.enableWarning", true);
        
        // 实体密度设置
        config.addDefault("entity_density.threshold", 500);
        
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
        config.addDefault("smart_cleanup.protectNamedEntities", true);
        config.addDefault("smart_cleanup.protectLeashedEntities", true);
        config.addDefault("smart_cleanup.protectRiddenEntities", true);
        config.addDefault("smart_cleanup.spawnReasonFilter", false);
        
        // 全局设置
        config.addDefault("global.enabled", false);
        config.addDefault("global.debug", false);
        
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    // 基础设置
    public int getCleanupInterval() {
        return config.getInt("general.cleanupInterval", 300);
    }
    
    public void setCleanupInterval(int interval) {
        config.set("general.cleanupInterval", interval);
        plugin.saveConfig();
    }
    
    // 实体清理设置
    public boolean isCleanupItems() {
        return config.getBoolean("entity_cleanup.cleanupItems", true);
    }
    
    public boolean isCleanupExperienceOrbs() {
        return config.getBoolean("entity_cleanup.cleanupExperienceOrbs", true);
    }
    
    public boolean isCleanupArrows() {
        return config.getBoolean("entity_cleanup.cleanupArrows", true);
    }
    
    public boolean isCleanupFallingBlocks() {
        return config.getBoolean("entity_cleanup.cleanupFallingBlocks", true);
    }
    
    public boolean isCleanupHostileMobs() {
        return config.getBoolean("entity_cleanup.cleanupHostileMobs", false);
    }
    
    public boolean isCleanupPassiveMobs() {
        return config.getBoolean("entity_cleanup.cleanupPassiveMobs", false);
    }
    
    // 清理阈值设置
    public int getMaxItemsPerChunk() {
        return config.getInt("thresholds.maxItemsPerChunk", 50);
    }
    
    public int getMaxEntitiesPerChunk() {
        return config.getInt("thresholds.maxEntitiesPerChunk", 100);
    }
    
    public int getItemAgeThreshold() {
        return config.getInt("thresholds.itemAgeThreshold", 60);
    }
    
    // 性能设置
    public boolean isAsyncCleanup() {
        return config.getBoolean("performance.asyncCleanup", true);
    }
    
    public int getMaxChunksPerTick() {
        return config.getInt("performance.maxChunksPerTick", 5);
    }
    
    public int getBatchSize() {
        return config.getInt("performance.batchSize", 100);
    }
    
    public int getBatchDelay() {
        return config.getInt("performance.batchDelay", 10);
    }
    
    // 畜牧业管理配置
    public boolean isLivestockDensityCheckEnabled() {
        return config.getBoolean("livestock.enableDensityCheck", true);
    }
    
    public int getMaxAnimalsPerChunk() {
        return config.getInt("livestock.maxAnimalsPerChunk", 20);
    }
    
    public int getWarningTime() {
        return config.getInt("livestock.warningTime", 5);
    }
    
    public boolean isWarningEnabled() {
        return config.getBoolean("livestock.enableWarning", true);
    }
    
    // 全局设置
    public boolean isPluginEnabled() {
        return config.getBoolean("global.enabled", false);
    }
    
    public boolean isDebugMode() {
        return config.getBoolean("global.debug", false);
    }
    
    // 消息设置
    public boolean isBroadcastCleanup() {
        return config.getBoolean("messages.broadcastCleanup", true);
    }
    
    public boolean isShowCleanupStats() {
        return config.getBoolean("messages.showCleanupStats", true);
    }
    
    // TPS监控配置
    public boolean isTpsMonitorEnabled() {
        return config.getBoolean("tps_monitor.enabled", true);
    }
    
    public double getLowTpsThreshold() {
        return config.getDouble("tps_monitor.lowTpsThreshold", 17.0);
    }
    
    public boolean isEmergencyCleanupEnabled() {
        return config.getBoolean("tps_monitor.emergencyCleanup", true);
    }
    
    // 聚集清理配置
    public boolean isClusterCleanupEnabled() {
        return config.getBoolean("cluster_cleanup.enabled", true);
    }
    
    public double getClusterDetectionDistance() {
        return config.getDouble("cluster_cleanup.detectionDistance", 3.0);
    }
    
    public int getMinClusterSize() {
        return config.getInt("cluster_cleanup.minClusterSize", 5);
    }
    
    public double getClusterPreserveRatio() {
        return config.getDouble("cluster_cleanup.preserveRatio", 0.3);
    }
    
    public boolean isOnlyCountSameType() {
        return config.getBoolean("cluster_cleanup.onlyCountSameType", true);
    }
    
    // 智能清理配置
    public boolean isProtectNamedEntities() {
        return config.getBoolean("smart_cleanup.protectNamedEntities", true);
    }
    
    public boolean isProtectLeashedEntities() {
        return config.getBoolean("smart_cleanup.protectLeashedEntities", true);
    }
    
    public boolean isProtectRiddenEntities() {
        return config.getBoolean("smart_cleanup.protectRiddenEntities", true);
    }
    
    public boolean isSpawnReasonFilterEnabled() {
        return config.getBoolean("smart_cleanup.spawnReasonFilter", false);
    }
    
    // 实体密度配置
    public int getEntityDensityThreshold() {
        return config.getInt("entity_density.threshold", 500);
    }
    
    // 配置文件访问方法
    public FileConfiguration getConfig() {
        return config;
    }
    
    public void saveConfig() {
        plugin.saveConfig();
    }
}